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
import com.ym.system.saas.domain.bo.SaasRoleTemplateBo;
import com.ym.system.saas.domain.vo.SaasRoleTemplateVo;
import com.ym.system.saas.service.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * SaaS 角色模板运营接口。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/saas/role-template")
public class SaasRoleTemplateController extends BaseController {
    private final ISaasRoleTemplateService templateService;

    @SaCheckPermission("saas:role-template:list")
    @GetMapping("/list")
    public R<PageResult<SaasRoleTemplateVo>> list(SaasRoleTemplateBo bo, PageQuery q) {
        return R.ok(templateService.queryPageList(bo, q));
    }

    @SaCheckPermission("saas:role-template:query")
    @GetMapping("/{id}")
    public R<SaasRoleTemplateVo> getInfo(@PathVariable Long id) {
        return R.ok(templateService.queryById(id));
    }

    @SaCheckPermission("saas:role-template:add")
    @Log(title = "SaaS角色模板", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody SaasRoleTemplateBo bo) {
        return R.ok(templateService.insertByBo(bo));
    }

    @SaCheckPermission("saas:role-template:edit")
    @Log(title = "SaaS角色模板", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SaasRoleTemplateBo bo) {
        templateService.updateByBo(bo);
        return R.ok();
    }

    @SaCheckPermission("saas:role-template:remove")
    @Log(title = "SaaS角色模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        templateService.deleteWithValidByIds(List.of(ids));
        return R.ok();
    }

    @SaCheckPermission("saas:role-template:sync")
    @Log(title = "同步SaaS角色模板", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/{id}/sync")
    public R<Void> sync(@PathVariable Long id, @RequestBody List<String> tenantIds) {
        templateService.sync(id, tenantIds);
        return R.ok();
    }

}
