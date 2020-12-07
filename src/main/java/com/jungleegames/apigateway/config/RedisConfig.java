package com.jungleegames.apigateway.config;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.boot.context.properties.ConfigurationProperties;
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
@ConfigurationProperties(value = "spring.redis.cluster")
@Configuration
public class RedisConfig {

	private RedisClusterClient clusterClient;
	private StatefulRedisClusterConnection<String, String> connection;
	
	private List<String> nodes;
	
	@PostConstruct
	public void init() {
		log.info("received nodes : " + nodes);
		if(nodes == null || nodes.isEmpty()) 
			throw new ConfigurationMissingException("Redis Nodes not configured");
		
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