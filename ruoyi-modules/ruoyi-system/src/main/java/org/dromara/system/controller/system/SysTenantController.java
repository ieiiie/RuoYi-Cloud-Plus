package org.dromara.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.constant.TenantConstants;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.encrypt.annotation.ApiEncrypt;
import org.dromara.common.excel.utils.ExcelBuilder;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.redis.annotation.RepeatSubmit;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.domain.bo.SysTenantBo;
import org.dromara.system.domain.vo.SysTenantVo;
import org.dromara.system.service.ISysTenantService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 平台租户管理。
 *
 * <p>接口只在 {@code tenant.enable=true} 时注册，并且要求默认租户的超级
 * 管理员角色。普通租户管理员即使拥有系统管理菜单，也不能新建或切换租户。</p>
 *
 * @author Lion Li
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/tenant")
@ConditionalOnProperty(value = "tenant.enable", havingValue = "true")
public class SysTenantController extends BaseController {

    private final ISysTenantService tenantService;

    @SaCheckRole(SystemConstants.SUPER_ADMIN_ROLE_KEY)
    @SaCheckPermission("system:tenant:list")
    @GetMapping("/list")
    public R<PageResult<SysTenantVo>> list(SysTenantBo bo, PageQuery pageQuery) {
        checkPlatformTenant();
        return R.ok(tenantService.queryPageList(bo, pageQuery));
    }

    @SaCheckRole(SystemConstants.SUPER_ADMIN_ROLE_KEY)
    @SaCheckPermission("system:tenant:export")
    @Log(title = "租户管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SysTenantBo bo, HttpServletResponse response) {
        checkPlatformTenant();
        List<SysTenantVo> list = tenantService.queryList(bo);
        ExcelBuilder.of(list, SysTenantVo.class).sheetName("租户数据").toResponse(response);
    }

    @SaCheckRole(SystemConstants.SUPER_ADMIN_ROLE_KEY)
    @SaCheckPermission("system:tenant:query")
    @GetMapping("/{id}")
    public R<SysTenantVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        checkPlatformTenant();
        return R.ok(tenantService.queryById(id));
    }

    @ApiEncrypt
    @SaCheckRole(SystemConstants.SUPER_ADMIN_ROLE_KEY)
    @SaCheckPermission("system:tenant:add")
    @Log(title = "租户管理", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SysTenantBo bo) {
        checkPlatformTenant();
        if (!tenantService.checkCompanyNameUnique(bo)) {
            return R.fail("新增租户'" + bo.getCompanyName() + "'失败，企业名称已存在");
        }
        return toAjax(tenantService.insertByBo(bo));
    }

    @SaCheckRole(SystemConstants.SUPER_ADMIN_ROLE_KEY)
    @SaCheckPermission("system:tenant:edit")
    @Log(title = "租户管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SysTenantBo bo) {
        checkPlatformTenant();
        SysTenantVo tenant = tenantService.queryById(bo.getId());
        if (tenant == null) {
            return R.fail("租户不存在");
        }
        tenantService.checkTenantAllowed(tenant.getTenantId());
        if (!tenantService.checkCompanyNameUnique(bo)) {
            return R.fail("修改租户'" + bo.getCompanyName() + "'失败，企业名称已存在");
        }
        return toAjax(tenantService.updateByBo(bo));
    }

    @SaCheckRole(SystemConstants.SUPER_ADMIN_ROLE_KEY)
    @SaCheckPermission("system:tenant:edit")
    @Log(title = "租户管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody SysTenantBo bo) {
        checkPlatformTenant();
        return toAjax(tenantService.updateTenantStatus(bo));
    }

    @SaCheckRole(SystemConstants.SUPER_ADMIN_ROLE_KEY)
    @SaCheckPermission("system:tenant:remove")
    @Log(title = "租户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        checkPlatformTenant();
        return toAjax(tenantService.deleteWithValidByIds(List.of(ids), true));
    }

    /**
     * 平台管理员临时进入指定租户的数据视图；选择会保存在当前登录会话中。
     */
    @SaCheckRole(SystemConstants.SUPER_ADMIN_ROLE_KEY)
    @GetMapping("/dynamic/{tenantId}")
    public R<Void> dynamicTenant(@NotBlank(message = "租户编号不能为空") @PathVariable String tenantId) {
        checkPlatformTenant();
        tenantService.checkTenantAvailable(tenantId);
        TenantHelper.setDynamic(tenantId, true);
        return R.ok();
    }

    @SaCheckRole(SystemConstants.SUPER_ADMIN_ROLE_KEY)
    @GetMapping("/dynamic/clear")
    public R<Void> clearDynamicTenant() {
        checkPlatformTenant();
        TenantHelper.clearDynamic();
        return R.ok();
    }

    @SaCheckRole(SystemConstants.SUPER_ADMIN_ROLE_KEY)
    @SaCheckPermission("system:tenant:edit")
    @Log(title = "租户管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/{tenantId}/package/{packageId}")
    public R<Void> syncTenantPackage(@NotBlank(message = "租户编号不能为空") @PathVariable String tenantId,
                                     @NotNull(message = "套餐编号不能为空") @PathVariable Long packageId) {
        checkPlatformTenant();
        return toAjax(tenantService.syncTenantPackage(tenantId, packageId));
    }

    /**
     * 平台能力仅由默认管理租户的超级管理员使用。
     *
     * <p>角色注解负责身份校验；此处额外绑定登录态中的租户编号，防止未来错误
     * 配置同名角色时跨租户取得平台权限。动态租户只影响业务数据范围，不改变
     * 登录会话所属的默认平台租户。</p>
     */
    private void checkPlatformTenant() {
        if (!TenantConstants.DEFAULT_TENANT_ID.equals(LoginHelper.getTenantId())) {
            throw new ServiceException("仅默认管理租户的超级管理员可以管理租户");
        }
    }

}
