package com.ym.iot.jetlinks.service;

import com.ym.iot.api.domain.vo.RemoteHfzkDeviceVo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** JetLinks 业务接口；控制器和 Dubbo 适配只依赖业务契约。 */
public interface IJetLinksIntegrationService {
    List<RemoteHfzkDeviceVo> listHfzkDevices(String type, Collection<String> sns);

    List<RemoteHfzkDeviceVo> listAllHfzkDevices(String type);

    Map<String, String> getOnlineStatusMap(Collection<String> codes);
}
