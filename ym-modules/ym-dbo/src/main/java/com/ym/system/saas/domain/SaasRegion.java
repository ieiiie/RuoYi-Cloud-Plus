package com.ym.system.saas.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * SaaS 行政区划。
 *
 * <p>数据来自 SaaS 数据源的 sys_region，表中不包含通用审计字段。</p>
 */
@Data
@TableName("sys_region")
public class SaasRegion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("region_id")
    private Long regionId;

    /** 父级行政区划 adcode，省级为 0。 */
    private Long parentId;

    private String regionName;
    private String regionLevel;
    private String adcode;
    private String citycode;
    private BigDecimal centerLng;
    private BigDecimal centerLat;
    private Integer sortOrder;
    private String fullName;
    private String path;
}
