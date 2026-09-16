package com.ym.system.controller.system;

import lombok.RequiredArgsConstructor;
import com.ym.common.core.domain.R;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.web.core.BaseController;
import com.ym.system.domain.vo.AuthorizedAppVo;
import com.ym.system.service.ISysAppRuntimeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 当前登录用户的业务应用入口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/app")
public class SysAppController extends BaseController {
    private final ISysAppRuntimeService appRuntimeService;

    /** 公开登录页配置；业务授权仍必须调用 authorized。 */
    @cn.dev33.satoken.annotation.SaIgnore
    @GetMapping("/public/{appKey}")
    public R<com.ym.system.domain.vo.PublicAppLoginVo> publicLogin(
        @org.springframework.web.bind.annotation.PathVariable String appKey) {
        return R.ok(appRuntimeService.selectPublicLogin(appKey));
    }

    /** 查询微应用和跨微前端组合应用。 */
    @GetMapping("/authorized")
    public R<List<AuthorizedAppVo>> authorized() {
        return R.ok(appRuntimeService.selectAuthorizedApps(LoginHelper.getUserId()));
    }
}
