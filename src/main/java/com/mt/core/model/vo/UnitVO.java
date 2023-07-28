package com.mt.core.model.vo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor(access = AccessLevel.PUBLIC)
@Data
public class UnitVO {

	private String costNo;

	private String costName;
}
