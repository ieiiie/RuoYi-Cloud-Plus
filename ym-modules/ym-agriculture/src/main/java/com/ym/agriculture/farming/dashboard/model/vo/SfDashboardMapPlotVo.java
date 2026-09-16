package com.ym.agriculture.farming.dashboard.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 农田地图单个地块：边界、展示状态、主批次与传感器锚点。
 *
 * @author ym-cloud
 */
@Data
public class SfDashboardMapPlotVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 地块主键
     */
    private Long fieldId;

    /**
     * 地块名称
     */
    private String fieldName;

    /**
     * 边界 GeoJSON
     */
    private String boundaryGeojson;

    /**
     * 中心经度
     */
    private BigDecimal centerLng;

    /**
     * 中心纬度
     */
    private BigDecimal centerLat;

    /**
     * 地图着色用状态（与 {@link com.ym.agriculture.farming.field.support.FieldMapDisplayStatus} 一致，如 PLANTING、GROWING、DISABLED、IDLE）
     */
    private String mapDisplayStatus;

    /**
     * 当前进行中批次主键；无则 null（点击地图可跳转批次详情时用）
     */
    private Long primaryBatchId;

    /**
     * 该地块下传感器标记
     */
    private List<SfDashboardSensorMarkerVo> sensors = new ArrayList<>();
}
