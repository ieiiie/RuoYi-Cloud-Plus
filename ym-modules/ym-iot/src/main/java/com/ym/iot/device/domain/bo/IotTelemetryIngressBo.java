package com.ym.iot.device.domain.bo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ym.iot.device.domain.IotDevice;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

import lombok.Data;

import java.util.Date;

/**
 * 遥测接入参数。
 */
@Data
public class IotTelemetryIngressBo {

    @NotBlank(message = "adapterKey 不能为空")
    private String adapterKey;
    private Long deviceId;
    private String deviceCode;
    @NotBlank(message = "payload 不能为空")
    private String payload;
    private Date defaultCollectTime;
    @JsonIgnore
    private IotDevice internalResolvedDevice;
    @JsonIgnore
    private Boolean skipDeviceLastReportUpdate;

    @AssertTrue(message = "deviceId 与 deviceCode 必须且只能填一个")
    public boolean isDeviceTargetValid() {
        boolean hasId = deviceId != null;
        boolean hasCode = deviceCode != null && !deviceCode.isBlank();
        return hasId != hasCode;
    }
}
