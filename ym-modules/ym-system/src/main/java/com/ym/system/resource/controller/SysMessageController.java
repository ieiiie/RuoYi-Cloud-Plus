package com.ym.system.resource.controller;

import lombok.RequiredArgsConstructor;
import com.ym.common.core.domain.R;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.web.core.BaseController;
import com.ym.system.resource.domain.vo.SysMessageBoxVo;
import com.ym.system.resource.service.ISysMessageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息记录控制器
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/message")
public class SysMessageController extends BaseController {

    private final ISysMessageService messageService;

    @GetMapping("/box")
    public R<SysMessageBoxVo> getBox() {
        return R.ok(messageService.queryMessageBox(LoginHelper.getUserId()));
    }
}
