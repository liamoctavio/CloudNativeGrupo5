package com.function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.function.db.Db;
import com.function.model.Bodega;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.io.IOException;
import java.sql.*;
import java.util.*;


public class BodegasFunction {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @FunctionName("bodegas")
  public HttpResponseMessage handle(
      @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE},
                   authLevel = AuthorizationLevel.FUNCTION, route = "bodegas/{id?}")
      HttpRequestMessage<Optional<String>> request,
      @BindingName("id") String id,
      final ExecutionContext ctx) throws Exception {

    switch (request.getHttpMethod().name()) {
      case "GET": return (id == null) ? listar(request) : obtener(request, Long.parseLong(id));
      case "POST": return crear(request);
      case "PUT": return actualizar(request, Long.parseLong(id));
      case "DELETE": return eliminar(request, Long.parseLong(id));
      default: return request.createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED).build();
    }
  }

  private HttpResponseMessage listar(HttpRequestMessage<?> req) throws SQLException, IOException {
    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement("SELECT ID, CODIGO, NOMBRE, DIRECCION FROM BODEGAS ORDER BY ID");
         ResultSet rs = ps.executeQuery()) {
      List<Bodega> out = new ArrayList<>();
      while (rs.next()) out.add(map(rs));
      return json(req, out, HttpStatus.OK);
    }
  }

  private HttpResponseMessage obtener(HttpRequestMessage<?> req, Long id) throws SQLException, IOException {
    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement("SELECT ID, CODIGO, NOMBRE, DIRECCION FROM BODEGAS WHERE ID=?")) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return json(req, map(rs), HttpStatus.OK);
        return req.createResponseBuilder(HttpStatus.NOT_FOUND).body("No encontrado").build();
      }
    }
  }

  private HttpResponseMessage crear(HttpRequestMessage<Optional<String>> req) throws Exception {
    Bodega in = MAPPER.readValue(req.getBody().orElse("{}"), Bodega.class);
    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement(
             "INSERT INTO BODEGAS (CODIGO, NOMBRE, DIRECCION) VALUES (?,?,?)", new String[]{"ID"})) {
      ps.setString(1, in.getCodigo());
      ps.setString(2, in.getNombre());
      ps.setString(3, in.getDireccion());
      ps.executeUpdate();
      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (keys.next()) return obtener(req, keys.getLong(1));
      }
      return req.createResponseBuilder(HttpStatus.CREATED).build();
    }
  }

  private HttpResponseMessage actualizar(HttpRequestMessage<Optional<String>> req, Long id) throws Exception {
    Bodega in = MAPPER.readValue(req.getBody().orElse("{}"), Bodega.class);
    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement("UPDATE BODEGAS SET CODIGO=?, NOMBRE=?, DIRECCION=? WHERE ID=?")) {
      ps.setString(1, in.getCodigo());
      ps.setString(2, in.getNombre());
      ps.setString(3, in.getDireccion());
      ps.setLong(4, id);
      int rows = ps.executeUpdate();
      if (rows == 0) return req.createResponseBuilder(HttpStatus.NOT_FOUND).build();
      return obtener(req, id);
    }
  }

  private HttpResponseMessage eliminar(HttpRequestMessage<?> req, Long id) throws SQLException {
    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement("DELETE FROM BODEGAS WHERE ID=?")) {
      ps.setLong(1, id);
      int rows = ps.executeUpdate();
      return req.createResponseBuilder(rows > 0 ? HttpStatus.NO_CONTENT : HttpStatus.NOT_FOUND).build();
    }
  }

  private static Bodega map(ResultSet rs) throws SQLException {
    return new Bodega(
        rs.getLong("ID"),
        rs.getString("CODIGO"),
        rs.getString("NOMBRE"),
        rs.getString("DIRECCION")
    );
  }

  private static HttpResponseMessage json(HttpRequestMessage<?> req, Object body, HttpStatus status) throws IOException {
    return req.createResponseBuilder(status)
        .header("Content-Type", "application/json")
        .body(MAPPER.writeValueAsString(body))
        .build();
  }
  
}

