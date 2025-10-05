package com.function;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.function.db.Db;
import com.function.model.Producto;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import com.function.events.EventBusEG;

public class ProductosFunction {

  private static final ObjectMapper MAPPER = new ObjectMapper()
      .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);

  @FunctionName("productos")
  public HttpResponseMessage productosRoot(
      @HttpTrigger(
          name = "req",
          methods = {HttpMethod.GET, HttpMethod.POST},
          authLevel = AuthorizationLevel.ANONYMOUS,
          route = "productos"
      ) HttpRequestMessage<Optional<String>> request,
      final ExecutionContext ctx) throws Exception {

    switch (request.getHttpMethod()) {
      case GET:  return listar(request);
      case POST: return crear(request);
      default:   return request.createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED).build();
    }
  }

  @FunctionName("productosById")
  public HttpResponseMessage productosById(
      @HttpTrigger(
          name = "req",
          methods = {HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE},
          authLevel = AuthorizationLevel.ANONYMOUS,
          route = "productos/{id}"
      ) HttpRequestMessage<Optional<String>> request,
      @BindingName("id") String idStr,
      final ExecutionContext ctx) throws Exception {

    long id;
    try { id = Long.parseLong(idStr); }
    catch (NumberFormatException e) {
      return badRequest(request, "{\"error\":\"id inválido\"}");
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
             "SELECT ID, SKU, NOMBRE, STOCK, PRECIO, BODEGA_ID FROM PRODUCTOS ORDER BY ID");
         ResultSet rs = ps.executeQuery()) {
      List<Producto> out = new ArrayList<>();
      while (rs.next()) out.add(map(rs));
      return json(req, out, HttpStatus.OK);
    }
  }

  private HttpResponseMessage obtener(HttpRequestMessage<?> req, long id) throws SQLException, IOException {
    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement(
             "SELECT ID, SKU, NOMBRE, STOCK, PRECIO, BODEGA_ID FROM PRODUCTOS WHERE ID=?")) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return json(req, map(rs), HttpStatus.OK);
        return req.createResponseBuilder(HttpStatus.NOT_FOUND).body("No encontrado").build();
      }
    }
  }

  private HttpResponseMessage crear(HttpRequestMessage<Optional<String>> req) {
    try {
      final String body = req.getBody().orElse("");
      if (body.isBlank()) return badRequest(req, "{\"error\":\"Body vacío\"}");

      Producto in = MAPPER.readValue(body, Producto.class);

      // Validación mínima
      if (isBlank(in.getSku()) || isBlank(in.getNombre())) {
        return badRequest(req, "{\"error\":\"sku y nombre son obligatorios\"}");
      }
      if (in.getStock() == null) in.setStock(0);
      if (in.getPrecio() == null) in.setPrecio(BigDecimal.ZERO);

      try (Connection con = Db.connect();
           PreparedStatement ps = con.prepareStatement(
               "INSERT INTO PRODUCTOS (SKU, NOMBRE, STOCK, PRECIO, BODEGA_ID) VALUES (?,?,?,?,?)"
           )) {
        ps.setString(1, in.getSku());
        ps.setString(2, in.getNombre());
        ps.setInt(3, in.getStock());
        ps.setBigDecimal(4, in.getPrecio());
        if (in.getBodegaId() == null) ps.setNull(5, Types.NUMERIC); else ps.setLong(5, in.getBodegaId());

        int rows = ps.executeUpdate();
        if (rows > 0) {
          Long newId = fetchIdProductoBySku(con, in.getSku());
          Map<String,Object> data = new HashMap<>();
          if (newId != null) data.put("id", newId);
          data.put("sku", in.getSku());
          data.put("nombre", in.getNombre());
          data.put("stock", in.getStock());
          data.put("precio", in.getPrecio());
          if (in.getBodegaId() != null) data.put("bodegaId", in.getBodegaId());

          EventBusEG.publish("Inventario.Producto.Creado",
              newId != null ? "/productos/"+newId : "/productos", data);

          int umbral = Integer.parseInt(System.getenv().getOrDefault("UMBRAL_STOCK","10"));
          if (in.getStock() < umbral) {
            EventBusEG.publish("Inventario.Producto.StockBajo",
                newId != null ? "/productos/"+newId : "/productos",
                Map.of("stock", in.getStock(), "umbral", umbral, "sku", in.getSku()));
          }

          return req.createResponseBuilder(HttpStatus.CREATED)
              .header("Content-Type","application/json")
              .body("{\"status\":\"created\"}")
              .build();
        }
        return serverError(req, "{\"error\":\"Insert no afectó filas\"}");
      }
    } catch (com.fasterxml.jackson.databind.JsonMappingException jm) {
      return badRequest(req, "{\"error\":\"JSON inválido\",\"detalle\":\"" +
          jm.getOriginalMessage().replace("\"","'") + "\"}");
    } catch (SQLException ex) {
      return dbError(req, ex);
    } catch (Exception e) {
      return serverError(req, "{\"error\":\"server\",\"message\":\"" +
          e.getMessage().replace("\"","'") + "\"}");
    }
  }

  private HttpResponseMessage actualizar(HttpRequestMessage<Optional<String>> req, long id) {
    try {
      Producto in = MAPPER.readValue(req.getBody().orElse("{}"), Producto.class);
      if (in.getStock() == null) in.setStock(0);
      if (in.getPrecio() == null) in.setPrecio(BigDecimal.ZERO);

      try (Connection con = Db.connect();
           PreparedStatement ps = con.prepareStatement(
               "UPDATE PRODUCTOS SET SKU=?, NOMBRE=?, STOCK=?, PRECIO=?, BODEGA_ID=? WHERE ID=?"
           )) {
        ps.setString(1, in.getSku());
        ps.setString(2, in.getNombre());
        ps.setInt(3, in.getStock());
        ps.setBigDecimal(4, in.getPrecio());
        if (in.getBodegaId() == null) ps.setNull(5, Types.NUMERIC); else ps.setLong(5, in.getBodegaId());
        ps.setLong(6, id);

        int rows = ps.executeUpdate();
        if (rows == 0) return req.createResponseBuilder(HttpStatus.NOT_FOUND).build();
        Map<String,Object> data = new HashMap<>();
        data.put("id", id);
        data.put("sku", in.getSku());
        data.put("nombre", in.getNombre());
        data.put("stock", in.getStock());
        data.put("precio", in.getPrecio());
        if (in.getBodegaId() != null) data.put("bodegaId", in.getBodegaId());
        EventBusEG.publish("Inventario.Producto.Actualizado", "/productos/"+id, data);

        int umbral = Integer.parseInt(System.getenv().getOrDefault("UMBRAL_STOCK","10"));
        if (in.getStock() < umbral) {
          EventBusEG.publish("Inventario.Producto.StockBajo",
              "/productos/"+id, Map.of("id", id, "stock", in.getStock(), "umbral", umbral, "sku", in.getSku()));
        }

        return obtener(req, id);
      }
    } catch (com.fasterxml.jackson.databind.JsonMappingException jm) {
      return badRequest(req, "{\"error\":\"JSON inválido\",\"detalle\":\"" +
          jm.getOriginalMessage().replace("\"","'") + "\"}");
    } catch (SQLException ex) {
      return dbError(req, ex);
    } catch (Exception e) {
      return serverError(req, "{\"error\":\"server\",\"message\":\"" +
          e.getMessage().replace("\"","'") + "\"}");
    }
  }

  private HttpResponseMessage eliminar(HttpRequestMessage<?> req, long id) {
    try (Connection con = Db.connect();
         PreparedStatement ps = con.prepareStatement("DELETE FROM PRODUCTOS WHERE ID=?")) {
      ps.setLong(1, id);
      int rows = ps.executeUpdate();

      if (rows > 0) {
        EventBusEG.publish("Inventario.Producto.Eliminado", "/productos/"+id, Map.of("id", id));
      }

      return req.createResponseBuilder(rows > 0 ? HttpStatus.NO_CONTENT : HttpStatus.NOT_FOUND).build();
    } catch (SQLException ex) {
      return dbError(req, ex);
    }
  }

  /* ================== Helpers ================== */

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

  private static Long fetchIdProductoBySku(Connection con, String sku) {
    try (PreparedStatement q = con.prepareStatement("SELECT ID FROM PRODUCTOS WHERE SKU=?")) {
      q.setString(1, sku);
      try (ResultSet rs = q.executeQuery()) { return rs.next() ? rs.getLong(1) : null; }
    } catch (SQLException e) { return null; }
  }

  private static HttpResponseMessage json(HttpRequestMessage<?> req, Object body, HttpStatus status) throws IOException {
    return req.createResponseBuilder(status)
        .header("Content-Type", "application/json")
        .body(MAPPER.writeValueAsString(body))
        .build();
  }

  private static HttpResponseMessage badRequest(HttpRequestMessage<?> req, String body) {
    return req.createResponseBuilder(HttpStatus.BAD_REQUEST)
        .header("Content-Type","application/json").body(body).build();
  }

  private static HttpResponseMessage serverError(HttpRequestMessage<?> req, String body) {
    return req.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
        .header("Content-Type","application/json").body(body).build();
  }

  private static HttpResponseMessage dbError(HttpRequestMessage<?> req, SQLException ex) {
    String body = "{\"error\":\"DB\",\"sqlstate\":\"" + ex.getSQLState() +
        "\",\"code\":" + ex.getErrorCode() +
        ",\"message\":\"" + ex.getMessage().replace("\"","'") + "\"}";
    return req.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
        .header("Content-Type","application/json").body(body).build();
  }

  private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
