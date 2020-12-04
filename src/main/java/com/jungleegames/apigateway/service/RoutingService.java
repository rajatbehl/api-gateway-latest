package com.jungleegames.apigateway.service;

import java.util.Set;

import org.springframework.cloud.gateway.route.RouteDefinition;

import com.jungleegames.apigateway.config.ConsulConfig;

public interface RoutingService {

	/**
	 * This method be responsible to initialize routes
	 * when service came up.
	 * @param consulConfig
	 */
	public void intializeRoutes(ConsulConfig consulConfig);
	
	/**
	 * This method will add new route.
	 * @param routeDefinition
	 */
	public void addRoute(RouteDefinition routeDefinition);
	
	/**
	 * This method will be called to update routes
	 * once there is update performed in consul.
	 * @param currentRoutes
	 */
	public void updateRoutes(Set<String> currentRoutes);
}
