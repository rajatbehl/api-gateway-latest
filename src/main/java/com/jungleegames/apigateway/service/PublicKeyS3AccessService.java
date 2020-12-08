package com.jungleegames.apigateway.service;

import java.net.URI;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.jungleegames.apigateway.config.ConsulConfig;
import com.jungleegames.apigateway.config.GatewayConfig;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class PublicKeyS3AccessService implements PublicKeyAccessService {

	@Autowired
	private GatewayConfig config;
	
	@Autowired
	private ConsulConfig consulConfig;
	
	private WebClient webClient;
	
	@PostConstruct
	private void init() {
		webClient = WebClient.builder().build();
	}
	
	@Override
	public Mono<String> get(String kid) {
		String amzaonS3EndPoint = consulConfig.getString("amazon.s3.endpoint", config.getAmazonS3Endpoint()) + "/" + kid;
		
		return webClient.get().uri(URI.create(amzaonS3EndPoint))
			.exchangeToMono(response -> {
				if(response.statusCode().equals(HttpStatus.OK)) {
					return response.bodyToMono(String.class);
				}else {
					log.error("failed to fetch public key received {} status", response.statusCode());
					return Mono.empty();
				}
			});
	}

}
