package com.jungleegames.apigateway.exceptions;

public class ConfigurationMissingException extends RuntimeException{

	private static final long serialVersionUID = 1l;
	
	public ConfigurationMissingException(String message) {
		super(message);
	}
}
