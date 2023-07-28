package com.mt.core.model.exception;

public class InvalidColumnNameMappingException extends Exception {
	private static final long serialVersionUID = 1L;

	public InvalidColumnNameMappingException(String message) {
		super(message);
	}

	public InvalidColumnNameMappingException(String message, Throwable cause) {
		super(message, cause);
	}
}