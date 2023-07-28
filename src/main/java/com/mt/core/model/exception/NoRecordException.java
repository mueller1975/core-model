package com.mt.core.model.exception;

/**
 * 查無資料列 Exception
 * 
 * @author Mueller Tsai
 */
public class NoRecordException extends ServiceException {

	private static final long serialVersionUID = 1L;

	public NoRecordException() {
		super("查詢結果為空");
	}

	public NoRecordException(String message) {
		super(message);
	}

	public NoRecordException(String message, Throwable cause) {
		super(message, cause);
	}
}
