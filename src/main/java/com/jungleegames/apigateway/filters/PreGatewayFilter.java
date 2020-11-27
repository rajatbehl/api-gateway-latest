package com.jungleegames.apigateway.filters;

import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.google.gson.Gson;
import com.jungleegames.apigateway.redis.dao.RedisDao;

import reactor.core.publisher.Mono;

@Component
public class PreGatewayFilter implements GlobalFilter{

	@Autowired
	private RedisDao redisDao;
	
	private Gson gson = new Gson();

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

		HttpHeaders httpHeaders = exchange.getRequest().getHeaders();

		String tokenReceivd = httpHeaders.getFirst("token");
		String userId = httpHeaders.getFirst("userId");
		if(Strings.isEmpty(tokenReceivd) || Strings.isEmpty(userId)) {
			ServerHttpResponse response = exchange.getResponse();
			response.setStatusCode(HttpStatus.UNAUTHORIZED);

			return Mono.empty();
		}
		
		String userToken = redisDao.get(userId);
		if(userToken == null || !userToken.equals(tokenReceivd)) {
			ServerHttpResponse response = exchange.getResponse();
			response.setStatusCode(HttpStatus.UNAUTHORIZED);

			return Mono.empty();
		}
		
		return chain.filter(exchange);
	}
	
}
