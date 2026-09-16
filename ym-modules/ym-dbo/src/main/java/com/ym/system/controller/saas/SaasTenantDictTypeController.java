package com.ym.system.controller.saas;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.domain.R;
import com.ym.common.core.validate.AddGroup;
import com.ym.common.core.validate.EditGroup;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.redis.annotation.RepeatSubmit;
import com.ym.common.web.core.BaseController;
import com.ym.system.saas.domain.bo.SaasTenantDictTypeBo;
import com.ym.system.saas.domain.vo.SaasTenantDictTypeVo;
import com.ym.system.saas.service.ISaasTenantDictTypeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Dbo 租户字典类型定义接口。 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/saas/tenant-dict/type")
public class SaasTenantDictTypeController extends BaseController {
    private final ISaasTenantDictTypeService service;

    @SaCheckPermission("saas:tenant-dict:list")
    @GetMapping("/list")
    public R<PageResult<SaasTenantDictTypeVo>> list(SaasTenantDictTypeBo bo, PageQuery pageQuery) {
        return R.ok(service.queryPage(bo, pageQuery));
    }

    @SaCheckPermission("saas:tenant-dict:query")
    @GetMapping("/{id}")
    public R<SaasTenantDictTypeVo> getInfo(@PathVariable Long id) {
        return R.ok(service.queryById(id));
    }

    @SaCheckPermission("saas:tenant-dict:add")
    @Log(title = "租户字典类型", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody SaasTenantDictTypeBo bo) {
        return R.ok(service.insert(bo));
    }

    @SaCheckPermission("saas:tenant-dict:edit")
    @Log(title = "租户字典类型", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SaasTenantDictTypeBo bo) {
        service.update(bo);
        return R.ok();
    }

    @SaCheckPermission("saas:tenant-dict:remove")
    @Log(title = "租户字典类型", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        service.deleteByIds(List.of(ids));
        return R.ok();
    }
}
