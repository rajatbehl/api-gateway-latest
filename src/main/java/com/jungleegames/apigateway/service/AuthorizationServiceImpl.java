package com.jungleegames.apigateway.service;

import java.security.Key;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

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
import com.jungleegames.apigateway.config.GatewayConfig;
import com.jungleegames.apigateway.model.AuthorizationResult;
import com.jungleegames.apigateway.model.AuthorizationResult.Result;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class AuthorizationServiceImpl implements AuthorizationService {

	@Autowired
	private PublicKeyAccessService publicKeyAccessService;
	
	@Autowired
	private GatewayConfig config;

	private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
	private Gson gson = new Gson();
	private JwtConsumer jwtConsumer = null;

	/**
	 * This map will be used to store public key(which will be downloaded
	 * from s3) corresponding to kid received as a header in jwt,
	 * These public keys will be purged after a fixed interval to
	 * avoid any hacks, these keys will be rotated also at main/auth server side.
	 */
	private Map<String, PublicJsonWebKey> publicKeys = null;
	
	@PostConstruct
	public void init() {
		JwtConsumerBuilder builder = new JwtConsumerBuilder()
				.setRequireExpirationTime()
				.setJwsAlgorithmConstraints(ConstraintType.PERMIT, AlgorithmIdentifiers.RSA_USING_SHA256)
				.setVerificationKeyResolver(new CustomVerificationKeyResolver());
				
		if(config.isJwtAuthDisabled()) {
			builder.setSkipSignatureVerification();
		}
		
		jwtConsumer = builder.build();
		publicKeys = new PassiveExpiringMap<>(30, TimeUnit.DAYS);
	}

	@Override
	public Mono<AuthorizationResult> authorize(String authHeader, List<String> accessRoles) {

		if(Strings.isNullOrEmpty(authHeader) || !authHeader.startsWith("Bearer ")) {
			log.error("Missing authorization Bearer token");
			return Mono.just(new AuthorizationResult(Result.TOKEN_MISSING));
		}

		String authToken = authHeader.split("Bearer ")[1];
		JsonWebStructure jws = null;
		try {
			jws = JsonWebStructure.fromCompactSerialization(authToken);
		} catch (JoseException ex) {
			log.error("failed to parse jwt", ex);
			return Mono.just(new AuthorizationResult(Result.TOKEN_PARSING_FAILED));
		}	
		String kid = jws.getKeyIdHeaderValue();
		if(kid == null) {
			log.error("KID header value is missing");
			return Mono.just(new AuthorizationResult(Result.KID_HEADER_MISSING));
		}
		
		if(!UUID_PATTERN.matcher(kid).matches()) {
			log.error("Invalid KID {}, it must be a valid UUID", kid);
			return Mono.just(new AuthorizationResult(Result.INVALID_TOKEN_KID));
		}
		
		if(config.isJwtAuthDisabled()) {
			return processJWTClaims(authToken, accessRoles); 
		}else {
			return refreshPublicKeys(kid)
					.flatMap(publicKey -> {
						return processJWTClaims(authToken, accessRoles);
					}).switchIfEmpty(Mono.defer(() -> {
						log.error("Public key not found for kid {}", kid);
						return Mono.just(new AuthorizationResult(Result.PUBLIC_KEY_NOT_FOUND));
					}));
		}

	}

	private Mono<AuthorizationResult> processJWTClaims(String authToken, List<String> accessRoles) {
		JwtClaims jwtClaims = null;
		try {
			jwtClaims = jwtConsumer.processToClaims(authToken);
			log.info("JWT validation succeeded! " + jwtClaims);
		}catch(InvalidJwtException ex) {
			log.error("Invalid JWT! " + ex, ex);
			AuthorizationResult result = new AuthorizationResult(Result.INVALID_TOKEN);
			if(ex.hasExpired()) {
				try {
					log.info("JWT expired at " + ex.getJwtContext().getJwtClaims().getExpirationTime());
				} catch (MalformedClaimException e) {
					log.error("error occured while fetching expiration time for jwt", e);
				}
				result.setResult(Result.TOKEN_EXPIRED);
			}

			return Mono.just(result);
		}

		String requestAccessRole = jwtClaims.getClaimValueAsString("role");
		if(accessRoles != null && !accessRoles.contains(requestAccessRole)) {
			log.error("Illegal access role {}, allowed access roles are {}",requestAccessRole, accessRoles);
			return Mono.just(new AuthorizationResult(Result.ILLEGAL_ACCESS_ROLE));
		}

		AuthorizationResult result = new AuthorizationResult(true);
		result.setPayload(gson.toJson(jwtClaims.getClaimsMap()));
		return Mono.just(result);
	}

	private Mono<Key> refreshPublicKeys(String kid) {
		if(!publicKeys.containsKey(kid)) {
			log.info("loading public key from s3 for kid {}", kid);
			return publicKeyAccessService.get(kid)
					.flatMap(publicKey -> {
						try {
							PublicJsonWebKey publicJsonWebKey = PublicJsonWebKey.Factory.newPublicJwk(publicKey);
							publicKeys.put(kid, publicJsonWebKey);
							return Mono.just(publicJsonWebKey.getKey());
						} catch (JoseException e) {
							return Mono.empty();
						}
					});
		}
		return Mono.just(publicKeys.get(kid).getKey());
	}

	class CustomVerificationKeyResolver implements VerificationKeyResolver {

		@Override
		public Key resolveKey(JsonWebSignature jws, List<JsonWebStructure> nestingContext) 
				throws UnresolvableKeyException {
			PublicJsonWebKey publicJsonWebKey = null;
			publicJsonWebKey = publicKeys.get(jws.getKeyIdHeaderValue());
			return publicJsonWebKey == null ? null : publicJsonWebKey.getKey();
		}

	}

}
