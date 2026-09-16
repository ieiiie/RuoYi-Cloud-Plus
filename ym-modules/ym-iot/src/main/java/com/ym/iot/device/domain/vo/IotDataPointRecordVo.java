package com.ym.iot.device.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** 设备单次采集记录视图。 */
@Data
public class IotDataPointRecordVo implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    /** 设备主键。 */
    private Long deviceId;

    /** 本次记录采集时间，格式由接口序列化为 yyyy-MM-dd HH:mm:ss。 */
    private Date collectTime;

    /** 原始毫秒时间，供相邻记录定位使用，避免格式化到秒后丢失交接边界精度。 */
    public Long getCollectTimestamp() {
        return collectTime == null ? null : collectTime.getTime();
    }

    /** 同一采集时间下的测点列表。 */
    private List<PointVo> points = new ArrayList<>();

    /** 单个测点值。 */
    @Data
    public static class PointVo implements Serializable {

        @Serial private static final long serialVersionUID = 1L;

        /** 测点编码，对应 iot_product_property.identifier。 */
        private String metricCode;

        /** 数值型测点值。 */
        private BigDecimal value;

        /** 文本型测点值，用于 JSON、图片地址、枚举文本等。 */
        private String textValue;

        /** 测点单位，例如 ℃、%、μS/cm。 */
        private String unit;

        /** 测点采集时间。 */
        private Date collectTime;
    }
}
