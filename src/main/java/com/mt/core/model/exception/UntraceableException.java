package com.mt.core.model.exception;

public class UntraceableException extends ServiceException {

	private static final long serialVersionUID = 1L;

	public UntraceableException(String errorMessage) {
		super(errorMessage);
	}

}
