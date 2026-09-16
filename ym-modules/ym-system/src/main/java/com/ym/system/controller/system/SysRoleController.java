package com.ym.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.domain.R;
import com.ym.common.core.utils.SpringUtils;
import com.ym.common.excel.utils.ExcelBuilder;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.redis.annotation.RepeatSubmit;
import com.ym.common.web.core.BaseController;
import com.ym.system.domain.bo.SysRoleAuthUserBo;
import com.ym.system.domain.bo.SysRoleBo;
import com.ym.system.domain.bo.SysRoleMenuBo;
import com.ym.system.domain.bo.SysRoleSortBo;
import com.ym.system.domain.bo.SysUserBo;
import com.ym.system.domain.vo.SysRoleVo;
import com.ym.system.domain.vo.SysUserVo;
import com.ym.system.event.OnlineUserCleanEvent;
import com.ym.system.service.ISysRoleService;
import com.ym.system.service.ISysUserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色信息
 *
 * @author Lion Li
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/role")
public class SysRoleController extends BaseController {

    private final ISysRoleService roleService;
    private final ISysUserService userService;

    /**
     * 获取角色信息列表
     */
    @SaCheckPermission("system:role:list")
    @GetMapping("/list")
    public R<PageResult<SysRoleVo>> list(SysRoleBo role, PageQuery pageQuery) {
        return R.ok(roleService.selectPageRoleList(role, pageQuery));
    }

    /**
     * 获取当前租户全部角色。
     */
    @SaCheckPermission("system:role:list")
    @GetMapping("/listAll")
    public R<List<SysRoleVo>> listAll(SysRoleBo role) {
        return R.ok(roleService.selectTenantRoleList(role));
    }

    /**
     * 导出角色信息列表
     */
    @Log(title = "角色管理", businessType = BusinessType.EXPORT)
    @SaCheckPermission("system:role:export")
    @PostMapping("/export")
    public void export(SysRoleBo role, HttpServletResponse response) {
        List<SysRoleVo> list = roleService.selectRoleList(role);
        ExcelBuilder.of(list, SysRoleVo.class).sheetName("角色数据").toResponse(response);
    }

    /**
     * 根据角色编号获取详细信息
     *
     * @param roleId 角色ID
     */
    @SaCheckPermission("system:role:query")
    @GetMapping(value = "/{roleId}")
    public R<SysRoleVo> getInfo(@PathVariable Long roleId) {
        roleService.checkRoleDataScope(roleId);
        return R.ok(roleService.selectRoleById(roleId));
    }

    /**
     * 新增角色
     */
    @SaCheckPermission("system:role:add")
    @Log(title = "角色管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<Long> add(@Validated @RequestBody SysRoleBo role) {
        if (!roleService.checkRoleNameUnique(role)) {
            return R.fail("新增角色'" + role.getRoleName() + "'失败，角色名称已存在");
        }
        roleService.insertRole(role);
        return R.ok(role.getRoleId());
    }

    /**
     * 修改角色名称和备注。
     *
     * @param role 角色参数
     * @return 操作结果
     */
    @SaCheckPermission("system:role:edit")
    @Log(title = "角色管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> edit(@Validated @RequestBody SysRoleBo role) {
        roleService.checkRoleAllowed(role);
        roleService.checkRoleDataScope(role.getRoleId());
        if (!roleService.checkRoleNameUnique(role)) {
            return R.fail("修改角色'" + role.getRoleName() + "'失败，角色名称已存在");
        }

        if (roleService.updateRole(role) > 0) {
            return R.ok();
        }
        return R.fail("修改角色'" + role.getRoleName() + "'失败，请联系管理员");
    }

    /**
     * 修改自定义角色菜单权限。
     */
    @SaCheckPermission("system:role:edit")
    @Log(title = "角色菜单权限", businessType = BusinessType.GRANT)
    @RepeatSubmit()
    @PutMapping("/menu")
    public R<Void> editMenu(@Validated @RequestBody SysRoleMenuBo roleMenu) {
        roleService.updateRoleMenu(roleMenu);
        SpringUtils.context().publishEvent(OnlineUserCleanEvent.byRole(roleMenu.getRoleId()));
        return R.ok();
    }

    /**
     * 保存当前租户完整角色顺序。
     */
    @SaCheckPermission("system:role:edit")
    @Log(title = "角色排序", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping("/sort")
    public R<Void> sort(@Validated @RequestBody SysRoleSortBo sort) {
        roleService.updateRoleSort(sort.getRoleIds());
        return R.ok();
    }

    /**
     * 状态修改
     */
    @SaCheckPermission("system:role:edit")
    @Log(title = "角色管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody SysRoleBo role) {
        roleService.checkRoleAllowed(role);
        roleService.checkRoleDataScope(role.getRoleId());
        if (roleService.updateRoleStatus(role.getRoleId(), role.getStatus()) > 0) {
            SpringUtils.context().publishEvent(OnlineUserCleanEvent.byRole(role.getRoleId()));
            return R.ok();
        }
        return R.fail("修改角色'" + role.getRoleName() + "'状态失败，请联系管理员");
    }

    /**
     * 删除角色
     *
     * @param roleIds 角色ID串
     */
    @SaCheckPermission("system:role:remove")
    @Log(title = "角色管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{roleIds}")
    public R<Void> remove(@PathVariable Long[] roleIds) {
        return toAjax(roleService.deleteRoleByIds(List.of(roleIds)));
    }

    /**
     * 获取角色选择框列表
     *
     * @param roleIds 角色ID串
     */
    @SaCheckPermission("system:role:query")
    @GetMapping("/optionselect")
    public R<List<SysRoleVo>> optionselect(@RequestParam(required = false) Long[] roleIds) {
        return R.ok(roleService.selectRoleByIds(roleIds == null ? null : List.of(roleIds)));
    }

    /**
     * 分页查询当前租户全部用户，并返回当前角色授权状态。
     */
    @SaCheckPermission("system:role:list")
    @GetMapping("/authUser/list")
    public R<PageResult<SysUserVo>> authUserList(SysUserBo user, PageQuery pageQuery) {
        roleService.checkRoleDataScope(user.getRoleId());
        return R.ok(userService.selectRoleAuthUserList(user, pageQuery));
    }

    /**
     * 事务性保存角色用户授权变更。
     */
    @SaCheckPermission("system:role:edit")
    @Log(title = "角色用户授权", businessType = BusinessType.GRANT)
    @RepeatSubmit()
    @PutMapping("/authUser/change")
    public R<Void> changeAuthUsers(@Validated @RequestBody SysRoleAuthUserBo authUser) {
        roleService.changeAuthUsers(authUser);
        return R.ok();
    }

}
