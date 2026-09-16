package com.ym.iot.api;

import com.ym.iot.api.domain.vo.RemoteHfzkDeviceVo;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** HFZK 与 JetLinks 设备状态只读集成契约。 */
public interface RemoteIotIntegrationService {
    List<RemoteHfzkDeviceVo> listHfzkDevices(String deviceType, Collection<String> deviceSns);
    List<RemoteHfzkDeviceVo> listAllHfzkDevices(String deviceType);
    Map<String, String> getOnlineStatusMap(Collection<String> deviceCodes);
}
