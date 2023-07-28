package com.mt.core.model.vo;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ExportRequestVO extends PageRequestVO {

	private List<ExportColumnVO> exportColumns; // 匯出欄位
}
