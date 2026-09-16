package com.ym.agriculture.farmtask.sop.controller;

import com.ym.common.core.domain.R;
import com.ym.agriculture.farmtask.sop.model.vo.SfStaskSopResolveVo;
import com.ym.agriculture.farmtask.sop.service.ISfStaskSopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 小程序任务 SOP 解析接口。 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/miniapp/smart-farming/stask/sops")
public class SfStaskSopMiniappController {

    private final ISfStaskSopService sopService;

    @GetMapping("/resolve")
    public R<SfStaskSopResolveVo> resolve(@RequestParam Long orderId) {
        return R.ok(sopService.resolveForMiniapp(orderId));
    }
}
