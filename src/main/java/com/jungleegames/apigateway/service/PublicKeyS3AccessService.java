package com.jungleegames.apigateway.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.jungleegames.apigateway.config.ConsulConfig;
import com.jungleegames.apigateway.config.GatewayConfig;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PublicKeyS3AccessService implements PublicKeyAccessService {

	@Autowired
	private GatewayConfig config;
	
	@Autowired
	private ResourceLoader resourceLoader;
	
	@Autowired
	private ConsulConfig consulConfig;
	
	@Override
	public String get(String kid) {
		String publicKey = null;
		String amzaonS3EndPoint = consulConfig.getString("amazon.s3.endpoint", config.getAmazonS3Endpoint());
		Resource resource = resourceLoader.getResource(amzaonS3EndPoint + "/" + kid);
		try(InputStream inputStream = resource.getInputStream()) {
			publicKey = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
			log.info("received public key {} for kid {}", publicKey, kid);
		} catch (IOException ex) {
			log.error("error occured while loading public key for kid : " + kid, ex);
		}
		
		return publicKey;
	}

}
