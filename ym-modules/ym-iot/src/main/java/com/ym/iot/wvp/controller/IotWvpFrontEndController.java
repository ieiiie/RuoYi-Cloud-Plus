package com.ym.iot.wvp.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.ym.common.core.domain.R;
import com.ym.common.web.core.BaseController;
import com.ym.iot.wvp.service.IWvpFrontEndPtzService;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * WVP 前端云台代理。
 *
 * <p>路径已加入全局 {@code security.excludes} 与 {@code xss.excludeUrls}（{@code /iot/wvp/**}），供前端播放器免登录调用。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/iot/wvp/front-end/ptz")
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression(
        "${ym.iot.jetlinks.enabled:false} || ${ym.iot.wvp.enabled:false}")
public class IotWvpFrontEndController extends BaseController {

    private final IWvpFrontEndPtzService wvpFrontEndPtzService;

    /**
     * 云台：command 为 up、down、left、right、upleft、upright、downleft、downright、zoomin、zoomout、stop；速度参数可选。
     */
    @SaCheckPermission("iot:video:control")
    @GetMapping("/{deviceId}/{channelId}")
    public R<Void> ptz(
            @PathVariable String deviceId,
            @PathVariable String channelId,
            @RequestParam String command,
            @RequestParam(required = false) Integer horizonSpeed,
            @RequestParam(required = false) Integer verticalSpeed,
            @RequestParam(required = false) Integer zoomSpeed) {
        wvpFrontEndPtzService.frontEndPtz(
                deviceId, channelId, command, horizonSpeed, verticalSpeed, zoomSpeed);
        return R.ok(null);
    }
}
