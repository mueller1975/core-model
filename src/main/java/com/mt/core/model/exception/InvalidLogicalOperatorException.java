package com.mt.core.model.exception;

public class InvalidLogicalOperatorException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public InvalidLogicalOperatorException(String message) {
		super(message);
	}

	public InvalidLogicalOperatorException(String message, Throwable cause) {
		super(message, cause);
	}
}