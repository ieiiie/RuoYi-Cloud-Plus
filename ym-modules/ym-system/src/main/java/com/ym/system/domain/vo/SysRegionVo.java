package com.ym.system.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.ym.system.domain.SysRegion;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/** 系统行政区划视图。 */
@Data
@AutoMapper(target = SysRegion.class)
public class SysRegionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long regionId;
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
