package com.ym.iot.jetlinks.service.impl;

import com.ym.iot.api.domain.vo.*;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.iot.jetlinks.service.IJetLinksDeviceService;
import com.ym.iot.jetlinks.service.IJetLinksHfzkService;
import com.ym.iot.jetlinks.service.IJetLinksIntegrationService;
import com.ym.iot.jetlinks.support.JetLinksMapping;
import com.ym.jetlinks.rpc.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
@ConditionalOnJetLinks
@RequiredArgsConstructor
public class JetLinksIntegrationServiceImpl implements IJetLinksIntegrationService {
    private final IJetLinksHfzkService hfzk;
    private final IJetLinksDeviceService devices;

    public List<RemoteHfzkDeviceVo> listHfzkDevices(String type, Collection<String> sns) {
        if (sns == null || sns.isEmpty()) return List.of();
        return listAllHfzkDevices(type).stream()
                .filter(d -> sns.contains(d.getDeviceSn()))
                .toList();
    }

    public List<RemoteHfzkDeviceVo> listAllHfzkDevices(String type) {
        Map<String, Object> f = new LinkedHashMap<>();
        if (type != null && !type.isBlank()) f.put("deviceType", type);
        return JetLinksMapping.beans(hfzk.records(f), "id", RemoteHfzkDeviceVo.class);
    }

    public Map<String, String> getOnlineStatusMap(Collection<String> codes) {
        Map<String, String> result = new LinkedHashMap<>();
        for (RecordDto row : devices.byCodes(codes)) {
            Object code = row.data().get("deviceCode"), online = row.data().get("onlineStatus");
            if (code != null) result.put(code.toString(), online == null ? "" : online.toString());
        }
        return result;
    }
}
