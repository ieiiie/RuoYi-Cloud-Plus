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
import com.ym.system.saas.domain.bo.SaasDictTypeBo;
import com.ym.system.saas.domain.vo.SaasDictTypeVo;
import com.ym.system.saas.service.ISaasDictService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SaaS 全局字典类型运营接口。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/saas/dict/type")
public class SaasDictTypeController extends BaseController {
    private final ISaasDictService dictService;

    @SaCheckPermission("saas:dict:list")
    @GetMapping("/list")
    public R<PageResult<SaasDictTypeVo>> list(SaasDictTypeBo bo, PageQuery q) {
        return R.ok(dictService.queryTypePage(bo, q));
    }

    @SaCheckPermission("saas:dict:query")
    @GetMapping("/{id}")
    public R<SaasDictTypeVo> getInfo(@PathVariable Long id) {
        return R.ok(dictService.queryType(id));
    }

    @SaCheckPermission("saas:dict:add")
    @Log(title = "SaaS全局字典", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody SaasDictTypeBo bo) {
        return R.ok(dictService.insertType(bo));
    }

    @SaCheckPermission("saas:dict:edit")
    @Log(title = "SaaS全局字典", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SaasDictTypeBo bo) {
        dictService.updateType(bo);
        return R.ok();
    }

    @SaCheckPermission("saas:dict:remove")
    @Log(title = "SaaS全局字典", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        dictService.deleteTypes(List.of(ids));
        return R.ok();
    }
}
