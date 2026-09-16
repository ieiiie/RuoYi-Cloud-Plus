package com.ym.system.controller.saas;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import com.ym.common.core.domain.*;
import com.ym.common.core.validate.*;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.redis.annotation.RepeatSubmit;
import com.ym.common.web.core.BaseController;
import com.ym.system.saas.domain.bo.SaasAppBo;
import com.ym.system.saas.domain.bo.SaasAppMenusBo;
import com.ym.system.saas.domain.vo.SaasAppVo;
import com.ym.system.saas.service.ISaasAppService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SaaS 应用运营接口。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/saas/app")
public class SaasAppController extends BaseController {
    private final ISaasAppService appService;

    @SaCheckPermission("saas:app:list")
    @GetMapping("/list")
    public R<PageResult<SaasAppVo>> list(SaasAppBo bo, PageQuery query) {
        return R.ok(appService.queryPageList(bo, query));
    }

    @SaCheckPermission("saas:app:query")
    @GetMapping("/{id}")
    public R<SaasAppVo> getInfo(@PathVariable Long id) {
        return R.ok(appService.queryById(id));
    }

    @SaCheckPermission("saas:app:add")
    @Log(title = "SaaS应用", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody SaasAppBo bo) {
        return R.ok(appService.insertByBo(bo));
    }

    @SaCheckPermission("saas:app:edit")
    @Log(title = "SaaS应用", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SaasAppBo bo) {
        appService.updateByBo(bo);
        return R.ok();
    }

    @SaCheckPermission("saas:app:remove")
    @Log(title = "SaaS应用", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        appService.deleteWithValidByIds(List.of(ids));
        return R.ok();
    }

    /** 保存组合应用菜单选择。 */
    @SaCheckPermission("saas:app:edit")
    @Log(title = "SaaS组合应用菜单", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/{appId}/menus")
    public R<Void> updateMenus(@PathVariable Long appId, @Validated @RequestBody SaasAppMenusBo bo) {
        appService.updateMenus(appId, bo.getMenuIds());
        return R.ok();
    }
}
