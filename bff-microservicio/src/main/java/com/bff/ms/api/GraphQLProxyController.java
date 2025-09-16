package com.bff.ms.api;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/bff")
public class GraphQLProxyController {

  private final WebClient graphqlClient;

  public GraphQLProxyController(@Qualifier("graphqlClient") WebClient graphqlClient) {
    this.graphqlClient = graphqlClient;
  }

  @PostMapping(value = "/graphql", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<String>> post(@RequestBody Map<String,Object> body) {
    Object q = body.get("query");
    if (q == null || !StringUtils.hasText(q.toString())) {
      return Mono.just(ResponseEntity.badRequest()
          .contentType(MediaType.APPLICATION_JSON)
          .body("{\"error\":\"Body JSON inválido. Esperado: { \\\"query\\\": \\\"...\\\" }\"}"));
    }

    return graphqlClient.post()
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body) 
        .exchangeToMono(resp ->
            resp.bodyToMono(String.class).defaultIfEmpty("{}")
                .map(b -> ResponseEntity.status(resp.rawStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(b)));
  }


  @GetMapping(value = "/graphql", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<String>> get(@RequestParam(name = "query", required = false) String query,
                                          @RequestParam(name = "variables", required = false) String variablesJson) {
    if (!StringUtils.hasText(query)) {
      return Mono.just(ResponseEntity.badRequest()
          .contentType(MediaType.APPLICATION_JSON)
          .body("{\"error\":\"Falta query en querystring\"}"));
    }
    Map<String,Object> payload = new HashMap<>();
    payload.put("query", query);
    if (StringUtils.hasText(variablesJson)) {

      payload.put("variables", variablesJson);
    }

    return graphqlClient.post()
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(payload)
        .exchangeToMono(resp ->
            resp.bodyToMono(String.class).defaultIfEmpty("{}")
                .map(b -> ResponseEntity.status(resp.rawStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(b)));
  }
}
