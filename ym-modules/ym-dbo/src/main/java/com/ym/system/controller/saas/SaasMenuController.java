package com.ym.system.controller.saas;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import com.ym.common.core.domain.R;
import com.ym.common.core.validate.*;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.redis.annotation.RepeatSubmit;
import com.ym.common.web.core.BaseController;
import com.ym.system.saas.domain.bo.SaasMenuBo;
import com.ym.system.saas.domain.vo.SaasMenuVo;
import com.ym.system.saas.service.ISaasMenuService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SaaS 租户菜单运营接口。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/saas/menu")
public class SaasMenuController extends BaseController {
    private final ISaasMenuService menuService;

    @SaCheckPermission("saas:menu:list")
    @GetMapping("/list")
    public R<List<SaasMenuVo>> list(SaasMenuBo bo) {
        return R.ok(menuService.queryList(bo));
    }

    @SaCheckPermission("saas:menu:query")
    @GetMapping("/{id}")
    public R<SaasMenuVo> getInfo(@PathVariable Long id) {
        return R.ok(menuService.queryById(id));
    }

    @SaCheckPermission("saas:menu:add")
    @Log(title = "SaaS租户菜单", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody SaasMenuBo bo) {
        return R.ok(menuService.insertByBo(bo));
    }

    @SaCheckPermission("saas:menu:edit")
    @Log(title = "SaaS租户菜单", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SaasMenuBo bo) {
        menuService.updateByBo(bo);
        return R.ok();
    }

    @SaCheckPermission("saas:menu:remove")
    @Log(title = "SaaS租户菜单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        menuService.deleteWithValidByIds(List.of(ids));
        return R.ok();
    }
}
