package com.jungleegames.apigateway.service;

import reactor.core.publisher.Mono;

public interface PublicKeyAccessService {
	
	/**
	 * This method will be used to download/retrieve public
	 * key stored corresponding to a kid
	 * @param keyId
	 * @return {@link String} required public key
	 */
	Mono<String> get(String kid);
}
