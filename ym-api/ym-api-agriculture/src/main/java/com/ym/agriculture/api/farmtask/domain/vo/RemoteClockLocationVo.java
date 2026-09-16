package com.ym.agriculture.api.farmtask.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 农事任务打卡地点跨服务视图。
 */
@Data
public class RemoteClockLocationVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
