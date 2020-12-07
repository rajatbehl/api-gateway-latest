package com.jungleegames.apigateway.redis.dao;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jungleegames.apigateway.config.RedisConfig;

import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RedisDaoImpl implements RedisDao {

	@Autowired
	private RedisConfig redisConfig;
	private RedisAdvancedClusterCommands<String, String> commands;
	private static final String SUCCESSFUL_REPLY = "OK";
	
	@PostConstruct
	public void init() {
		commands = redisConfig.getConnection().sync();
	}
	
	@Override
	public String get(String key) {
		log.info("going to fetch value for key : " + key);
		return commands.get(key);
	}
	
	@Override
	public boolean store(String key, String value, long expireTimeInSeconds) {
		log.info("going to store key {} with value {} and expirationTime {}", key, value, expireTimeInSeconds);
		return SUCCESSFUL_REPLY.equals(commands.set(key, value)) && commands.expire(key, expireTimeInSeconds);
	}

}
