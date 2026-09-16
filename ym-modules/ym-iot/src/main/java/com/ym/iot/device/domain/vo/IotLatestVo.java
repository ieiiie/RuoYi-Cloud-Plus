package com.ym.iot.device.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
 * 设备各指标最新值。
 */
@Data
public class IotLatestVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long deviceId;
    private Map<String, BigDecimal> metrics;
    private Map<String, Date> collectTimes;
    private Map<String, String> units;
    private Map<String, String> names;
    private Map<String, String> texts;
}
