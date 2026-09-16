package com.ym.iot.wvp.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;

import com.ym.common.core.domain.R;
import com.ym.common.web.core.BaseController;
import com.ym.iot.wvp.domain.dto.play.WvpPlayStreamData;
import com.ym.iot.wvp.service.IWvpPlayService;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * WVP Web 播放代理。
 *
 * <p>路径已加入全局 {@code security.excludes} 与 {@code xss.excludeUrls}（{@code /iot/wvp/**}），供前端播放器免登录起停流。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/iot/wvp/play")
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression(
        "${ym.iot.jetlinks.enabled:false} || ${ym.iot.wvp.enabled:false}")
public class IotWvpPlayController extends BaseController {

    private final IWvpPlayService wvpPlayService;

    /** 开始播放，路径形态与 WVP 一致：{@code .../start/{deviceId}/{channelId}}。 */
    @SaCheckPermission(
            value = {"iot:video:play", "iot:video:list"},
            mode = SaMode.OR)
    @GetMapping("/start/{deviceId}/{channelId}")
    public R<WvpPlayStreamData> start(
            @PathVariable String deviceId, @PathVariable String channelId) {
        return R.ok(wvpPlayService.playStart(deviceId, channelId));
    }

    /** 停止播放。 */
    @SaCheckPermission(
            value = {"iot:video:play", "iot:video:list"},
            mode = SaMode.OR)
    @GetMapping("/stop/{deviceId}/{channelId}")
    public R<Void> stop(@PathVariable String deviceId, @PathVariable String channelId) {
        wvpPlayService.playStop(deviceId, channelId);
        return R.ok(null);
    }
}
