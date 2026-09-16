package com.ym.iot.wvp.service;

import com.ym.iot.wvp.domain.dto.gb.WvpGbChannelListResponse;
import com.ym.iot.wvp.domain.dto.gb.WvpGbDeviceListResponse;

/**
 * WVP 国标设备与通道查询，代理 {@code /api/device/query/*}。
 * <p>
 * 播放与云台请使用 {@link IWvpPlayService}、{@link IWvpFrontEndPtzService}。
 */
public interface IWvpGb28181Service {

    /**
     * {@code GET /api/device/query/devices}
     */
    WvpGbDeviceListResponse listGbDevices(Integer start, Integer limit, Boolean online, String q);

    /**
     * 国标设备列表（参数与 {@link #listGbDevices} 一致并增加可选 {@code tenantId}）：超级管理员或匿名且未传 {@code tenantId} 时行为与 {@link #listGbDevices} 相同（{@code start}/{@code limit} 透传 WVP）；
     * 否则与 {@code iot_device}（{@code device_code} 匹配 WVP {@code deviceId}）按租户求交后，一次性返回全部匹配项（{@code total} 为过滤后条数，不再按 {@code start}/{@code limit} 切页）。
     * <p>
     * <b>注意：</b>WVP 侧设备量很大时会多次请求 WVP 并在内存中过滤，单次接口耗时与内存占用可能较高。
     * </p>
     *
     * @param start    WVP 页码，见 {@link #listGbDevices}
     * @param limit    WVP 每页条数，见 {@link #listGbDevices}；仅在未做 iot 过滤时生效
     * @param tenantId 可选租户 ID；非超管时有值优先于当前登录租户
     * @param q        可选关键字，透传 WVP {@code query}
     * @return WVP 列表结构（{@code total} + {@code list}）
     */
    WvpGbDeviceListResponse listGbDevicesForTenant(Integer start, Integer limit, String tenantId, String q);

    /**
     * {@code GET /api/v1/device/channellist}
     */
    WvpGbChannelListResponse listGbChannels(String serial, Integer start, Integer limit);
}
