package com.bff.ms.api;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/bff/bodegas")
public class BodegasController {
  private final WebClient bodegasClient;
  public BodegasController(@Qualifier("bodegasClient") WebClient bodegasClient) { this.bodegasClient = bodegasClient; }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<String>> listar() {
    return bodegasClient.get()
        .exchangeToMono(resp ->
            resp.bodyToMono(String.class).defaultIfEmpty("[]")
                .map(body -> ResponseEntity.status(resp.rawStatusCode())
                    .contentType(MediaType.APPLICATION_JSON).body(body)));
  }

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<String>> uno(@PathVariable Long id) {
    return bodegasClient.get().uri("/{id}", id)
        .exchangeToMono(resp ->
            resp.bodyToMono(String.class).defaultIfEmpty("")
                .map(body -> ResponseEntity.status(resp.rawStatusCode())
                    .contentType(MediaType.APPLICATION_JSON).body(body)));
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<String>> crear(@RequestBody BodegaDto dto) {
    return bodegasClient.post().contentType(MediaType.APPLICATION_JSON).bodyValue(dto)
        .exchangeToMono(resp ->
            resp.bodyToMono(String.class).defaultIfEmpty("")
                .map(body -> ResponseEntity.status(resp.rawStatusCode())
                    .contentType(MediaType.APPLICATION_JSON).body(body)));
  }

  @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<String>> actualizar(@PathVariable Long id, @RequestBody BodegaDto dto) {
    return bodegasClient.put().uri("/{id}", id).contentType(MediaType.APPLICATION_JSON).bodyValue(dto)
        .exchangeToMono(resp ->
            resp.bodyToMono(String.class).defaultIfEmpty("")
                .map(body -> ResponseEntity.status(resp.rawStatusCode())
                    .contentType(MediaType.APPLICATION_JSON).body(body)));
  }

  @DeleteMapping(value = "/{id}")
  public Mono<ResponseEntity<Void>> eliminar(@PathVariable Long id) {
    return bodegasClient.delete().uri("/{id}", id)
        .exchangeToMono(resp -> Mono.just(ResponseEntity.status(resp.rawStatusCode()).build()));
  }
}