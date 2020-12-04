package com.jungleegames.apigateway.redis.dao;

public interface RedisDao {
	
	/**
	 * Retrieve key from redis
	 * @param key
	 * @return value
	 */
	String get(String key);
	
	/**
	 * Store value corresponding to a key with expiration time in redis.
	 * @param key
	 * @param value
	 * @param expireTimeInSeconds
	 * @return boolean representing key saved or not
	 */
	boolean store(String key, String value, long expireTimeInSeconds);
}
