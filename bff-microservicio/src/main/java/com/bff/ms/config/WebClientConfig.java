package com.bff.ms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

  private WebClient buildClient(String base) {
    String clean = base.endsWith("/") ? base.substring(0, base.length()-1) : base;
    return WebClient.builder()
        .baseUrl(clean)
        .filter((req, next) -> {
          System.out.println("[BFF] --> " + req.method() + " " + req.url());
          return next.exchange(req)
              .doOnNext(resp -> System.out.println("[BFF] <-- " + resp.statusCode().value() + " " + clean));
        })
        .build();
  }

  @Bean(name = "productosClient")
  WebClient productosClient(@Value("${functions.productosBaseUrl}") String base) {
    return buildClient(base);
  }

  @Bean(name = "bodegasClient")
  WebClient bodegasClient(@Value("${functions.bodegasBaseUrl}") String base) {
    return buildClient(base);
  }

  @Bean(name = "graphqlClient")
  WebClient graphqlClient(
      @Value("${functions.graphqlBaseUrl}") String base             

  ) {
    WebClient.Builder b = WebClient.builder()
        .baseUrl(base.endsWith("/") ? base.substring(0, base.length()-1) : base)
        .filter((req, next) -> {
          System.out.println("[BFF][GQL] --> " + req.method() + " " + req.url());
          return next.exchange(req)
              .doOnNext(resp -> System.out.println("[BFF][GQL] <-- " + resp.statusCode().value()));
        });

    return b.build();
  }

}
