package com.jungleegames.apigateway.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AuthorizationResult {

	private boolean valid;
	private String payload;
	
	public AuthorizationResult(boolean valid) {
		this.valid = valid;
	}
}
