package com.ym.agriculture.farming.crop.model.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;

import java.io.Serial;
import java.io.Serializable;

/**
 * 作物品种导出对象。
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SfCropVarietyVo.class)
public class SfCropVarietyExportVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("所属品类")
    private String speciesName;
    @ExcelProperty("品种编码")
    private String varietyCode;
    @ExcelProperty("品种名称")
    private String varietyName;
    @ExcelProperty("生长周期（天）")
    private Integer growthCycleDays;
    @ExcelProperty("状态")
    private String status;
    @ExcelProperty("备注")
    private String remark;
}
