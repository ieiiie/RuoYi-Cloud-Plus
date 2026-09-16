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
import com.ym.system.saas.domain.bo.SaasTenantBo;
import com.ym.system.saas.domain.vo.SaasTenantVo;
import com.ym.system.saas.service.ISaasTenantService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SaaS 租户运营接口。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/saas/tenant")
public class SaasTenantController extends BaseController {
    private final ISaasTenantService tenantService;

    @SaCheckPermission("saas:tenant:list")
    @GetMapping("/list")
    public R<PageResult<SaasTenantVo>> list(SaasTenantBo bo, PageQuery q) {
        return R.ok(tenantService.queryPageList(bo, q));
    }

    @SaCheckPermission("saas:tenant:query")
    @GetMapping("/{id}")
    public R<SaasTenantVo> getInfo(@PathVariable Long id) {
        return R.ok(tenantService.queryById(id));
    }

    @SaCheckPermission("saas:tenant:add")
    @Log(title = "SaaS租户", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody SaasTenantBo bo) {
        return R.ok(tenantService.insertByBo(bo));
    }

    @SaCheckPermission("saas:tenant:edit")
    @Log(title = "SaaS租户", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SaasTenantBo bo) {
        tenantService.updateByBo(bo);
        return R.ok();
    }

    @SaCheckPermission("saas:tenant:remove")
    @Log(title = "SaaS租户", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        tenantService.deleteWithValidByIds(List.of(ids));
        return R.ok();
    }
}
