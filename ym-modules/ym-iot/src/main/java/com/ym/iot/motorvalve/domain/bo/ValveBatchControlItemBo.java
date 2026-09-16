package com.ym.iot.motorvalve.domain.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 电动阀批量控制单设备参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ValveBatchControlItemBo extends ValveControlBo {

    /** 设备主键 */
    private Long deviceId;
}
