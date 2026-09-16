package com.ym.system.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import com.ym.common.excel.annotation.ExcelDictFormat;
import com.ym.common.excel.convert.ExcelDictConvert;
import com.ym.system.domain.SysConfig;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;


/**
 * 参数配置视图对象 sys_config
 *
 * @author Michelle.Chung
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SysConfig.class)
public class SysConfigVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 参数主键
     */
    @ExcelProperty(value = "参数主键")
    private Long configId;

    /** 参数定义ID。 */
    private Long definitionId;

    /** 参数值类型。 */
    private String valueType;

    /** 租户是否允许编辑。 */
    private Boolean tenantEditable;

    /** 是否必填。 */
    private Boolean requiredFlag;

    /** 最小长度。 */
    private Integer minLength;

    /** 最大长度。 */
    private Integer maxLength;

    /** 最小数值。 */
    private BigDecimal minValue;

    /** 最大数值。 */
    private BigDecimal maxValue;

    /** 正则校验表达式。 */
    private String regexPattern;

    /** 枚举选项 JSON。 */
    private String enumOptions;

    /**
     * 参数名称
     */
    @ExcelProperty(value = "参数名称")
    private String configName;

    /**
     * 参数键名
     */
    @ExcelProperty(value = "参数键名")
    private String configKey;

    /**
     * 参数键值
     */
    @ExcelProperty(value = "参数键值")
    private String configValue;

    /**
     * 系统内置（Y是 N否）
     */
    @ExcelProperty(value = "系统内置", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_yes_no")
    private String configType;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private LocalDateTime createTime;

}
