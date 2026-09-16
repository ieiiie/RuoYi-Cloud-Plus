package com.ym.iot.device.domain.vo;

import com.ym.jetlinks.rpc.RecordDto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/** 设备管理专用视图，不能用作控制设备选择器的授权依据。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IotDeviceHistoryVo extends IotDeviceVo {
    private String ownershipStatus;
    private boolean canOperate;
    private boolean canViewHistory;
    private List<Period> ownershipPeriods;
    private List<RecordDto> properties;

    public record Period(long from, Long to) {}
}
