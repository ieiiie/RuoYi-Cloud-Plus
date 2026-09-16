package com.ym.agriculture.farming.crop.model.vo;

import org.apache.fesod.sheet.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 农作物物种导入视图对象
 *
 * @author ym-cloud
 */
@Data
public class SfCropSpeciesImportVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 物种编号
     */
    @ExcelProperty(value = "物种编号")
    private String speciesCode;

    /**
     * 物种名称
     */
    @ExcelProperty(value = "物种名称")
    private String speciesName;

    /**
     * 遥感编号
     */
    @ExcelProperty(value = "遥感编号")
    private Integer remoteSensingCode;

    /**
     * 状态（0正常 1停用）
     */
    @ExcelProperty(value = "状态")
    private String status;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

}
