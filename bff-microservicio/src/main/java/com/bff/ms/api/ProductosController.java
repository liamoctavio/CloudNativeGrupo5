package com.bff.ms.api;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/bff/productos")
public class ProductosController {
  private final WebClient productosClient;
  public ProductosController(@Qualifier("productosClient") WebClient productosClient) { this.productosClient = productosClient; }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<String> listar() { return productosClient.get().retrieve().bodyToMono(String.class); }

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<String> uno(@PathVariable Long id) { return productosClient.get().uri("/{id}", id).retrieve().bodyToMono(String.class); }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<String> crear(@RequestBody ProductoDto dto) {
    return productosClient.post().contentType(MediaType.APPLICATION_JSON).bodyValue(dto).retrieve().bodyToMono(String.class);
  }

  @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<String> actualizar(@PathVariable Long id, @RequestBody ProductoDto dto) {
    return productosClient.put().uri("/{id}", id).contentType(MediaType.APPLICATION_JSON).bodyValue(dto).retrieve().bodyToMono(String.class);
  }

  @DeleteMapping(value = "/{id}")
  public Mono<Void> eliminar(@PathVariable Long id) { return productosClient.delete().uri("/{id}", id).retrieve().bodyToMono(Void.class); }
}

