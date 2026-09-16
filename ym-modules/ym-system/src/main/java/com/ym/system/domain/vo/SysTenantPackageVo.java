package com.ym.system.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import com.ym.common.excel.annotation.ExcelDictFormat;
import com.ym.common.excel.convert.ExcelDictConvert;
import com.ym.system.domain.SysTenantPackage;

import java.io.Serial;
import java.io.Serializable;

/**
 * 租户套餐视图对象。
 *
 * @author Lion Li
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SysTenantPackage.class)
public class SysTenantPackageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("套餐主键")
    private Long packageId;

    @ExcelProperty("套餐名称")
    private String packageName;

    @ExcelProperty("关联菜单")
    private String menuIds;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("菜单树父子联动")
    private Boolean menuCheckStrictly;

    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=正常,1=停用")
    private String status;

}
