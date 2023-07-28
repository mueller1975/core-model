package com.mt.core.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * 前端表格匯出欄位 VO
 * 
 * @author Mueller Tsai
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExportColumnVO {

	private String prop; // 後端 entity 屬性
	private String name; // 匯出 header title
}
