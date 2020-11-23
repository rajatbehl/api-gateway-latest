package com.jungleegames.apigateway.routes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jungleegames.apigateway.config.EndpointsConfig;

@Configuration
public class CommunicationRoute {
	
	@Autowired
	private EndpointsConfig endpointsConfig;

	@Bean
	public RouteLocator communicationRoutes(RouteLocatorBuilder builder) {
		return builder.routes()
				.route("raf", r -> r.path("/comm/**")
						.filters(f -> f.rewritePath("/comm(?<segment>/?.*)", "$\\{segment}"))
						.uri(endpointsConfig.getCommunication())).build();
				
	}
	
}
