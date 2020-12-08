package com.jungleegames.apigateway.service;

import java.security.Key;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.jose4j.jwa.AlgorithmConstraints.ConstraintType;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.keys.resolvers.VerificationKeyResolver;
import org.jose4j.lang.JoseException;
import org.jose4j.lang.UnresolvableKeyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.jungleegames.apigateway.model.AuthorizationResult;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AuthorizationServiceImpl implements AuthorizationService {

	@Autowired
	private PublicKeyAccessService publicKeyAccessService;
	
	private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
	private Gson gson = new Gson();
	
	/**
	 * This map will be used to store public key(which will be downloaded
	 * from s3) corresponding to kid received as a header in jwt,
	 * These public keys will be purged after a fixed interval to
	 * avoid any hacks, these keys will be rotated also at main/auth server side.
	 */
	private Map<String, String> publicKeys = new PassiveExpiringMap<>(30, TimeUnit.DAYS);
	
	@Override
	public AuthorizationResult authorize(String authToken, List<String> accessRoles) {
		
		if(Strings.isNullOrEmpty(authToken)) {
			log.error("Authorization header missing");
			return new AuthorizationResult(false);
		}
		
		String[] tokenArray = authToken.split("Bearer ");
		
		if(tokenArray == null || tokenArray.length != 2) {
			log.error("Invalid auth token");
			return new AuthorizationResult(false);
		}
		
		authToken = tokenArray[1];
		JwtConsumer jwtConsumer = new JwtConsumerBuilder()
				.setRequireExpirationTime()
				.setJwsAlgorithmConstraints(ConstraintType.PERMIT, AlgorithmIdentifiers.RSA_USING_SHA256)
				.setVerificationKeyResolver(new CustomVerificationKeyResolver())
				.build();
		
		JwtClaims jwtClaims = null;
		try {
			jwtClaims = jwtConsumer.processToClaims(authToken);
			log.info("JWT validation succeeded! " + jwtClaims);
			
		}catch(InvalidJwtException ex) {
			log.error("Invalid JWT! " + ex, ex);
			
			if(ex.hasExpired()) {
				try {
					log.info("JWT expired at " + ex.getJwtContext().getJwtClaims().getExpirationTime());
				} catch (MalformedClaimException e) {
					log.error("error occured while fetching expiration time for jwt", e);
				}
			}
			
			return new AuthorizationResult(false);
		}
		String requestAccessRole = jwtClaims.getClaimValueAsString("role");
		if(accessRoles != null && !accessRoles.contains(requestAccessRole)) {
			log.error("Illegal access role {}, allowed access roles are {}",requestAccessRole, accessRoles);
			return new AuthorizationResult(false);
		}
		
		AuthorizationResult result = new AuthorizationResult(true);
		result.setPayload(gson.toJson(jwtClaims.getClaimsMap()));
		return result;
	}
	
	class CustomVerificationKeyResolver implements VerificationKeyResolver {
		
		@Override
		public Key resolveKey(JsonWebSignature jws, List<JsonWebStructure> nestingContext) 
				throws UnresolvableKeyException {
			
			String keyId = jws.getKeyIdHeaderValue();
			if(keyId == null) {
				log.error("KID header value is missing");
				return null;
			}
			
			PublicJsonWebKey publicJsonWebKey = null;
			String publicKey = getPublicKey(keyId);
			if(publicKey == null) {
				log.error("public key not found for kid {}", keyId);
				return null;
			}
			try {
				publicJsonWebKey = PublicJsonWebKey.Factory.newPublicJwk(publicKey);
			} catch (JoseException e) {
				log.error("error occured while creating public key from kid value : " + keyId, e);
			}
			return publicJsonWebKey == null ? null : publicJsonWebKey.getKey();
		}

		private String getPublicKey(String keyId) {
			if(!UUID_PATTERN.matcher(keyId).matches()) {
				log.error("Invalid KID {}, it must be a valid UUID", keyId);
				return null;
			}
			
			String publicKey = publicKeys.get(keyId);
			if(publicKey == null) {
				publicKey = publicKeyAccessService.get(keyId);
				if(publicKey != null) {
					publicKeys.put(keyId, publicKey);
				}
			}
			return publicKey;
		}
	}

}
