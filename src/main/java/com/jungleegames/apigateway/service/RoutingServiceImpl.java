package com.jungleegames.apigateway.service;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.jungleegames.apigateway.config.GatewayConfig;
import com.jungleegames.apigateway.config.ConsulConfig;
import com.jungleegames.apigateway.routes.GatewayRoutesRefresher;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.model.kv.Value;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class RoutingServiceImpl implements RoutingService{

	@Autowired
	private RouteDefinitionRepository repository;
	
	@Autowired
	private GatewayRoutesRefresher refresher;
	
	@Autowired
	private GatewayConfig config;
	
	private Set<String> registeredRoutes = ConcurrentHashMap.newKeySet();
	private Gson gson = new Gson().newBuilder().setPrettyPrinting().create();
	
	@Override
	public void intializeRoutes(ConsulConfig consulConfig) {
		KeyValueClient keyValueClient = consulConfig.getKeyValueClient();
		Optional<Value> routesConfigured = keyValueClient.getValue(config.getConsulPath().concat(config.getRoutingPathPrefix()));
		if(routesConfigured.isPresent()) {
			log.info("going to initialize gateway routes...");
			keyValueClient.getKeys(config.getConsulPath().concat(config.getRoutingPathPrefix()))
			.stream().filter((key) -> !key.endsWith("/"))
			.forEach(key -> {
				String routeKey = key.replaceAll(config.getConsulPath(), "");
				keyValueClient.getValueAsString(routeKey).ifPresent(value -> {
					log.info("going to process Key {}, value {}", routeKey, value);
					RouteDefinition routeDefinition = gson.fromJson(value, RouteDefinition.class);
					log.info("received route definition : " + routeDefinition);
					this.addRoute(routeDefinition);
				});
			});
		}else {
			log.info("gateway routes are not configured yet");
		}
		
		
	}
	
	@Override
	public void addRoute(RouteDefinition routeDefinition) {
		log.info("adding route : " + routeDefinition);
		repository.save(Mono.just(routeDefinition)).subscribe();
		registeredRoutes.add(routeDefinition.getId());
		refreshRoutes();
	}
	
	@Override
	public void updateRoutes(Set<String> currentRoutes) {
		registeredRoutes.forEach(routeId -> {
			if(!currentRoutes.contains(routeId)) {
				deleteRoute(routeId);
				registeredRoutes.remove(routeId);
			}
		});
	}
	
	private void deleteRoute(String routeId) {
		log.info("going to remove route {}", routeId);
		repository.delete(Mono.just(routeId)).subscribe();
		refreshRoutes();
	}
	
	private void refreshRoutes() {
		refresher.refreshRoutes();
	}
	
}
