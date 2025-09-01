package com.function;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.function.db.Db;
import com.function.model.Producto;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class ProductosFunction {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @FunctionName("productos")
  public HttpResponseMessage handle(
      @HttpTrigger(
          name = "req",
          methods = {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE},
          authLevel = AuthorizationLevel.FUNCTION,
          route = "productos/{id?}")
      HttpRequestMessage<Optional<String>> request,
      @BindingName("id") String id,
      final ExecutionContext context) throws Exception {

    switch (request.getHttpMethod().name()) {
      case "GET":
        return (id == null) ? listar(request) : obtener(request, Long.parseLong(id));
      case "POST":
        return crear(request);
      case "PUT":
        return actualizar(request, Long.parseLong(id));
      case "DELETE":
        return eliminar(request, Long.parseLong(id));
      default:
        return request.createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED).build();
    }
  }

  private HttpResponseMessage listar(HttpRequestMessage<?> req) throws SQLException, IOException {
    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement("SELECT ID, SKU, NOMBRE, STOCK, PRECIO, BODEGA_ID FROM PRODUCTOS ORDER BY ID");
         ResultSet rs = ps.executeQuery()) {
      List<Producto> out = new ArrayList<>();
      while (rs.next()) out.add(map(rs));
      return json(req, out, HttpStatus.OK);
    }
  }

  private HttpResponseMessage obtener(HttpRequestMessage<?> req, Long id) throws SQLException, IOException {
    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement("SELECT ID, SKU, NOMBRE, STOCK, PRECIO, BODEGA_ID FROM PRODUCTOS WHERE ID = ?")) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return json(req, map(rs), HttpStatus.OK);
        return req.createResponseBuilder(HttpStatus.NOT_FOUND).body("No encontrado").build();
      }
    }
  }

  private HttpResponseMessage crear(HttpRequestMessage<Optional<String>> req) throws Exception {
    Producto in = MAPPER.readValue(req.getBody().orElse("{}"), Producto.class);
    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement(
           "INSERT INTO PRODUCTOS (SKU, NOMBRE, STOCK, PRECIO, BODEGA_ID) VALUES (?,?,?,?,?)", new String[]{"ID"})) {
      ps.setString(1, in.getSku());
      ps.setString(2, in.getNombre());
      ps.setInt(3, in.getStock() == null ? 0 : in.getStock());
      ps.setBigDecimal(4, in.getPrecio() == null ? BigDecimal.ZERO : in.getPrecio());
      if (in.getBodegaId() == null) ps.setNull(5, Types.NUMERIC); else ps.setLong(5, in.getBodegaId());
      ps.executeUpdate();
      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (keys.next()) return obtener(req, keys.getLong(1));
      }
      return req.createResponseBuilder(HttpStatus.CREATED).build();
    }
  }

  private HttpResponseMessage actualizar(HttpRequestMessage<Optional<String>> req, Long id) throws Exception {
    Producto in = MAPPER.readValue(req.getBody().orElse("{}"), Producto.class);
    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement(
           "UPDATE PRODUCTOS SET SKU=?, NOMBRE=?, STOCK=?, PRECIO=?, BODEGA_ID=? WHERE ID=?")) {
      ps.setString(1, in.getSku());
      ps.setString(2, in.getNombre());
      ps.setInt(3, in.getStock() == null ? 0 : in.getStock());
      ps.setBigDecimal(4, in.getPrecio() == null ? BigDecimal.ZERO : in.getPrecio());
      if (in.getBodegaId() == null) ps.setNull(5, Types.NUMERIC); else ps.setLong(5, in.getBodegaId());
      ps.setLong(6, id);
      int rows = ps.executeUpdate();
      if (rows == 0) return req.createResponseBuilder(HttpStatus.NOT_FOUND).build();
      return obtener(req, id);
    }
  }

  private HttpResponseMessage eliminar(HttpRequestMessage<?> req, Long id) throws SQLException {
    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement("DELETE FROM PRODUCTOS WHERE ID=?")) {
      ps.setLong(1, id);
      int rows = ps.executeUpdate();
      return req.createResponseBuilder(rows > 0 ? HttpStatus.NO_CONTENT : HttpStatus.NOT_FOUND).build();
    }
  }

  private static Producto map(ResultSet rs) throws SQLException {
    Long bodegaId = (rs.getObject("BODEGA_ID") == null ? null : rs.getLong("BODEGA_ID"));
    return new Producto(
        rs.getLong("ID"),
        rs.getString("SKU"),
        rs.getString("NOMBRE"),
        rs.getInt("STOCK"),
        rs.getBigDecimal("PRECIO"),
        bodegaId
    );
  }

  private static HttpResponseMessage json(HttpRequestMessage<?> req, Object body, HttpStatus status) throws IOException {
    return req.createResponseBuilder(status)
        .header("Content-Type", "application/json")
        .body(MAPPER.writeValueAsString(body))
        .build();
  }

  public HttpResponseMessage run(HttpRequestMessage<Optional<String>> req, ExecutionContext context) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'run'");
  }
}
