package com.ym.agriculture.farming.field.model.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 地块档案导出对象。
 *
 * @author ym-cloud
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SfFieldVo.class)
public class SfFieldExportVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("地块编码")
    private String fieldCode;

    @ExcelProperty("地块名称")
    private String fieldName;

    @ExcelProperty("地块类型")
    private String fieldType;

    @ExcelProperty("大棚简称")
    private String greenhouseShortName;

    @ExcelProperty("面积（亩）")
    private BigDecimal areaMu;

    @ExcelProperty("行政区划")
    private String adminDivisionText;

    @ExcelProperty("地址")
    private String addressText;

    @ExcelProperty("启用状态")
    private String status;

    @ExcelProperty("业务状态")
    private String fieldStatus;

    @ExcelProperty("创建时间")
    private Date createTime;

    @ExcelProperty("备注")
    private String remark;
}
