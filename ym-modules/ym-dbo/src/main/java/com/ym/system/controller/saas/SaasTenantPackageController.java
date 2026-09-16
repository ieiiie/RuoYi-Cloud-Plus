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
import com.ym.system.saas.domain.bo.SaasTenantPackageBo;
import com.ym.system.saas.domain.vo.SaasTenantPackageVo;
import com.ym.system.saas.service.ISaasTenantPackageService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SaaS 套餐运营接口。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/saas/tenant-package")
public class SaasTenantPackageController extends BaseController {
    private final ISaasTenantPackageService packageService;

    @SaCheckPermission("saas:tenant-package:list")
    @GetMapping("/list")
    public R<PageResult<SaasTenantPackageVo>> list(SaasTenantPackageBo bo, PageQuery q) {
        return R.ok(packageService.queryPageList(bo, q));
    }

    @SaCheckPermission("saas:tenant-package:query")
    @GetMapping("/{id}")
    public R<SaasTenantPackageVo> getInfo(@PathVariable Long id) {
        return R.ok(packageService.queryById(id));
    }

    @SaCheckPermission("saas:tenant-package:add")
    @Log(title = "SaaS套餐", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody SaasTenantPackageBo bo) {
        return R.ok(packageService.insertByBo(bo));
    }

    @SaCheckPermission("saas:tenant-package:edit")
    @Log(title = "SaaS套餐", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SaasTenantPackageBo bo) {
        packageService.updateByBo(bo);
        return R.ok();
    }

    @SaCheckPermission("saas:tenant-package:edit")
    @Log(title = "SaaS套餐状态", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody SaasTenantPackageBo bo) {
        packageService.updateStatus(bo);
        return R.ok();
    }

    @SaCheckPermission("saas:tenant-package:remove")
    @Log(title = "SaaS套餐", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        packageService.deleteWithValidByIds(List.of(ids));
        return R.ok();
    }
}
