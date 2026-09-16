package com.ym.system.saas.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.ym.system.saas.domain.SaasRegion;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/** SaaS 行政区划视图。 */
@Data
@AutoMapper(target = SaasRegion.class)
public class SaasRegionVo implements Serializable {

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
