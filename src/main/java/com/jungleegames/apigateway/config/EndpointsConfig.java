package com.jungleegames.apigateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "endpoints.url")
@Data
public class EndpointsConfig {
	
	private String raf;
	private String communication;
}
