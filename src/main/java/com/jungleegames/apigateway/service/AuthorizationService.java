package com.jungleegames.apigateway.service;

import java.util.List;

import com.jungleegames.apigateway.filters.JWTAuthorizationGatewayFilterFactory;
import com.jungleegames.apigateway.model.AuthorizationResult;

import reactor.core.publisher.Mono;

public interface AuthorizationService {
	
	/**
	 * This method will be used to authenticate the incoming
	 * request. This will be used mostly for {@link JWTAuthorizationGatewayFilterFactory}.
	 * @param authToken
	 * @param accessRoles
	 * @return {@link AuthorizationResult}
	 */
	Mono<AuthorizationResult> authorize(String authHeader, List<String> accessRoles);
}
