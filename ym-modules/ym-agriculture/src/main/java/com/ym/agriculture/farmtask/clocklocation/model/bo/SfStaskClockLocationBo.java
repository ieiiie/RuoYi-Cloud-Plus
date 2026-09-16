package com.ym.agriculture.farmtask.clocklocation.model.bo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 打卡地点保存参数。
 */
@Data
public class SfStaskClockLocationBo {

    @NotBlank(message = "地点名称不能为空")
    private String locationName;

    @NotNull(message = "中心点经度不能为空")
    private BigDecimal centerLng;

    @NotNull(message = "中心点纬度不能为空")
    private BigDecimal centerLat;

    @NotNull(message = "打卡半径不能为空")
    @Min(value = 1, message = "打卡半径不能小于1米")
    private Integer radiusMeters;

    @NotBlank(message = "围栏状态不能为空")
    @Pattern(regexp = "[01]", message = "围栏状态只能为0或1")
    private String enabled;

    private Integer mapZoomLevel;
    private String addressText;
    private String remark;
}
