package com.bff.ms.api;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/bff/bodegas")
public class BodegasController {
  private final WebClient bodegasClient;
  public BodegasController(@Qualifier("bodegasClient") WebClient bodegasClient) { this.bodegasClient = bodegasClient; }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<String> listar() { return bodegasClient.get().retrieve().bodyToMono(String.class); }

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<String> uno(@PathVariable Long id) { return bodegasClient.get().uri("/{id}", id).retrieve().bodyToMono(String.class); }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<String> crear(@RequestBody BodegaDto dto) {
    return bodegasClient.post().contentType(MediaType.APPLICATION_JSON).bodyValue(dto).retrieve().bodyToMono(String.class);
  }

  @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<String> actualizar(@PathVariable Long id, @RequestBody BodegaDto dto) {
    return bodegasClient.put().uri("/{id}", id).contentType(MediaType.APPLICATION_JSON).bodyValue(dto).retrieve().bodyToMono(String.class);
  }

  @DeleteMapping(value = "/{id}")
  public Mono<Void> eliminar(@PathVariable Long id) { return bodegasClient.delete().uri("/{id}", id).retrieve().bodyToMono(Void.class); }
}

