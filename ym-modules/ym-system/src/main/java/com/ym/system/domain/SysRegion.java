package com.ym.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 系统行政区划。
 *
 * <p>该表不包含通用审计字段，因此不继承 BaseEntity。</p>
 */
@Data
@TableName("sys_region")
public class SysRegion implements Serializable {

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
