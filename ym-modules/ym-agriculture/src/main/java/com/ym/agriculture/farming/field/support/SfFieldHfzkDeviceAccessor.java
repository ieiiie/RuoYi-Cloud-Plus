package com.ym.agriculture.farming.field.support;

import com.ym.iot.api.RemoteIotIntegrationService;
import com.ym.iot.api.domain.vo.RemoteHfzkDeviceVo;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** HFZK/WVP 设备跨服务访问桥接。 */
@Component
public class SfFieldHfzkDeviceAccessor {
    private static final String WVP_CAMERA = "WVP_CAMERA";

    @DubboReference
    private RemoteIotIntegrationService remoteIotIntegrationService;

    public Map<String, RemoteHfzkDeviceVo> selectWvpCameraMap(Collection<String> deviceSns) {
        if (deviceSns == null || deviceSns.isEmpty()) return Collections.emptyMap();
        return index(remoteIotIntegrationService.listHfzkDevices(WVP_CAMERA, deviceSns));
    }

    public Map<String, RemoteHfzkDeviceVo> selectAllWvpCameraMap() {
        return index(remoteIotIntegrationService.listAllHfzkDevices(WVP_CAMERA));
    }

    public Map<String, String> selectOnlineStatusMap(Collection<String> deviceCodes) {
        return remoteIotIntegrationService.getOnlineStatusMap(deviceCodes);
    }

    public List<String> filterCameraSns(Collection<String> deviceSns) {
        Map<String, RemoteHfzkDeviceVo> rows = selectWvpCameraMap(deviceSns);
        return rows.isEmpty() ? List.of() : rows.keySet().stream().sorted().collect(Collectors.toList());
    }

    private static Map<String, RemoteHfzkDeviceVo> index(List<RemoteHfzkDeviceVo> rows) {
        Map<String, RemoteHfzkDeviceVo> result = new LinkedHashMap<>();
        for (RemoteHfzkDeviceVo row : rows == null ? List.<RemoteHfzkDeviceVo>of() : rows) {
            if (row != null && row.getDeviceSn() != null) result.putIfAbsent(row.getDeviceSn(), row);
        }
        return result;
    }
}
