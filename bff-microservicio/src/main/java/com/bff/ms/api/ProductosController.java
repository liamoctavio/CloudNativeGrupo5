package com.bff.ms.api;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/bff/productos")
public class ProductosController {
  private final WebClient productosClient;
  public ProductosController(@Qualifier("productosClient") WebClient productosClient) {
    this.productosClient = productosClient;
  }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<String>> listar() {
    return productosClient.get()
        .exchangeToMono(resp ->
            resp.bodyToMono(String.class).defaultIfEmpty("[]")
                .map(body -> ResponseEntity.status(resp.rawStatusCode())
                    .contentType(MediaType.APPLICATION_JSON).body(body)));
  }

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<String>> uno(@PathVariable Long id) {
    return productosClient.get().uri("/{id}", id)
        .exchangeToMono(resp ->
            resp.bodyToMono(String.class).defaultIfEmpty("")
                .map(body -> ResponseEntity.status(resp.rawStatusCode())
                    .contentType(MediaType.APPLICATION_JSON).body(body)));
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<String>> crear(@RequestBody ProductoDto dto) {
    return productosClient.post().contentType(MediaType.APPLICATION_JSON).bodyValue(dto)
        .exchangeToMono(resp ->
            resp.bodyToMono(String.class).defaultIfEmpty("")
                .map(body -> ResponseEntity.status(resp.rawStatusCode())
                    .contentType(MediaType.APPLICATION_JSON).body(body)));
  }

  @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<String>> actualizar(@PathVariable Long id, @RequestBody ProductoDto dto) {
    return productosClient.put().uri("/{id}", id).contentType(MediaType.APPLICATION_JSON).bodyValue(dto)
        .exchangeToMono(resp ->
            resp.bodyToMono(String.class).defaultIfEmpty("")
                .map(body -> ResponseEntity.status(resp.rawStatusCode())
                    .contentType(MediaType.APPLICATION_JSON).body(body)));
  }

  @DeleteMapping("/{id}")
  public Mono<ResponseEntity<Void>> eliminar(@PathVariable Long id) {
    return productosClient.delete().uri("/{id}", id)
        .exchangeToMono(resp -> Mono.just(ResponseEntity.status(resp.rawStatusCode()).build()));
  }
}