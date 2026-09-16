package com.ym.agriculture.farming.bigscreen.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 大屏地图地块边界。
 */
@Data
public class SfBigscreenMapFieldVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 地块主键 */
    private Long fieldId;

    /** 地块名称 */
    private String fieldName;

    /** 边界 GeoJSON */
    private String boundaryGeojson;

    /** 中心经度 */
    private BigDecimal centerLng;

    /** 中心纬度 */
    private BigDecimal centerLat;

    /** 地图展示状态 */
    private String mapDisplayStatus;

    /** 活跃批次 ID */
    private Long activeBatchId;
}
