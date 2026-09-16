package com.ym.iot.dubbo;

import com.ym.iot.api.RemoteIotIntegrationService;
import com.ym.iot.api.domain.vo.RemoteHfzkDeviceVo;
import com.ym.iot.jetlinks.service.IJetLinksIntegrationService;

import lombok.RequiredArgsConstructor;

import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** 业务系统设备查询入口，设备目录和实时状态统一由 JetLinks 提供。 */
@Service
@DubboService
@RequiredArgsConstructor
public class RemoteIotIntegrationServiceImpl implements RemoteIotIntegrationService {

    private final IJetLinksIntegrationService jetLinks;

    @Override
    public List<RemoteHfzkDeviceVo> listHfzkDevices(
            String deviceType, Collection<String> deviceSns) {
        return jetLinks.listHfzkDevices(deviceType, deviceSns);
    }

    @Override
    public List<RemoteHfzkDeviceVo> listAllHfzkDevices(String deviceType) {
        return jetLinks.listAllHfzkDevices(deviceType);
    }

    @Override
    public Map<String, String> getOnlineStatusMap(Collection<String> deviceCodes) {
        return jetLinks.getOnlineStatusMap(deviceCodes);
    }
}
