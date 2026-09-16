package com.ym.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.hutool.crypto.digest.BCrypt;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.constant.TenantConstants;
import com.ym.common.core.domain.R;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.validate.EditGroup;
import com.ym.common.encrypt.annotation.ApiEncrypt;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.redis.annotation.RepeatSubmit;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.system.domain.bo.SysGlobalUserBo;
import com.ym.system.domain.vo.SysGlobalUserVo;
import com.ym.system.service.ISysGlobalUserService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 平台全局账号管理。
 *
 * <p>租户管理员只通过用户管理维护本租户成员关系；只有默认管理租户的超级管理员
 * 可以维护账号本身的资料、状态和密码。</p>
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/global/user")
@ConditionalOnProperty(value = "tenant.enable", havingValue = "true")
public class SysGlobalUserController {

    private final ISysGlobalUserService globalUserService;

    @SaCheckRole(SystemConstants.SUPER_ADMIN_ROLE_KEY)
    @SaCheckPermission("system:user:list")
    @GetMapping("/list")
    public R<List<SysGlobalUserVo>> list(SysGlobalUserBo bo) {
        checkPlatformTenant();
        return R.ok(globalUserService.queryList(bo));
    }

    @SaCheckRole(SystemConstants.SUPER_ADMIN_ROLE_KEY)
    @SaCheckPermission("system:user:query")
    @GetMapping("/{globalUserId}")
    public R<SysGlobalUserVo> getInfo(@NotNull(message = "全局账号ID不能为空") @PathVariable Long globalUserId) {
        checkPlatformTenant();
        return R.ok(globalUserService.queryVoById(globalUserId));
    }

    @SaCheckRole(SystemConstants.SUPER_ADMIN_ROLE_KEY)
    @SaCheckPermission("system:user:edit")
    @Log(title = "全局账号", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SysGlobalUserBo bo) {
        checkPlatformTenant();
        return globalUserService.updateGlobalUser(bo) > 0 ? R.ok() : R.fail("修改全局账号失败");
    }

    @ApiEncrypt
    @SaCheckRole(SystemConstants.SUPER_ADMIN_ROLE_KEY)
    @SaCheckPermission("system:user:resetPwd")
    @Log(title = "全局账号", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/{globalUserId}/resetPwd")
    public R<Void> resetPwd(@NotNull(message = "全局账号ID不能为空") @PathVariable Long globalUserId,
                            @Validated @RequestBody GlobalUserPasswordBo bo) {
        checkPlatformTenant();
        return globalUserService.resetPassword(globalUserId, BCrypt.hashpw(bo.password())) > 0
            ? R.ok() : R.fail("重置全局账号密码失败");
    }

    private void checkPlatformTenant() {
        if (!TenantConstants.DEFAULT_TENANT_ID.equals(LoginHelper.getTenantId())) {
            throw new ServiceException("仅默认管理租户的超级管理员可以管理全局账号");
        }
    }

    /** 平台管理员重置全局账号密码的请求体。 */
    public record GlobalUserPasswordBo(@NotBlank(message = "密码不能为空") String password) {
    }
}
