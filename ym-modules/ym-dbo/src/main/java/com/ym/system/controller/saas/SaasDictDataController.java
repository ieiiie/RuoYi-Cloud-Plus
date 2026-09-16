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
import com.ym.system.saas.domain.bo.SaasDictDataBo;
import com.ym.system.saas.domain.vo.SaasDictDataVo;
import com.ym.system.saas.service.ISaasDictService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SaaS 全局字典数据运营接口。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/saas/dict/data")
public class SaasDictDataController extends BaseController {
    private final ISaasDictService dictService;

    @SaCheckPermission("saas:dict:list")
    @GetMapping("/list")
    public R<PageResult<SaasDictDataVo>> list(SaasDictDataBo bo, PageQuery q) {
        return R.ok(dictService.queryDataPage(bo, q));
    }

    @SaCheckPermission("saas:dict:query")
    @GetMapping("/{id}")
    public R<SaasDictDataVo> getInfo(@PathVariable Long id) {
        return R.ok(dictService.queryData(id));
    }

    @SaCheckPermission("saas:dict:add")
    @Log(title = "SaaS全局字典数据", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody SaasDictDataBo bo) {
        return R.ok(dictService.insertData(bo));
    }

    @SaCheckPermission("saas:dict:edit")
    @Log(title = "SaaS全局字典数据", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SaasDictDataBo bo) {
        dictService.updateData(bo);
        return R.ok();
    }

    @SaCheckPermission("saas:dict:remove")
    @Log(title = "SaaS全局字典数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        dictService.deleteData(List.of(ids));
        return R.ok();
    }
}
