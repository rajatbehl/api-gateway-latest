package com.jungleegames.apigateway.filters;

import static org.springframework.cloud.gateway.support.GatewayToStringStyler.filterToStringCreator;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.jungleegames.apigateway.config.ConsulConfig;
import com.jungleegames.apigateway.config.GatewayConfig;
import com.jungleegames.apigateway.model.AuthorizationResult;
import com.jungleegames.apigateway.service.AuthorizationService;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class JWTAuthorizationGatewayFilterFactory implements GatewayFilterFactory<JWTAuthorizationGatewayFilterFactory.Config>{

	@Autowired
	private AuthorizationService authenticationService;
	
	@Autowired
	private GatewayConfig gatewayConfig;
	
	@Autowired
	private ConsulConfig consulConfig;
	
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
				if(!consulConfig.getBoolean("jwt-auth.enabled", gatewayConfig.isJwtAuthEnabled())) {
					log.warn("JWT authorization is disabled currently.");
					return chain.filter(exchange);
				}
				
				AuthorizationResult authenticationResult = authenticationService.authorize(request.getHeaders()
						.getFirst(HttpHeaders.AUTHORIZATION), config.getAccessRoles());
				if(!authenticationResult.isValid()) {
					exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
					return Mono.empty();
				}
				
				ServerHttpRequest modifiedRequest = request.mutate().header(JWT_PAYLOAD, authenticationResult.getPayload()).build();
				return chain.filter(exchange.mutate().request(modifiedRequest).build());
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
