package com.function;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.function.db.Db;
import com.function.model.Bodega;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.io.IOException;
import java.sql.*;
import java.util.*;

public class BodegasFunction {

  private static final ObjectMapper MAPPER = new ObjectMapper()
      .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);


  @FunctionName("bodegas")
  public HttpResponseMessage bodegasRoot(
      @HttpTrigger(
          name = "req",
          methods = {HttpMethod.GET, HttpMethod.POST},
          authLevel = AuthorizationLevel.ANONYMOUS, 
          route = "bodegas"
      ) HttpRequestMessage<Optional<String>> request,
      final ExecutionContext ctx) throws Exception {

    switch (request.getHttpMethod()) {
      case GET:  return listar(request);
      case POST: return crear(request);
      default:   return request.createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED).build();
    }
  }


  @FunctionName("bodegasById")
  public HttpResponseMessage bodegasById(
      @HttpTrigger(
          name = "req",
          methods = {HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE},
          authLevel = AuthorizationLevel.ANONYMOUS, 
          route = "bodegas/{id}"
      ) HttpRequestMessage<Optional<String>> request,
      @BindingName("id") String idStr,
      final ExecutionContext ctx) throws Exception {

    long id;
    try { id = Long.parseLong(idStr); }
    catch (NumberFormatException e) {
      return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
          .body("{\"error\":\"id inválido\"}")
          .header("Content-Type","application/json").build();
    }

    switch (request.getHttpMethod()) {
      case GET:    return obtener(request, id);
      case PUT:    return actualizar(request, id);
      case DELETE: return eliminar(request, id);
      default:     return request.createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED).build();
    }
  }



  private HttpResponseMessage listar(HttpRequestMessage<?> req) throws SQLException, IOException {
    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement(
             "SELECT ID, CODIGO, NOMBRE, DIRECCION FROM BODEGAS ORDER BY ID");
         ResultSet rs = ps.executeQuery()) {
      List<Bodega> out = new ArrayList<>();
      while (rs.next()) out.add(map(rs));
      return json(req, out, HttpStatus.OK);
    }
  }

  private HttpResponseMessage obtener(HttpRequestMessage<?> req, long id) throws SQLException, IOException {
    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement(
             "SELECT ID, CODIGO, NOMBRE, DIRECCION FROM BODEGAS WHERE ID=?")) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return json(req, map(rs), HttpStatus.OK);
        return req.createResponseBuilder(HttpStatus.NOT_FOUND).body("No encontrado").build();
      }
    }
  }

  private HttpResponseMessage crear(HttpRequestMessage<Optional<String>> req) {
    try {
      String body = req.getBody().orElse("");
      if (body.isBlank()) {
        return req.createResponseBuilder(HttpStatus.BAD_REQUEST)
            .header("Content-Type","application/json")
            .body("{\"error\":\"Body vacío\"}")
            .build();
      }

      Bodega in = MAPPER.readValue(body, Bodega.class);
      if (in.getCodigo() == null || in.getNombre() == null || in.getDireccion() == null) {
        return req.createResponseBuilder(HttpStatus.BAD_REQUEST)
            .header("Content-Type","application/json")
            .body("{\"error\":\"codigo, nombre y direccion son obligatorios\"}")
            .build();
      }

      try (Connection con = Db.connect();
           PreparedStatement ps = con.prepareStatement(
               "INSERT INTO BODEGAS (CODIGO, NOMBRE, DIRECCION) VALUES (?,?,?)")) {
        ps.setString(1, in.getCodigo());
        ps.setString(2, in.getNombre());
        ps.setString(3, in.getDireccion());
        int rows = ps.executeUpdate();
        if (rows > 0) {
          return req.createResponseBuilder(HttpStatus.CREATED)
              .header("Content-Type","application/json")
              .body("{\"status\":\"created\"}")
              .build();
        }
        return req.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
            .header("Content-Type","application/json")
            .body("{\"error\":\"Insert no afectó filas\"}")
            .build();
      }
    } catch (com.fasterxml.jackson.databind.JsonMappingException jm) {
      return req.createResponseBuilder(HttpStatus.BAD_REQUEST)
          .header("Content-Type","application/json")
          .body("{\"error\":\"JSON inválido\",\"detalle\":\"" +
                jm.getOriginalMessage().replace("\"","'") + "\"}")
          .build();
    } catch (SQLException ex) {
      return req.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
          .header("Content-Type","application/json")
          .body("{\"error\":\"DB\",\"sqlstate\":\"" + ex.getSQLState() +
                "\",\"code\":" + ex.getErrorCode() +
                ",\"message\":\"" + ex.getMessage().replace("\"","'") + "\"}")
          .build();
    } catch (Exception e) {
      return req.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
          .header("Content-Type","application/json")
          .body("{\"error\":\"server\",\"message\":\"" +
                e.getMessage().replace("\"","'") + "\"}")
          .build();
    }
  }

  private HttpResponseMessage actualizar(HttpRequestMessage<Optional<String>> req, long id) throws Exception {
    Bodega in = MAPPER.readValue(req.getBody().orElse("{}"), Bodega.class);
    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement(
             "UPDATE BODEGAS SET CODIGO=?, NOMBRE=?, DIRECCION=? WHERE ID=?")) {
      ps.setString(1, in.getCodigo());
      ps.setString(2, in.getNombre());
      ps.setString(3, in.getDireccion());
      ps.setLong(4, id);
      int rows = ps.executeUpdate();
      if (rows == 0) return req.createResponseBuilder(HttpStatus.NOT_FOUND).build();
      return obtener(req, id);
    }
  }

  private HttpResponseMessage eliminar(HttpRequestMessage<?> req, long id) throws SQLException {
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
