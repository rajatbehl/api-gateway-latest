package com.jungleegames.apigateway.routes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.jungleegames.apigateway.config.EndpointsConfig;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class RafRoute {
	
	@Autowired
	private EndpointsConfig endpointsConfig;
	
	@Bean
	public RouteLocator rafRoutes(RouteLocatorBuilder builder) {
		log.info("checking route...");
		return builder.routes()
				.route("raf", r -> r.path("/raf/**")
						.filters(f -> f.rewritePath("/raf(?<segment>/?.*)", "$\\{segment}"))
						.uri(endpointsConfig.getRaf())).build();
				
	}

}
