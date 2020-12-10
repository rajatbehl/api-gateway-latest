package com.jungleegames.apigateway.filters;

import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
public class LoggingGatewayFilterFactory implements GatewayFilterFactory<LoggingGatewayFilterFactory.Config> {

	@Data
	public static class Config {
		private boolean requestMaskEnable;
		private boolean responseMaskEnable;
		private boolean requestLogEnable;
		private boolean responseLogEnable;
		private List<String> requestMaskKeys;
		private List<String> responseMaskKeys;

	}

	@Override
	public GatewayFilter apply(Config config) {
		return new LoggingFilter(config);
	}


	@Override
	public Class<Config> getConfigClass() {
		return LoggingGatewayFilterFactory.Config.class;
	}
}
