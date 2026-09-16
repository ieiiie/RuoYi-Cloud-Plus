package com.ym.agriculture.farmtask.employee.controller;

import com.ym.common.core.domain.R;
import com.ym.common.redis.annotation.RepeatSubmit;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.web.core.BaseController;
import com.ym.agriculture.farmtask.employee.model.bo.SysInviteCodeBo;
import com.ym.agriculture.farmtask.employee.model.vo.MiniProgramCodeVo;
import com.ym.agriculture.farmtask.employee.model.vo.SysInviteCodeVo;
import com.ym.agriculture.farmtask.employee.service.ISysInviteCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 邀请码管理控制器。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/invite-code")
public class SysInviteCodeController extends BaseController {

    private final ISysInviteCodeService inviteCodeService;

    /**
     * 查询角色邀请码分页列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页条件
     * @return 邀请码分页列表
     */
    @GetMapping("/list")
    public R<PageResult<SysInviteCodeVo>> list(SysInviteCodeBo bo, PageQuery pageQuery) {
        return R.ok(inviteCodeService.queryPage(bo, pageQuery));
    }

    /**
     * 生成6位角色邀请码。
     *
     * @param bo 生成参数
     * @return 邀请码
     */
    @Log(title = "邀请码管理", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping("/generate")
    public R<SysInviteCodeVo> generate(@Validated @RequestBody SysInviteCodeBo bo) {
        return R.ok(inviteCodeService.generateRoleInviteCode(bo));
    }

    /**
     * 删除角色邀请码。
     *
     * @param codeId 邀请码ID
     * @return 删除结果
     */
    @Log(title = "邀请码管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{codeId}")
    public R<Void> delete(@PathVariable Long codeId) {
        return toAjax(inviteCodeService.deleteInviteCode(codeId));
    }

    /**
     * 生成携带邀请码参数的小程序码。
     *
     * @param codeId 邀请码ID
     * @param bo     小程序码参数
     * @return 小程序码图片 Base64
     */
    @PostMapping("/{codeId}/mini-code")
    public R<MiniProgramCodeVo> miniCode(@PathVariable Long codeId, @RequestBody(required = false) SysInviteCodeBo bo) {
        return R.ok(inviteCodeService.generateMiniProgramCode(codeId, bo));
    }
}
