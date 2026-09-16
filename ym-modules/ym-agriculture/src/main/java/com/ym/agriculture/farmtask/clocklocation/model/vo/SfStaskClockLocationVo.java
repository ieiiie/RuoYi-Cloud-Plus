package com.ym.agriculture.farmtask.clocklocation.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 打卡地点视图。
 */
@Data
public class SfStaskClockLocationVo {

    private Long clockLocationId;
    private String locationName;
    private BigDecimal centerLng;
    private BigDecimal centerLat;
    private String coordinateType;
    private Integer radiusMeters;
    private String enabled;
    private String mapProvider;
    private Integer mapZoomLevel;
    private String addressText;
    private String remark;
    private LocalDateTime updateTime;
    private Boolean configured;
    private Boolean fenceCheckRequired;
}
