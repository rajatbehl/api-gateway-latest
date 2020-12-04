package com.jungleegames.apigateway.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.jungleegames.apigateway.config.GatewayConfig;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PublicKeyS3AccessService implements PublicKeyAccessService {

	@Autowired
	private GatewayConfig config;
	
	@Autowired
	private ResourceLoader resourceLoader;
	
	@Override
	public String get(String kid) {
		String publicKey = null;
		Resource resource = resourceLoader.getResource(config.getAmazonS3Endpoint() + "/" + kid);
		try(InputStream inputStream = resource.getInputStream()) {
			publicKey = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
			log.info("received public key {} for kid {}", publicKey, kid);
		} catch (IOException ex) {
			log.error("error occured while loading public key for kid : " + kid, ex);
		}
		
		return publicKey;
	}

}
