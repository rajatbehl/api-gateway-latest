package com.jungleegames.apigateway.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(value = Include.NON_NULL)
public class AuthorizationResult {

	private boolean valid;
	private String payload;
	private Result result = Result.SUCCESS;
	
	public static enum Result {
		SUCCESS,
		TOKEN_MISSING,
		TOKEN_PARSING_FAILED,
		KID_HEADER_MISSING,
		INVALID_TOKEN_KID,
		PUBLIC_KEY_NOT_FOUND,
		INVALID_TOKEN,
		ILLEGAL_ACCESS_ROLE,
		TOKEN_EXPIRED
	}
	
	public AuthorizationResult(Result result) {
		this.result  = result;
	}
	
	public AuthorizationResult(boolean valid) {
		this.valid  = valid;
	}
	
}
