package com.jungleegames.apigateway.config;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.jungleegames.apigateway.exceptions.ConfigurationMissingException;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.internal.HostAndPort;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@Configuration
public class RedisConfig {

	private RedisClusterClient clusterClient;
	private StatefulRedisClusterConnection<String, String> connection;
	
	@Autowired
	private ConsulConfigStore configStore;
	
	@PostConstruct
	public void init() {
		String configuredNodes = configStore.getString("spring.redis.cluster.nodes");
		log.info("received nodes : " + configuredNodes);
		if(configuredNodes == null) 
			throw new ConfigurationMissingException("Redis Nodes not configured");
		
		String[] nodes = configuredNodes.split(",");
		HostAndPort hostAndPort = null;
		RedisURI redisURI = null;
		
		List<RedisURI> redisURIs = new ArrayList<>();
		for(String node : nodes) {
			hostAndPort = HostAndPort.parse(node);
			redisURI = RedisURI.create(hostAndPort.getHostText(), hostAndPort.getPort());
			redisURIs.add(redisURI);
		}
		clusterClient = RedisClusterClient.create(redisURIs);
		log.info("partitions : " + clusterClient.getPartitions().toString());
		connection = clusterClient.connect();
	}
	
	@PreDestroy
	public void destroy() {
		connection.close();
		clusterClient.shutdown();
	}

}