package com.jungleegames.apigateway.config;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.context.annotation.Configuration;

import com.google.gson.Gson;
import com.jungleegames.apigateway.service.RoutingService;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.cache.ConsulCache.Listener;
import com.orbitz.consul.cache.KVCache;
import com.orbitz.consul.model.kv.Value;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Getter
@Slf4j
public class ConsulConfig {

	@Autowired
	private GatewayConfig config;
	
	@Autowired
	private RoutingService routingService;
	
	private KVCache cache;
	private KeyValueClient keyValueClient;
	/**
	 * This map will hold the key-value mapping stored in consul and 
	 * will get updated if there is some updates in consul.
	 */
	private Map<String, Object> configs = new ConcurrentHashMap<>();
	private Gson gson = new Gson();

	@PostConstruct
	public void init() {
		log.info("initializing consul config store with url {} and path {}",config.getConsulURL(), config.getConsulPath());
		Consul client = Consul.builder().withUrl(config.getConsulURL()).build();
		keyValueClient = client.keyValueClient();
		cache = KVCache.newCache(keyValueClient, config.getConsulPath());
		cache.addListener(consulListener());
		cache.start();
		routingService.intializeRoutes(this);
	}	
	
	public String getString(String key, String defaultValue) {
		Object value = configs.get(key);
		if(value == null) {
			configs.put(key, defaultValue);
		}
		return String.valueOf(configs.get(key));
	}
	
	public long getLong(String key, long defauleValue) {
		Object temp = configs.get(key);
		if(temp == null) {
			configs.put(key, defauleValue);
		}
		
		long value = 0;
		try {
			value = Long.parseLong(String.valueOf(configs.get(key)));
		}catch(NumberFormatException ex) {
			log.error("invalid long value : " + value);
		}
		
		return value;
	}

	public boolean getBoolean(String key, boolean defaultValue) {
		Object temp = configs.get(key);
		if(temp == null) {
			configs.put(key, defaultValue);
		}
		
		return Boolean.valueOf(String.valueOf(configs.get(key)));
	}
	
	@PreDestroy
	public void destroy() {
		cache.stop();
	}
	
	private Listener<String, Value> consulListener() {
		return newValues -> {
			log.info("received callback for consul property changes...");
			Set<String> currentlyRegisteredRoutes = new HashSet<>();
			newValues.values().stream().forEach(keyValue -> {
				keyValue.getValueAsString().ifPresent(value -> {
					String key = keyValue.getKey().replace(config.getConsulPath(), "");// To trim out the path
					if (!key.isEmpty()) {
						log.info("Key: " + key + " Value: " + value);
						configs.put(key, value);
					}
					if(key.startsWith(config.getRoutingPathPrefix())) {
						RouteDefinition routeDefinition = gson.fromJson(value, RouteDefinition.class);
						currentlyRegisteredRoutes.add(routeDefinition.getId());
						routingService.addRoute(routeDefinition);
					}
				});
			});
			
			/**
			 * Called to maintain only those routes
			 * which are currently configured in consul.
			 */
			routingService.updateRoutes(currentlyRegisteredRoutes);
		};
	}
	
}
