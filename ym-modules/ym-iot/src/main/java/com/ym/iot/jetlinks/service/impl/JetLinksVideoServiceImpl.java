package com.ym.iot.jetlinks.service.impl;

import static com.ym.iot.jetlinks.client.JetLinksRpcClient.*;
import static com.ym.iot.jetlinks.support.JetLinksMapping.*;

import cn.hutool.core.bean.BeanUtil;

import com.ym.common.core.exception.ServiceException;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.iot.jetlinks.client.JetLinksRpcClient;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.iot.jetlinks.service.IJetLinksDeviceService;
import com.ym.iot.jetlinks.service.IJetLinksVideoService;
import com.ym.iot.jetlinks.support.JetLinksAccess;
import com.ym.iot.wvp.domain.dto.gb.WvpGbChannelItem;
import com.ym.iot.wvp.domain.dto.gb.WvpGbChannelListResponse;
import com.ym.iot.wvp.domain.dto.gb.WvpGbDeviceItem;
import com.ym.iot.wvp.domain.dto.gb.WvpGbDeviceListResponse;
import com.ym.iot.wvp.domain.dto.play.WvpPlayStreamData;
import com.ym.iot.wvp.domain.vo.WvpSnapshotVo;
import com.ym.iot.wvp.enums.WvpFrontEndPtzCommand;
import com.ym.iot.wvp.support.WvpFrontEndPtzSpeedSupport;
import com.ym.jetlinks.rpc.*;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Primary
@ConditionalOnJetLinks
@RequiredArgsConstructor
public class JetLinksVideoServiceImpl implements IJetLinksVideoService {
    private final JetLinksRpcClient rpc;
    private final JetLinksAccess access;
    private final IJetLinksDeviceService devices;
    // Only directory membership is cached. Access/assignment checks still run for each command.
    private final Map<String, Long> channelDirectory =
            java.util.Collections.synchronizedMap(
                    new LinkedHashMap<>(128, .75f, true) {
                        @Override
                        protected boolean removeEldestEntry(Map.Entry<String, Long> entry) {
                            return size() > 1024;
                        }
                    });

    @Override
    public WvpGbDeviceListResponse listGbDevices(
            Integer start, Integer limit, Boolean online, String text) {
        Map<String, Object> f = new LinkedHashMap<>();
        if (text != null) f.put("query", text);
        if (online != null) f.put("onLine", online);
        var page = new com.ym.common.mybatis.core.page.PageQuery(limit, start);
        var rows = all(rpc.getVideo()::devices, query(access.ids(), f, null));
        var sliced = slice(rows, page);
        WvpGbDeviceListResponse result = new WvpGbDeviceListResponse();
        result.setTotal(Math.toIntExact(sliced.getTotal()));
        result.setList(beans(sliced.getRows(), null, WvpGbDeviceItem.class));
        return result;
    }

    @Override
    public WvpGbDeviceListResponse listGbDevicesForTenant(
            Integer start, Integer limit, String tenantId, String text) {
        access.ids();
        if (tenantId != null && !tenantId.isBlank() && !tenantId.equals(TenantHelper.getTenantId()))
            throw new ServiceException("禁止通过tenantId覆盖当前登录租户");
        // Legacy tenant-scoped endpoint returns all scoped matches without paging.
        return listGbDevices(1, Integer.MAX_VALUE, null, text);
    }

    @Override
    public WvpGbChannelListResponse listGbChannels(String serial, Integer start, Integer limit) {
        Long id = devices.requireCode(serial);
        access.require(id);
        var q = query(List.of(id.toString()), Map.of(), null);
        List<RecordDto> rows = all(query -> rpc.getVideo().channels(serial, query), q);
        var page = slice(rows, new com.ym.common.mybatis.core.page.PageQuery(limit, start));
        WvpGbChannelListResponse result = new WvpGbChannelListResponse();
        result.setChannelCount(Math.toIntExact(page.getTotal()));
        result.setChannelList(
                page.getRows().stream().map(JetLinksVideoServiceImpl::channelView).toList());
        return result;
    }

    /** 视频目录的行 ID 只用于存储；播放必须使用保留前导零的国标通道编号。 */
    static WvpGbChannelItem channelView(RecordDto row) {
        WvpGbChannelItem view = bean(row, null, WvpGbChannelItem.class);
        String channel = Objects.toString(row.data().get("channelId"), "");
        if (channel.isBlank()) channel = Objects.toString(row.data().get("deviceId"), "");
        if (channel.isBlank()) throw new ServiceException("视频目录缺少国标通道编号");
        view.setId(channel);
        view.setDeviceId(Objects.toString(row.data().get("gbDeviceId"), ""));
        // WVP 返回 status，兼容页面读取 DeviceOnline，不能用摄像头在线代替通道在线。
        Object status = row.data().get("status");
        view.setDeviceOnline(
                status != null
                        && Set.of("1", "true", "on", "online")
                                .contains(status.toString().toLowerCase(Locale.ROOT)));
        return view;
    }

    private Long requireChannel(String device, String channel) {
        Long id = devices.requireCode(device);
        if (channel == null || channel.isBlank()) throw new ServiceException("通道编号不能为空");
        var ownership = access.snapshot(id);
        String cacheKey =
                id + ":" + ownership.getAssignmentVersion() + ":" + device + ":" + channel;
        Long expiresAt = channelDirectory.get(cacheKey);
        if (expiresAt != null && expiresAt > System.nanoTime()) return id;
        List<RecordDto> rows =
                all(
                        q -> rpc.getVideo().channels(device, q),
                        query(List.of(id.toString()), Map.of(), null));
        boolean found =
                rows.stream()
                        .anyMatch(
                                r ->
                                        channel.equals(
                                                        Objects.toString(
                                                                r.data().get("channelId"), null))
                                                || channel.equals(
                                                        Objects.toString(
                                                                r.data().get("deviceId"), null)));
        if (!found) throw new ServiceException("通道不存在或不属于已授权设备");
        channelDirectory.put(
                cacheKey, System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5));
        return id;
    }

    @Override
    public WvpPlayStreamData playStart(String device, String channel) {
        Long id = requireChannel(device, channel);
        return access.write(
                id,
                () ->
                        BeanUtil.toBean(
                                await(
                                        rpc.getVideo()
                                                .start(
                                                        access.context(rpc, id),
                                                        device,
                                                        channel,
                                                        false)),
                                WvpPlayStreamData.class));
    }

    @Override
    public void playStop(String device, String channel) {
        Long id = requireChannel(device, channel);
        access.write(
                id,
                () -> {
                    if (!Boolean.TRUE.equals(
                            await(
                                    rpc.getVideo()
                                            .stop(
                                                    access.context(rpc, id),
                                                    device,
                                                    channel,
                                                    false))))
                        throw new ServiceException("JetLinks停止播放失败");
                    return true;
                });
    }

    /** Same lease/stop behavior as JetLinks native PTZ. Renew never resends a movement command. */
    public void holdPtz(String device, String channel, String command, int speed, boolean renew) {
        if (!Set.of("up", "down", "left", "right", "stop").contains(command)
                || speed < 0
                || speed > 100
                || renew && command.equals("stop")) throw new ServiceException("非法云台控制参数");
        Long id = requireChannel(device, channel);
        access.write(
                id,
                () ->
                        await(
                                rpc.getVideo()
                                        .control(
                                                access.context(rpc, id),
                                                device,
                                                channel,
                                                "ptz",
                                                command,
                                                speed,
                                                renew)));
    }

    @Override
    public void frontEndPtz(
            String device,
            String channel,
            String command,
            Integer horizon,
            Integer vertical,
            Integer zoom) {
        Long id = requireChannel(device, channel);
        WvpFrontEndPtzCommand cmd = WvpFrontEndPtzCommand.fromApiValue(command);
        if (cmd == null) throw new ServiceException("非法云台指令");
        var speeds = WvpFrontEndPtzSpeedSupport.resolve(cmd, horizon, vertical, zoom);
        access.write(
                id,
                () ->
                        await(
                                rpc.getVideo()
                                        .frontEndPtz(
                                                access.context(rpc, id),
                                                device,
                                                channel,
                                                cmd.getApiValue(),
                                                speeds.horizonSpeed(),
                                                speeds.verticalSpeed(),
                                                speeds.zoomSpeed())));
    }

    @Override
    public WvpSnapshotVo querySnapshot(String device, String channel) {
        Long id = requireChannel(device, channel);
        return access.write(
                id,
                () -> {
                    var result =
                            await(
                                    rpc.getVideo()
                                            .snapshot(access.context(rpc, id), device, channel));
                    if (result == null) throw new ServiceException("JetLinks未返回抓图结果");
                    WvpSnapshotVo view = BeanUtil.toBean(result, WvpSnapshotVo.class);
                    view.setDeviceId(device);
                    view.setChannelId(channel);
                    return view;
                });
    }
}
