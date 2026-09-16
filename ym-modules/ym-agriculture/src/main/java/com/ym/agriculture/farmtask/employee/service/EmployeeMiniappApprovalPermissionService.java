package com.ym.agriculture.farmtask.employee.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;
import com.ym.agriculture.farmtask.employee.dao.SysEmployeeMiniappRoleMapper;
import com.ym.agriculture.farmtask.employee.model.entity.SysEmployeeMiniappRole;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import com.ym.system.api.RemoteRoleService;
import com.ym.system.api.domain.vo.RemoteRoleVo;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 小程序人员系统角色绑定与权限解析服务。
 */
@Service
@RequiredArgsConstructor
public class EmployeeMiniappApprovalPermissionService {

    private final SysEmployeeMiniappRoleMapper employeeMiniappRoleMapper;

    @DubboReference
    private RemoteRoleService remoteRoleService;

    /**
     * 判断人员是否具有小程序注册审批权限。
     *
     * @param employee 人员档案
     * @return 具备领导岗位且绑定角色包含审批权限时返回 true
     */
    public boolean canApproveRegister(SysEmployeeVo employee) {
        if (employee == null
            || !EmployeeConstants.APP_ROLE_STASK_LEADER.equals(employee.getAppRoleCode())) {
            return false;
        }
        return resolvePermissions(employee).contains(EmployeeConstants.PERMISSION_MINIAPP_REGISTER_APPROVE);
    }

    /**
     * 校验人员待保存的小程序系统角色。
     *
     * @param appRoleCode 人员小程序岗位
     * @param tenantId 租户编号
     * @param roleIds 系统角色ID集合，可为空
     */
    public void validateBindings(String appRoleCode, String tenantId, Collection<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return;
        }
        if (StringUtils.isBlank(tenantId)) {
            throw new ServiceException("人员租户不能为空");
        }
        List<Long> distinctRoleIds = roleIds.stream()
            .filter(java.util.Objects::nonNull)
            .distinct()
            .toList();
        if (CollUtil.isEmpty(distinctRoleIds)) {
            return;
        }
        if (distinctRoleIds.size() != roleIds.stream().filter(java.util.Objects::nonNull).count()) {
            throw new ServiceException("小程序系统角色不能重复");
        }
        if (remoteRoleService.filterActiveRoleIds(tenantId, distinctRoleIds).size() != distinctRoleIds.size()) {
            throw new ServiceException("小程序系统角色不存在、已停用或不属于当前租户");
        }
    }

    /**
     * 查询当前租户可绑定给小程序人员的系统角色。
     *
     * @return 全部正常系统角色
     */
    public List<RemoteRoleVo> listBindableRoles() {
        String tenantId = TenantHelper.getTenantId();
        return remoteRoleService.listActiveRoles(tenantId);
    }

    /**
     * 覆盖保存人员的小程序系统角色绑定。
     *
     * @param employeeId 人员ID
     * @param tenantId 租户编号
     * @param appRoleCode 小程序岗位编码
     * @param roleIds 系统角色ID集合
     */
    public void replaceBindings(Long employeeId, String tenantId, String appRoleCode, Collection<Long> roleIds) {
        List<Long> normalizedRoleIds = normalizeRoleIds(roleIds);
        validateBindings(appRoleCode, tenantId, normalizedRoleIds);
        employeeMiniappRoleMapper.delete(new QueryWrapper<SysEmployeeMiniappRole>()
            .eq("employee_id", employeeId));
        if (CollUtil.isEmpty(normalizedRoleIds)) {
            return;
        }
        employeeMiniappRoleMapper.insertBatch(normalizedRoleIds.stream().map(roleId -> {
            SysEmployeeMiniappRole relation = new SysEmployeeMiniappRole();
            relation.setEmployeeId(employeeId);
            relation.setRoleId(roleId);
            relation.setTenantId(tenantId);
            return relation;
        }).toList());
    }

    /**
     * 删除人员的小程序系统角色绑定。
     *
     * @param employeeId 人员ID
     */
    public void removeBindings(Long employeeId) {
        employeeMiniappRoleMapper.delete(new QueryWrapper<SysEmployeeMiniappRole>()
            .eq("employee_id", employeeId));
    }

    /**
     * 批量为人员视图填充绑定角色ID，避免人员列表 N+1 查询。
     *
     * @param employees 人员视图列表
     */
    public void enrichRoleIds(List<SysEmployeeVo> employees) {
        if (CollUtil.isEmpty(employees)) {
            return;
        }
        List<Long> employeeIds = employees.stream()
            .map(SysEmployeeVo::getEmployeeId)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .toList();
        if (CollUtil.isEmpty(employeeIds)) {
            return;
        }
        Map<Long, List<Long>> roleIdsByEmployeeId = employeeMiniappRoleMapper.selectList(
                new QueryWrapper<SysEmployeeMiniappRole>().in("employee_id", employeeIds))
            .stream()
            .collect(Collectors.groupingBy(SysEmployeeMiniappRole::getEmployeeId,
                Collectors.mapping(SysEmployeeMiniappRole::getRoleId, Collectors.toList())));
        for (SysEmployeeVo employee : employees) {
            List<Long> roleIds = roleIdsByEmployeeId.getOrDefault(employee.getEmployeeId(), List.of());
            employee.setMiniappRoleIds(roleIds);
            employee.setMiniappRegisterApprovalRoleId(roleIds.stream().findFirst().orElse(null));
        }
    }

    /**
     * 返回可下发给小程序的权限码集合。
     *
     * @param employee 人员档案
     * @return 当前支持的小程序权限集合
     */
    public Set<String> resolvePermissions(SysEmployeeVo employee) {
        if (employee == null) {
            return Set.of();
        }
        List<Long> roleIds = listEmployeeRoleIds(employee.getEmployeeId(), employee.getTenantId());
        if (CollUtil.isEmpty(roleIds)) {
            return Set.of();
        }
        List<Long> validRoleIds = remoteRoleService.filterActiveRoleIds(employee.getTenantId(), roleIds);
        if (CollUtil.isEmpty(validRoleIds)) {
            return Set.of();
        }
        Set<String> supportedPermissions = EmployeeConstants.APP_ROLE_STASK_LEADER.equals(employee.getAppRoleCode())
            ? Set.of(EmployeeConstants.PERMISSION_MINIAPP_REGISTER_APPROVE,
                EmployeeConstants.PERMISSION_MINIAPP_INSPECTION_PHOTO_ACCESS)
            : Set.of(EmployeeConstants.PERMISSION_MINIAPP_INSPECTION_PHOTO_ACCESS);
        return remoteRoleService.selectPermissions(employee.getTenantId(), validRoleIds).stream()
            .filter(StringUtils::isNotBlank)
            .filter(supportedPermissions::contains)
            .collect(Collectors.toSet());
    }

    private List<Long> normalizeRoleIds(Collection<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return List.of();
        }
        return roleIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
    }

    private List<Long> listEmployeeRoleIds(Long employeeId, String tenantId) {
        if (employeeId == null || StringUtils.isBlank(tenantId)) {
            return List.of();
        }
        return employeeMiniappRoleMapper.selectObjs(new QueryWrapper<SysEmployeeMiniappRole>()
            .select("role_id")
            .eq("employee_id", employeeId)
            .eq("tenant_id", tenantId));
    }
}
