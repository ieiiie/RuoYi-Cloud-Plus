package com.ym.agriculture.farming.field.support;

import com.ym.iot.api.RemoteIotDeviceService;
import com.ym.iot.api.domain.bo.RemoteDeviceQueryBo;
import com.ym.iot.api.domain.vo.RemoteDeviceSummaryVo;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Collection;

/** 地块域访问 IoT 设备的 Dubbo 桥接。 */
@Component
public class SfFieldIotDeviceAccessor {

    @DubboReference
    private RemoteIotDeviceService remoteIotDeviceService;

    public List<RemoteDeviceSummaryVo> queryList(RemoteDeviceQueryBo query) {
        return remoteIotDeviceService.listDevices(query);
    }

    public List<RemoteDeviceSummaryVo> listByCodes(Collection<String> deviceCodes) {
        if (deviceCodes == null || deviceCodes.isEmpty()) {
            return List.of();
        }
        return remoteIotDeviceService.listDevicesByCodes(deviceCodes);
    }
}
