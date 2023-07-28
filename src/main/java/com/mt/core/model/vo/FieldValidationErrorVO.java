package com.mt.core.model.vo;

import lombok.Data;

@Data
public class FieldValidationErrorVO {
	String field;
	Object result;

	public FieldValidationErrorVO() {
	}

	public FieldValidationErrorVO(String field, Object result) {
		this.field = field;
		this.result = result;
	}
}
