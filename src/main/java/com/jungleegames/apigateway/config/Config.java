package com.jungleegames.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@Data
public class Config {

	@Value("${consul.url}")
	private String consulURL;
	
	@Value("${consul.path}")
	private String consulPath;
	
	@Value("${routing-path.prefix}")
	private String routingPathPrefix;
}
