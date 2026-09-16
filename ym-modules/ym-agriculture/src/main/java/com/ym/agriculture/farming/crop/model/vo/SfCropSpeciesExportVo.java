package com.ym.agriculture.farming.crop.model.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;

import java.io.Serial;
import java.io.Serializable;

/**
 * 作物品类导出对象。
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SfCropSpeciesVo.class)
public class SfCropSpeciesExportVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("品类编码")
    private String speciesCode;
    @ExcelProperty("品类名称")
    private String speciesName;
    @ExcelProperty("遥感编码")
    private Integer remoteSensingCode;
    @ExcelProperty("状态")
    private String status;
    @ExcelProperty("备注")
    private String remark;
}
