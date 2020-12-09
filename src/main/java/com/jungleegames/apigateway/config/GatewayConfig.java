package com.jungleegames.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@Data
public class GatewayConfig {

	@Value("${consul.host}")
	private String consulHost;
	
	@Value("${consul.port}")
	private int consulPort;
	
	@Value("${consul.path}")
	private String consulPath;
	
	@Value("${consul.token}")
	private String consulToken;
	
	@Value("${routing-path.prefix}")
	private String routingPathPrefix;
	
	@Value("${amazon.s3.endpoint}")
	private String amazonS3Endpoint;
	
	@Value("${jwt-auth.expiration-time}")
	private int jwtExpirationTime;
	
	@Value("${jwt-auth.disabled}")
	private boolean jwtAuthDisabled;
	
}
