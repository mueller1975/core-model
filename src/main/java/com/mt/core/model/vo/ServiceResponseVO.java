package com.mt.core.model.vo;

import lombok.Data;

@Data
public class ServiceResponseVO {

	public static final int DEFAULT_FAIL_CODE = 100;
	protected int code;
	protected Object data;

	public ServiceResponseVO() {
	}

	public ServiceResponseVO(int code, Object data) {
		this.code = code;
		this.data = data;
	}

	public static ServiceResponseVO succeed() {
		return new ServiceResponseVO(0, "SUCCESS");
	}

	public static ServiceResponseVO succeed(Object data) {
		return new ServiceResponseVO(0, data);
	}

	public static ServiceResponseVO fail(int code, Object data) {
		return new ServiceResponseVO(code, data);
	}

	public static ServiceResponseVO fail(Object data) {
		return new ServiceResponseVO(DEFAULT_FAIL_CODE, data);
	}

	public static ServiceResponseVO fail(Throwable e) {
		String message = e.getMessage();

		if (e.getCause() != null) {
			message = String.format("%s (%s)", message, e.getCause().getMessage());
		}

		return new ServiceResponseVO(DEFAULT_FAIL_CODE, message);
	}
}
