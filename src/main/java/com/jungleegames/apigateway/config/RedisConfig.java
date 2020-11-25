package com.jungleegames.apigateway.config;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.internal.HostAndPort;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@RefreshScope
@ConfigurationProperties(value = "spring.redis.cluster")
@Configuration
@Data
@Slf4j
public class RedisConfig {

	private List<String> nodes;
	private RedisClusterClient clusterClient;
	private StatefulRedisClusterConnection<String, String> connection;
	
	@PostConstruct
	public void init() {
		HostAndPort hostAndPort = null;
		RedisURI redisURI = null;
		
		List<RedisURI> redisURIs = new ArrayList<>();
		for(String node : nodes) {
			hostAndPort = HostAndPort.parse(node);
			redisURI = RedisURI.create(hostAndPort.getHostText(), hostAndPort.getPort());
			redisURIs.add(redisURI);
		}
		log.info("preparing cluster wih nodes : " + redisURIs);
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