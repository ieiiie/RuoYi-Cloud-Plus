package com.ym.iot.device.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 单设备单指标时序数据。
 */
@Data
public class IotSeriesVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String metricCode;
    private List<SeriesPoint> points;
    private String unit;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeriesPoint implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Date time;
        private BigDecimal value;
    }
}
