package com.ym.agriculture.farming.batch.model.vo;

import lombok.Data;
import org.apache.fesod.sheet.annotation.ExcelProperty;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 种植批次导出行。
 */
@Data
public class SfPlantingBatchExportVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("批次编号")
    private String batchCode;
    @ExcelProperty("地块")
    private String fieldName;
    @ExcelProperty("品种")
    private String varietyName;
    @ExcelProperty("物种")
    private String speciesName;
    @ExcelProperty("播种日期")
    private Date sowingDate;
    @ExcelProperty("预计采收")
    private Date expectedHarvestDate;
    @ExcelProperty("实际采收")
    private Date actualHarvestDate;
    @ExcelProperty("状态")
    private String batchStatus;
    @ExcelProperty("第几茬")
    private Integer croppingIndex;
    @ExcelProperty("创建时间")
    private Date createTime;
    @ExcelProperty("备注")
    private String remark;
}
