package com.ym.system.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;
import com.ym.common.excel.annotation.ExcelDictFormat;
import com.ym.common.excel.convert.ExcelDictConvert;
import com.ym.system.domain.SysTenant;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 租户视图对象。
 *
 * @author Lion Li
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SysTenant.class)
public class SysTenantVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("主键")
    private Long id;

    @ExcelProperty("租户编号")
    private String tenantId;

    @ExcelProperty("联系人")
    private String contactUserName;

    @ExcelProperty("联系电话")
    private String contactPhone;

    @ExcelProperty("企业名称")
    private String companyName;

    /** 租户品牌 Logo 地址。 */
    private String logoUrl;

    @ExcelProperty("统一社会信用代码")
    private String licenseNumber;

    @ExcelProperty("地址")
    private String address;

    private String provinceCode;

    private String cityCode;

    private String districtCode;

    private String regionName;

    private BigDecimal longitude;

    private BigDecimal latitude;

    @ExcelProperty("域名")
    private String domain;

    @ExcelProperty("企业简介")
    private String intro;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("租户套餐")
    private Long packageId;

    /** OSS 配置 ID。 */
    private Long ossConfigId;

    @ExcelProperty("过期时间")
    private LocalDateTime expireTime;

    @ExcelProperty("用户数量上限")
    private Long accountCount;

    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=正常,1=停用")
    private String status;

    private LocalDateTime createTime;

}
