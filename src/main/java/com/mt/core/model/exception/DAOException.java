package com.mt.core.model.exception;

/**
 * General DAO Exception
 * 
 * @author Mueller Tsai, 2018.09.21
 */
public class DAOException extends Exception {
	private static final long serialVersionUID = 1L;

	public DAOException() {
		super();
	}

	public DAOException(String message) {
		super(message);
	}

	public DAOException(String message, Throwable cause) {
		super(message, cause);
	}

	public DAOException(Throwable cause) {
		super(cause);
	}
}
