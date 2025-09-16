package com.function;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.Scalars;
import graphql.schema.DataFetcher;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;


public class FunctionGraphQL {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final HttpClient HTTP = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10))
      .build();

  private static String joinUrl(String base, String path) {
    String b = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    String p = path.startsWith("/") ? path : ("/" + path);
    return b + p;
  }

  private static HttpRequest.Builder get(String url, String key) {
    HttpRequest.Builder b = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(Duration.ofSeconds(20))
        .header("Accept", "application/json")
        .GET();
    if (key != null && !key.isBlank()) b.header("x-functions-key", key);
    return b;
  }

  private static HttpRequest.Builder postJson(String url, String key, String json) {
    HttpRequest.Builder b = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(Duration.ofSeconds(20))
        .header("Accept", "application/json")
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json));
    if (key != null && !key.isBlank()) b.header("x-functions-key", key);
    return b;
  }

  private static <T> T getJson(String url, String key, TypeReference<T> type) throws Exception {
    HttpResponse<String> resp = HTTP.send(get(url, key).build(), HttpResponse.BodyHandlers.ofString());
    if (resp.statusCode() / 100 == 2) return MAPPER.readValue(resp.body(), type);
    throw new RuntimeException("GET " + url + " -> " + resp.statusCode() + " " + resp.body());
  }

  private static <T> T postJson(String url, String key, Object body, TypeReference<T> type) throws Exception {
    String json = MAPPER.writeValueAsString(body);
    HttpResponse<String> resp = HTTP.send(postJson(url, key, json).build(), HttpResponse.BodyHandlers.ofString());
    if (resp.statusCode() / 100 == 2 || resp.statusCode() == 201) {
      if (resp.body() == null || resp.body().isBlank()) return null;
      return MAPPER.readValue(resp.body(), type);
    }
    throw new RuntimeException("POST " + url + " -> " + resp.statusCode() + " " + resp.body());
  }

  private static final GraphQL graphQL;

  static {
    // ==== Tipos ====
    GraphQLObjectType productoType = GraphQLObjectType.newObject()
        .name("Producto")
        .field(f -> f.name("id").type(Scalars.GraphQLID))
        .field(f -> f.name("sku").type(Scalars.GraphQLString))
        .field(f -> f.name("nombre").type(Scalars.GraphQLString))
        .field(f -> f.name("stock").type(Scalars.GraphQLInt))
        .field(f -> f.name("precio").type(Scalars.GraphQLFloat)) // BigDecimal -> Float en GraphQL
        .field(f -> f.name("bodegaId").type(Scalars.GraphQLID))
        .build();

    GraphQLObjectType bodegaType = GraphQLObjectType.newObject()
        .name("Bodega")
        .field(f -> f.name("id").type(Scalars.GraphQLID))
        .field(f -> f.name("codigo").type(Scalars.GraphQLString))
        .field(f -> f.name("nombre").type(Scalars.GraphQLString))
        .field(f -> f.name("direccion").type(Scalars.GraphQLString))
        .field(f -> f.name("productos").type(new GraphQLList(productoType))) // resolver anidado
        .build();

    // ==== Env vars ====
    final String PROD_BASE = Objects.requireNonNull(System.getenv("API_PRODUCTOS_BASE"),
        "Falta env API_PRODUCTOS_BASE");
    final String BOD_BASE  = Objects.requireNonNull(System.getenv("API_BODEGAS_BASE"),
        "Falta env API_BODEGAS_BASE");
    final String PROD_KEY  = Optional.ofNullable(System.getenv("API_PRODUCTOS_KEY")).orElse("");
    final String BOD_KEY   = Optional.ofNullable(System.getenv("API_BODEGAS_KEY")).orElse("");

    final String URL_PROD_LIST = joinUrl(PROD_BASE, "/api/productos");
    final String URL_BOD_LIST  = joinUrl(BOD_BASE,  "/api/bodegas");


    DataFetcher<List<Map<String,Object>>> productosDF = env -> {
      List<Map<String,Object>> list = getJson(URL_PROD_LIST, PROD_KEY,
          new TypeReference<List<Map<String,Object>>>() {});
      String bodegaId = env.getArgument("bodegaId");
      if (bodegaId == null || bodegaId.isBlank()) return list;

      List<Map<String,Object>> out = new ArrayList<>();
      for (Map<String,Object> p : list) {
        Object bid = p.get("bodegaId");
        if (bid != null && bodegaId.equals(String.valueOf(bid))) out.add(p);
      }
      return out;
    };

    DataFetcher<Map<String,Object>> productoDF = env -> {
      String id = String.valueOf(env.getArgument("id"));
      String url = joinUrl(PROD_BASE, "/api/productos/" + URLEncoder.encode(id, StandardCharsets.UTF_8));
      return getJson(url, PROD_KEY, new TypeReference<Map<String,Object>>() {});
    };

    DataFetcher<List<Map<String,Object>>> bodegasDF = env -> {
      return getJson(URL_BOD_LIST, BOD_KEY, new TypeReference<List<Map<String,Object>>>() {});
    };

    DataFetcher<Map<String,Object>> bodegaDF = env -> {
      String id = String.valueOf(env.getArgument("id"));
      String url = joinUrl(BOD_BASE, "/api/bodegas/" + URLEncoder.encode(id, StandardCharsets.UTF_8));
      return getJson(url, BOD_KEY, new TypeReference<Map<String,Object>>() {});
    };

    DataFetcher<List<Map<String,Object>>> bodegaProductosDF = env -> {
      @SuppressWarnings("unchecked")
      Map<String,Object> bodega = env.getSource();
      Object idObj = bodega.get("id");
      if (idObj == null) return List.of();
      String bodegaId = String.valueOf(idObj);

      List<Map<String,Object>> list = getJson(URL_PROD_LIST, PROD_KEY,
          new TypeReference<List<Map<String,Object>>>() {});
      List<Map<String,Object>> out = new ArrayList<>();
      for (Map<String,Object> p : list) {
        Object bid = p.get("bodegaId");
        if (bid != null && bodegaId.equals(String.valueOf(bid))) out.add(p);
      }
      return out;
    };

    DataFetcher<Map<String,Object>> crearProductoDF = env -> {
      String sku = env.getArgument("sku");
      String nombre = env.getArgument("nombre");
      Integer stock = env.getArgument("stock");
      Double precio = env.getArgument("precio");
      String bodegaId = env.getArgument("bodegaId"); // opcional

      Map<String,Object> payload = new LinkedHashMap<>();
      payload.put("sku", sku);
      payload.put("nombre", nombre);
      payload.put("stock", stock != null ? stock : 0);
      payload.put("precio", precio != null ? new BigDecimal(precio) : BigDecimal.ZERO);
      if (bodegaId != null && !bodegaId.isBlank()) {
        try { payload.put("bodegaId", Long.parseLong(bodegaId)); }
        catch (NumberFormatException ignore) { payload.put("bodegaId", null); }
      } else {
        payload.put("bodegaId", null);
      }

      Map<String,Object> created = postJson(URL_PROD_LIST, PROD_KEY, payload,
          new TypeReference<Map<String,Object>>() {});
      return created != null ? created : Map.of("status","created");
    };

    GraphQLObjectType queryType = GraphQLObjectType.newObject()
        .name("Query")
        .field(f -> f.name("productos")
            .type(new GraphQLList(productoType))
            .argument(a -> a.name("bodegaId").type(Scalars.GraphQLID))
            .dataFetcher(productosDF))
        .field(f -> f.name("producto")
            .type(productoType)
            .argument(a -> a.name("id").type(Scalars.GraphQLID))
            .dataFetcher(productoDF))
        .field(f -> f.name("bodegas")
            .type(new GraphQLList(bodegaType))
            .dataFetcher(bodegasDF))
        .field(f -> f.name("bodega")
            .type(bodegaType)
            .argument(a -> a.name("id").type(Scalars.GraphQLID))
            .dataFetcher(bodegaDF))
        .build();

    GraphQLObjectType mutationType = GraphQLObjectType.newObject()
        .name("Mutation")
        .field(f -> f.name("crearProducto")
            .type(productoType) 
            .argument(a -> a.name("sku").type(Scalars.GraphQLString))
            .argument(a -> a.name("nombre").type(Scalars.GraphQLString))
            .argument(a -> a.name("stock").type(Scalars.GraphQLInt))
            .argument(a -> a.name("precio").type(Scalars.GraphQLFloat))
            .argument(a -> a.name("bodegaId").type(Scalars.GraphQLID))
            .dataFetcher(crearProductoDF))
        .build();

    GraphQLCodeRegistry codeRegistry = GraphQLCodeRegistry.newCodeRegistry()
        .dataFetcher(FieldCoordinates.coordinates("Bodega", "productos"), bodegaProductosDF)
        .build();


    GraphQLSchema schema = GraphQLSchema.newSchema()
        .query(queryType)
        .mutation(mutationType)         
        .additionalType(bodegaType)
        .additionalType(productoType)
        .codeRegistry(codeRegistry)     
        .build();

    graphQL = GraphQL.newGraphQL(schema).build();
  }

  @FunctionName("graphql")
  public HttpResponseMessage run(
      @HttpTrigger(
          name = "req",
          methods = { HttpMethod.POST }, 
          authLevel = AuthorizationLevel.ANONYMOUS
      ) HttpRequestMessage<Map<String, Object>> request,
      final ExecutionContext context
  ) {
    Map<String, Object> body = request.getBody();
    if (body == null || !body.containsKey("query")) {
      return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
          .header("Content-Type","application/json")
          .body(Map.of("error","Body JSON inválido. Esperado: { \"query\": \"...\" }"))
          .build();
    }

    String query = String.valueOf(body.get("query"));
    Map<String,Object> variables = Map.of();
    Object vars = body.get("variables");
    if (vars instanceof Map) variables = (Map<String,Object>) vars;

    ExecutionInput input = ExecutionInput.newExecutionInput()
        .query(query)
        .variables(variables)
        .build();

    Map<String,Object> result = graphQL.execute(input).toSpecification();
    return request.createResponseBuilder(HttpStatus.OK)
        .header("Content-Type","application/json")
        .body(result)
        .build();
  }
}
