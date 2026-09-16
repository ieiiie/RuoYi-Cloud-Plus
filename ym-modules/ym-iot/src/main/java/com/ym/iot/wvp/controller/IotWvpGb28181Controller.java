package com.ym.iot.wvp.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;

import com.ym.common.core.domain.R;
import com.ym.common.web.core.BaseController;
import com.ym.iot.wvp.domain.dto.gb.WvpGbChannelListResponse;
import com.ym.iot.wvp.domain.dto.gb.WvpGbDeviceListResponse;
import com.ym.iot.wvp.service.IWvpGb28181Service;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * WVP 国标设备/通道查询
 *
 * <p>播放请使用 {@code /iot/wvp/play/start|stop/...}；云台请使用 {@code /iot/wvp/front-end/ptz/...}。
 *
 * <p>需在配置中设置 {@code ym.iot.wvp.enabled=true} 并填写 {@code base-url}、账号密码。
 *
 * <p>路径已加入全局 {@code security.excludes} 与 {@code xss.excludeUrls}（{@code
 * /iot/wvp/**}），供前端免登录拉取设备/通道列表。 {@code /device/page} 与 {@link #deviceList} 相同参数并增加可选 {@code
 * tenantId}，过滤规则见 {@link IWvpGb28181Service#listGbDevicesForTenant}。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/iot/wvp/gb")
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression(
        "${ym.iot.jetlinks.enabled:false} || ${ym.iot.wvp.enabled:false}")
public class IotWvpGb28181Controller extends BaseController {

    private final IWvpGb28181Service wvpGb28181Service;

    /*
        @GetMapping("/device/list")
        public R<WvpGbDeviceListResponse> deviceList(
            @RequestParam(required = false) Integer start,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String q) {
            return R.ok(wvpGb28181Service.listGbDevices(start, limit, null, q));
        }
    */

    /**
     * 国标设备列表
     *
     * <p>超级管理员不按租户过滤；非超管且匿名且未传 tenantId 时不按 iot_device 过滤；否则与 iot_device 求交后一次返回全部匹配项（见 Service
     * 说明）。
     */
    @SaCheckPermission(
            value = {"iot:video:list", "iot:device:list", "iot:device:query", "sf:field:list"},
            mode = SaMode.OR)
    @GetMapping("/device/list")
    public R<WvpGbDeviceListResponse> devicePage(
            @RequestParam(required = false) Integer start,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tenantId) {
        return R.ok(wvpGb28181Service.listGbDevicesForTenant(start, limit, tenantId, q));
    }

    @SaCheckPermission(
            value = {"iot:video:list", "iot:device:list", "iot:device:query", "sf:field:list"},
            mode = SaMode.OR)
    @GetMapping("/channel/list")
    public R<WvpGbChannelListResponse> channelList(
            @RequestParam String serial,
            @RequestParam(required = false) Integer start,
            @RequestParam(required = false) Integer limit) {
        return R.ok(wvpGb28181Service.listGbChannels(serial, start, limit));
    }
}
