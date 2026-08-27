package org.dromara.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.hutool.crypto.digest.BCrypt;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.constant.TenantConstants;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.encrypt.annotation.ApiEncrypt;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.redis.annotation.RepeatSubmit;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.bo.SysGlobalUserBo;
import org.dromara.system.domain.vo.SysGlobalUserVo;
import org.dromara.system.service.ISysGlobalUserService;
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
