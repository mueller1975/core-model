package com.mt.core.model.vo;

import java.util.Collections;
import java.util.List;

import lombok.Data;

@Data
public class PageResponseVO<T> {

	private int total;
	private List<T> rows;

	public PageResponseVO() {
	}

	public PageResponseVO(int total, List<T> rows) {
		this.total = total;
		this.rows = rows;
	}

	public static PageResponseVO<?> emptyResult() {
		return new PageResponseVO<>(0, Collections.emptyList());
	}
}
