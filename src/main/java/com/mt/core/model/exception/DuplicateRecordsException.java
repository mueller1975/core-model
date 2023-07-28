package com.mt.core.model.exception;

/**
 * 重覆資料列 Exception
 * 
 * @author Mueller Tsai
 */
public class DuplicateRecordsException extends DAOException {

	private static final long serialVersionUID = 1L;

	public DuplicateRecordsException() {
		super();
	}

	public DuplicateRecordsException(String message) {
		super(message);
	}

	public DuplicateRecordsException(String message, Throwable cause) {
		super(message, cause);
	}

	public DuplicateRecordsException(Throwable cause) {
		super(cause);
	}
}
