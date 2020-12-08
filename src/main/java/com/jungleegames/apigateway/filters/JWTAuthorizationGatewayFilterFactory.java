package com.jungleegames.apigateway.filters;

import static org.springframework.cloud.gateway.support.GatewayToStringStyler.filterToStringCreator;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.google.gson.Gson;
import com.jungleegames.apigateway.model.AuthorizationResult;
import com.jungleegames.apigateway.service.AuthorizationService;

import lombok.Data;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * This is custom filter to achieve authentication of routes.
 * The Authentication GatewayFilter factory takes allowedRoles 
 * parameter. This filter can be used to mark any route to be 
 * authenticated first;along with that access roles can be defined
 * to restrict to specific set of roles. The following listing
 * configures a Authentication GatewayFilter:
 * 
 * application.yml
 spring:
  application:
    name: API-Gateway
  cloud:
    gateway:
      routes:
      - id: jwtauthorization_route
        uri:
          https://example.org
        predicates:
        - Path=/users/**
        filters:
        - name: JWTAuthorization
          args:
            accessRoles: "jwr-player, admin"
 * 
 * For a request path of /users/* downstream request will be 
 * authenticated first.
 * @author rbehl
 *
 */
@Component
public class JWTAuthorizationGatewayFilterFactory implements GatewayFilterFactory<JWTAuthorizationGatewayFilterFactory.Config>{

	@Autowired
	private AuthorizationService authenticationService;
	
	private Gson gson = new Gson();
	private static final String JWT_PAYLOAD = "JWT-Payload";
	
	@Override
	public Class<Config> getConfigClass() {
		return JWTAuthorizationGatewayFilterFactory.Config.class;
	}
	
	@Override
	public GatewayFilter apply(Config config) {

		return new GatewayFilter() {

			@Override
			public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
				ServerHttpRequest request = exchange.getRequest();
				Mono<AuthorizationResult> authorizationResult = authenticationService.authorize(request.getHeaders()
						.getFirst(HttpHeaders.AUTHORIZATION), config.getAccessRoles());
				
				return authorizationResult.flatMap(result -> {
					if(Boolean.TRUE.equals(result.isValid())) {
						ServerHttpRequest modifiedRequest = request.mutate().header(JWT_PAYLOAD, result.getPayload()).build();
						return chain.filter(exchange.mutate().request(modifiedRequest).build());
					}else {
						exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
						byte[] response = gson.toJson(result).getBytes(StandardCharsets.UTF_8);
						DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(response);
					    return exchange.getResponse().writeWith(Flux.just(buffer));
					}
				});
			}

			@Override
			public String toString() {
				return filterToStringCreator(JWTAuthorizationGatewayFilterFactory.this)
						.append(config.getAccessRoles()). toString();
			}
		};
	}
	
	@Data
	public static class Config {
		private List<String> accessRoles;
		
	}
	
}
