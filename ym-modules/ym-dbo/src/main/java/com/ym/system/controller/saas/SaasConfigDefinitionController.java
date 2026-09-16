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
import com.ym.system.saas.domain.bo.SaasConfigDefinitionBo;
import com.ym.system.saas.domain.vo.SaasConfigDefinitionVo;
import com.ym.system.saas.service.ISaasConfigDefinitionService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SaaS 参数定义运营接口。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/saas/config-definition")
public class SaasConfigDefinitionController extends BaseController {
    private final ISaasConfigDefinitionService definitionService;

    @SaCheckPermission("saas:config-definition:list")
    @GetMapping("/list")
    public R<PageResult<SaasConfigDefinitionVo>> list(SaasConfigDefinitionBo bo, PageQuery q) {
        return R.ok(definitionService.queryPageList(bo, q));
    }

    @SaCheckPermission("saas:config-definition:query")
    @GetMapping("/{id}")
    public R<SaasConfigDefinitionVo> getInfo(@PathVariable Long id) {
        return R.ok(definitionService.queryById(id));
    }

    @SaCheckPermission("saas:config-definition:add")
    @Log(title = "SaaS参数定义", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody SaasConfigDefinitionBo bo) {
        return R.ok(definitionService.insertByBo(bo));
    }

    @SaCheckPermission("saas:config-definition:edit")
    @Log(title = "SaaS参数定义", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SaasConfigDefinitionBo bo) {
        definitionService.updateByBo(bo);
        return R.ok();
    }

    @SaCheckPermission("saas:config-definition:remove")
    @Log(title = "SaaS参数定义", businessType = BusinessType.UPDATE)
    @DeleteMapping("/{ids}")
    public R<Void> disable(@PathVariable Long[] ids) {
        definitionService.disableByIds(List.of(ids));
        return R.ok();
    }
}
