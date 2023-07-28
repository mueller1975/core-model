package com.mt.core.model.vo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageRequestVO {

	private int page; // page index, 0 based
	private int size; // row count per page

	// 欲排序的屬性 (多個)
	private List<SortDescriptor> sortProps; // newly created, 2021.12.2

	private FilterDescriptor filter;

	@Data
	public static class SortDescriptor {
		private String field;
		private String dir;
	}

	@Data
	public static class Filter {
		private String logic;
		private List<FilterDescriptor> subFilters;
	}

	@Data
	public static class FilterDescriptor {
		private String field;
		private Object value;
		private String operator;

		private Filter filter;
	}

}
