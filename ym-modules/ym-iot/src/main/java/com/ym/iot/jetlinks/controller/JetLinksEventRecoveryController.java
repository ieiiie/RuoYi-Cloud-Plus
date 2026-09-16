package com.ym.iot.jetlinks.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.ym.common.core.domain.R;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.web.core.BaseController;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.iot.jetlinks.service.IJetLinksBusinessEvents;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Validated
@RestController
@ConditionalOnJetLinks
@RequiredArgsConstructor
@RequestMapping("/iot/jetlinks/events")
public class JetLinksEventRecoveryController extends BaseController {
    private final IJetLinksBusinessEvents events;

    private static String administrator() {
        if (!LoginHelper.isSuperAdmin()) throw new ServiceException("仅平台管理员可重放核心事件");
        return LoginHelper.getUserIdStr();
    }

    @GetMapping("/failures")
    @SaCheckPermission("iot:event:query")
    public R<List<Map<String, Object>>> failures(@RequestParam(defaultValue = "100") int limit) {
        administrator();
        return R.ok(events.failures(limit));
    }

    @PostMapping("/replay")
    @SaCheckPermission("iot:event:replay")
    public R<Void> replay(@RequestParam String eventId) {
        events.replay(eventId, administrator());
        return R.ok();
    }
}
