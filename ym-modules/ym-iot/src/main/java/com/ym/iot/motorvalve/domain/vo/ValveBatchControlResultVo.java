package com.ym.iot.motorvalve.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 电动阀批量控制单设备下发结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValveBatchControlResultVo {

    /** 设备主键 */
    private Long deviceId;

    /** 是否下发成功 */
    private Boolean success;

    /** 实际下发的指令文本，失败时为空 */
    private String commandText;

    /** 结果说明或失败原因 */
    private String message;
}
