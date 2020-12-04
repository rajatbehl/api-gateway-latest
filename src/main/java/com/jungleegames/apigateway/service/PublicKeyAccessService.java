package com.jungleegames.apigateway.service;

public interface PublicKeyAccessService {
	
	/**
	 * This method will be used to download/retrieve public
	 * key stored corresponding to a kid
	 * @param keyId
	 * @return {@link String} required public key
	 */
	String get(String kid);
}
