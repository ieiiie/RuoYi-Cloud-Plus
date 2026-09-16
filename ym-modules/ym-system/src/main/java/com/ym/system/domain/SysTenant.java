package com.ym.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ym.common.mybatis.core.domain.BaseEntity;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 租户对象 sys_tenant。
 *
 * <p>该表属于平台全局数据，不参与业务表的 tenant_id 行级过滤。</p>
 *
 * @author Lion Li
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant")
public class SysTenant extends BaseEntity {

    /** 主键。 */
    @TableId(value = "id")
    private Long id;

    /** 租户编号。 */
    private String tenantId;

    /** 联系人。 */
    private String contactUserName;

    /** 联系电话。 */
    private String contactPhone;

    /** 企业名称。 */
    private String companyName;

    /** 租户品牌 Logo 地址，由运营平台维护。 */
    private String logoUrl;

    /** 统一社会信用代码。 */
    private String licenseNumber;

    /** 地址。 */
    private String address;

    private String provinceCode;

    private String cityCode;

    private String districtCode;

    private String regionName;

    private BigDecimal longitude;

    private BigDecimal latitude;

    /** 域名。 */
    private String domain;

    /** 企业简介。 */
    private String intro;

    /** 备注。 */
    private String remark;

    /** 租户套餐编号。 */
    private Long packageId;

    /** OSS 配置 ID。 */
    private Long ossConfigId;

    /** 过期时间，空表示不限制。 */
    private LocalDateTime expireTime;

    /** 用户数量上限，-1 表示不限制。 */
    private Long accountCount;

    /** 状态（0正常 1停用）。 */
    private String status;

    /** 删除标志（0存在 1删除）。 */
    @TableLogic
    private String delFlag;

}
