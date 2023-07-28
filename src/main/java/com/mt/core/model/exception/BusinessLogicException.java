package com.mt.core.model.exception;

/**
 * Business Logic Exception
 * 
 * @author Mueller Tsai, 2019.06.26
 */
public class BusinessLogicException extends Exception {

	private static final long serialVersionUID = 1L;

	public BusinessLogicException(String errorMessage) {
		super(errorMessage);
	}

	public BusinessLogicException(String errorMessage, Throwable err) {
		super(errorMessage, err);
	}
}
