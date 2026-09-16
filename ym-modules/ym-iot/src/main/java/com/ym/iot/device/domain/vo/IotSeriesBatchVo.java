package com.ym.iot.device.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 多指标时序（同一设备、同一时间窗）
 *
 * @author ym-cloud
 */
@Data
public class IotSeriesBatchVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 各指标一条曲线，顺序与请求参数一致；无数据时仍返回对应项且 points 可为空
     */
    private List<IotSeriesVo> series;
}
