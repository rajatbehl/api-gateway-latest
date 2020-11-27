package com.jungleegames.apigateway.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.jungleegames.apigateway.config.Config;
import com.jungleegames.apigateway.config.ConsulConfigStore;
import com.jungleegames.apigateway.routes.GatewayRoutesRefresher;
import com.orbitz.consul.KeyValueClient;

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
	private Config config;
	private Set<String> registeredRoutes = ConcurrentHashMap.newKeySet();
	private Gson gson = new Gson().newBuilder().setPrettyPrinting().create();
	
	@Override
	public void intializeRoutes(ConsulConfigStore configStore) {
		log.info("going to initialize gateway routes...");
		KeyValueClient keyValueClient = configStore.getKeyValueClient();
		keyValueClient.getKeys(config.getConsulPath().concat(config.getRoutingPathPrefix()))
			.stream().filter((key) -> !key.endsWith("/"))
			.forEach(key -> {
				String routeKey = key.replaceAll(config.getConsulPath(), "");
				keyValueClient.getValueAsString(routeKey).ifPresent(value -> {
					log.info("going to process Key {}, value {}", routeKey, value);
					RouteDefinition routeDefinition = gson.fromJson(value, RouteDefinition.class);
					this.addRoute(routeDefinition);
				});
			});
		
	}
	
	@Override
	public void addRoute(RouteDefinition routeDefinition) {
		log.info("adding route : " + routeDefinition);
		repository.save(Mono.just(routeDefinition)).subscribe();
			//.doOnSuccess(onSuccess -> log.info("route with id {} added successfully", routeDefinition.getId()))
			//.doOnError(onError -> log.error("error while adding route with id : " + routeDefinition.getId(), onError));
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
