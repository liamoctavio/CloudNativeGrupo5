package com.bff.ms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
  @Bean(name = "productosClient")
  WebClient productosClient(@Value("${functions.productosBaseUrl}") String base) {
    return WebClient.builder().baseUrl(base).build();
  }
  @Bean(name = "bodegasClient")
  WebClient bodegasClient(@Value("${functions.bodegasBaseUrl}") String base) {
    return WebClient.builder().baseUrl(base).build();
  }
}

