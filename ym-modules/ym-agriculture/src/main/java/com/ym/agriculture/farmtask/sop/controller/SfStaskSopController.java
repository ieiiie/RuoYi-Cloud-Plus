package com.ym.agriculture.farmtask.sop.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ym.common.core.domain.R;
import com.ym.common.core.validate.AddGroup;
import com.ym.common.core.validate.EditGroup;
import com.ym.common.redis.annotation.RepeatSubmit;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.web.core.BaseController;
import com.ym.agriculture.farmtask.sop.model.bo.SfStaskSopBo;
import com.ym.agriculture.farmtask.sop.model.bo.SfStaskSopQueryBo;
import com.ym.agriculture.farmtask.sop.model.vo.SfStaskSopVo;
import com.ym.agriculture.farmtask.sop.service.ISfStaskSopService;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理端农事 SOP 接口。 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/smart-farming/stask/sops")
public class SfStaskSopController extends BaseController {

    private final ISfStaskSopService sopService;

    @SaCheckPermission("smartfarming:staskSop:list")
    @GetMapping("/page")
    public R<PageResult<SfStaskSopVo>> page(SfStaskSopQueryBo bo, PageQuery pageQuery) {
        return R.ok(sopService.page(bo, pageQuery));
    }

    @SaCheckPermission("smartfarming:staskSop:query")
    @GetMapping("/{sopId}")
    public R<SfStaskSopVo> getInfo(@PathVariable Long sopId) {
        return R.ok(sopService.getById(sopId));
    }

    @Log(title = "农事SOP", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @SaCheckPermission("smartfarming:staskSop:add")
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody SfStaskSopBo bo) {
        return R.ok(sopService.add(bo));
    }

    @Log(title = "农事SOP", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @SaCheckPermission("smartfarming:staskSop:edit")
    @PutMapping("/{sopId}")
    public R<Void> edit(@PathVariable Long sopId,
                        @Validated({EditGroup.class, Default.class}) @RequestBody SfStaskSopBo bo) {
        sopService.update(sopId, bo);
        return R.ok();
    }

    @Log(title = "农事SOP", businessType = BusinessType.DELETE)
    @RepeatSubmit()
    @SaCheckPermission("smartfarming:staskSop:remove")
    @DeleteMapping("/{sopId}")
    public R<Void> remove(@PathVariable Long sopId) {
        sopService.remove(sopId);
        return R.ok();
    }

    @Log(title = "农事SOP复制草稿", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @SaCheckPermission("smartfarming:staskSop:copy")
    @PostMapping("/{sopId}/copy")
    public R<SfStaskSopVo> copy(@PathVariable Long sopId) {
        return R.ok(sopService.copyDraft(sopId));
    }
}
