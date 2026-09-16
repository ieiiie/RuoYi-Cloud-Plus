package com.ym.iot.jetlinks.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.ym.common.core.domain.R;
import com.ym.common.web.core.BaseController;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.iot.jetlinks.service.IJetLinksVideoService;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@lombok.RequiredArgsConstructor
@RestController
@ConditionalOnJetLinks
@RequestMapping("/iot/wvp/ptz-hold")
public class JetLinksPtzController extends BaseController {
    private final IJetLinksVideoService service;

    @PostMapping("/{deviceId}/{channelId}")
    @SaCheckPermission("iot:video:control")
    public R<Void> control(
            @PathVariable("deviceId") String device,
            @PathVariable("channelId") String channel,
            @RequestParam("command") String command,
            @RequestParam(value = "speed", defaultValue = "30") int speed,
            @RequestParam(value = "renew", defaultValue = "false") boolean renew) {
        service.holdPtz(device, channel, command, speed, renew);
        return R.ok(null);
    }
}
