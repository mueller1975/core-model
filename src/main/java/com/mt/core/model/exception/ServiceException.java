package com.mt.core.model.exception;

/**
 * Model Service Exception
 * 
 * @author Mueller Tsai, 2019.05.14
 */
public class ServiceException extends Exception {

	private static final long serialVersionUID = 1L;

	public ServiceException(String errorMessage) {
		super(errorMessage);
	}

	public ServiceException(String errorMessage, Throwable err) {
		super(errorMessage, err);
	}
}
