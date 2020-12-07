package com.jungleegames.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@Data
public class GatewayConfig {

	@Value("${consul.url}")
	private String consulURL;
	
	@Value("${consul.path}")
	private String consulPath;
	
	@Value("${routing-path.prefix}")
	private String routingPathPrefix;
	
	@Value("${amazon.s3.endpoint}")
	private String amazonS3Endpoint;
	
	@Value("${jwt-auth.expiration-time}")
	private int jwtExpirationTime;
	
	@Value("${jwt-auth.enabled}")
	private boolean jwtAuthEnabled;
	
}
