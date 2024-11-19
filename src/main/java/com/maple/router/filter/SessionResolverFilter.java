package com.maple.router.filter;

import lombok.val;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Component
public class SessionResolverFilter extends AbstractGatewayFilterFactory {

  private final WebClient webClient = WebClient.create();

  @Override
  public GatewayFilter apply(final Object config) {
    return (exchange, chain) -> {
      val request = exchange.getRequest();
      val authHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
      if (authHeader == null || authHeader.size() != 1) {
        throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED);
      }
      return webClient
          .get()
          .uri("http://localhost:8082/api/v1/auth/session")
          .header(HttpHeaders.AUTHORIZATION, authHeader.get(0))
          .retrieve()
          .bodyToMono(Session.class)
          .flatMap(
              response -> {
                val roleArray = new String[response.getRoles().size()];
                val newRequest =
                    request
                        .mutate()
                        .header("X-User-Roles", response.getRoles().toArray(roleArray))
                        .header("X-User-Id", String.valueOf(response.getUserId()))
                        .header(
                            "X-Organization-Id",
                            String.valueOf(response.getOrganizationId()))
                        .build();
                return chain.filter(exchange.mutate().request(newRequest).build());
              })
          .onErrorResume(
              err -> {
                val httpStatus =
                    err instanceof WebClientResponseException
                        ? ((WebClientResponseException) err).getStatusCode()
                        : HttpStatus.INTERNAL_SERVER_ERROR;
                return Mono.defer(
                    () ->
                        setErrorResponse(exchange.getResponse(), httpStatus)
                            .setComplete()
                            .then(Mono.empty()));
              });
    };
  }

  private ServerHttpResponse setErrorResponse(
      final ServerHttpResponse serverHttpResponse, final HttpStatusCode statusCode) {
    serverHttpResponse.setStatusCode(statusCode);
    return serverHttpResponse;
  }
}