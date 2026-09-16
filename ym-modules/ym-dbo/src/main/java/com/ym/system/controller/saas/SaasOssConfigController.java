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
import com.ym.system.saas.domain.bo.SaasOssConfigBo;
import com.ym.system.saas.domain.vo.SaasOssConfigVo;
import com.ym.system.saas.service.ISaasOssConfigService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SaaS OSS 配置运营接口。
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/saas/oss-config")
public class SaasOssConfigController extends BaseController {
    private final ISaasOssConfigService configService;

    @SaCheckPermission("saas:oss-config:list")
    @GetMapping("/list")
    public R<PageResult<SaasOssConfigVo>> list(SaasOssConfigBo bo, PageQuery q) {
        return R.ok(configService.queryPageList(bo, q));
    }

    @SaCheckPermission("saas:tenant:list")
    @GetMapping("/options")
    public R<List<SaasOssConfigVo>> options() {
        return R.ok(configService.queryList());
    }

    @SaCheckPermission("saas:oss-config:query")
    @GetMapping("/{id}")
    public R<SaasOssConfigVo> getInfo(@PathVariable Long id) {
        return R.ok(configService.queryById(id));
    }

    @SaCheckPermission("saas:oss-config:add")
    @Log(title = "SaaS OSS配置", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody SaasOssConfigBo bo) {
        return R.ok(configService.insertByBo(bo));
    }

    @SaCheckPermission("saas:oss-config:edit")
    @Log(title = "SaaS OSS配置", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SaasOssConfigBo bo) {
        configService.updateByBo(bo);
        return R.ok();
    }

    @SaCheckPermission("saas:oss-config:remove")
    @Log(title = "SaaS OSS配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        configService.deleteWithValidByIds(List.of(ids));
        return R.ok();
    }
}
