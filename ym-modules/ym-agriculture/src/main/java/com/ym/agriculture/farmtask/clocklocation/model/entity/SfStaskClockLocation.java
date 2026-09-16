package com.ym.agriculture.farmtask.clocklocation.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 租户农事任务打卡地点。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_stask_clock_location")
public class SfStaskClockLocation extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "clock_location_id", type = IdType.ASSIGN_ID)
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
}
