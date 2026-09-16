package com.ym.iot.jetlinks.service;

import com.ym.iot.wvp.domain.dto.gb.WvpGbChannelListResponse;
import com.ym.iot.wvp.domain.dto.gb.WvpGbDeviceListResponse;
import com.ym.iot.wvp.domain.dto.play.WvpPlayStreamData;
import com.ym.iot.wvp.domain.vo.WvpSnapshotVo;
import com.ym.iot.wvp.service.IWvpFrontEndPtzService;
import com.ym.iot.wvp.service.IWvpGb28181Service;
import com.ym.iot.wvp.service.IWvpPlayService;
import com.ym.iot.wvp.service.IWvpSnapshotService;

/** JetLinks 业务接口；控制器和 Dubbo 适配只依赖业务契约。 */
public interface IJetLinksVideoService
        extends IWvpGb28181Service, IWvpPlayService, IWvpFrontEndPtzService, IWvpSnapshotService {
    WvpGbDeviceListResponse listGbDevices(
            Integer start, Integer limit, Boolean online, String text);

    WvpGbDeviceListResponse listGbDevicesForTenant(
            Integer start, Integer limit, String tenantId, String text);

    WvpGbChannelListResponse listGbChannels(String serial, Integer start, Integer limit);

    WvpPlayStreamData playStart(String device, String channel);

    void playStop(String device, String channel);

    void holdPtz(String device, String channel, String command, int speed, boolean renew);

    void frontEndPtz(
            String device,
            String channel,
            String command,
            Integer horizon,
            Integer vertical,
            Integer zoom);

    WvpSnapshotVo querySnapshot(String device, String channel);
}
