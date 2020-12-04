package com.jungleegames.apigateway.filters;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.google.common.base.Strings;

import reactor.core.publisher.Mono;

@Component
public class PreGatewayFilter implements GlobalFilter{

	private static final String X_FORWARDED_FOR = "X-Forwarded-For";

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

		String forwarededHeader = exchange.getRequest().getHeaders().getFirst(X_FORWARDED_FOR);
		if(Strings.isNullOrEmpty(forwarededHeader)) {
			ServerHttpRequest modifiedRequest = exchange.getRequest().mutate().header(X_FORWARDED_FOR, 
					exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()).build();
			return chain.filter(exchange.mutate().request(modifiedRequest).build());
		}
		return chain.filter(exchange);
	}


}
