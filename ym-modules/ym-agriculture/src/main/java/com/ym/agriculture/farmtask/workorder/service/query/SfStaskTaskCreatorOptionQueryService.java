package com.ym.agriculture.farmtask.workorder.service.query;

import cn.hutool.core.collection.CollUtil;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.shared.i18n.StaskMessageResolver;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskTaskPackageMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderMapper;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskCreatorRole;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskTaskPackage;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskTaskCreatorOptionVo;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeAccessor;
import com.ym.agriculture.farmtask.workorder.support.SfStaskRoleNameReader;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * stask 小程序任务发起人选项查询。
 */
@RequiredArgsConstructor
@Service
public class SfStaskTaskCreatorOptionQueryService {

    private static final List<String> ALL_STATUSES = List.of(
        StaskOrderStatus.DRAFT, StaskOrderStatus.PENDING_TECH_CONFIRM,
        StaskOrderStatus.TECH_REJECTED, StaskOrderStatus.PENDING_LEADER_ACCEPT,
        StaskOrderStatus.ASSIGN_COMPLETE, StaskOrderStatus.LEADER_ARRIVED,
        StaskOrderStatus.PENDING_ACCEPTANCE, StaskOrderStatus.ACCEPTANCE_PASSED,
        StaskOrderStatus.ACCEPTANCE_REJECTED, StaskOrderStatus.VOIDED, StaskOrderStatus.CANCELLED);

    private final SfStaskTaskPackageMapper taskPackageMapper;
    private final SfStaskWorkOrderMapper workOrderMapper;
    private final SfStaskEmployeeAccessor employeeAccessor;
    private final SfStaskRoleNameReader roleNameReader;
    private final StaskMessageResolver messages;

    /**
     * 查询当前角色可见任务中的发起人。
     *
     * @param roleCode 当前业务角色
     * @param keyword  姓名关键字
     * @return 去重后的发起人选项
     */
    public List<SfStaskTaskCreatorOptionVo> options(String roleCode, String keyword) {
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_COMMON_TENANT_CONTEXT_MISSING);
        }
        Long employeeId = LoginHelper.getUserId();
        Map<Long, String> roleByEmployee = new LinkedHashMap<>();
        if (EmployeeConstants.APP_ROLE_STASK_EXPERT.equals(roleCode)) {
            taskPackageMapper.selectTechnicianAllTasks(tenantId, employeeId, StaskOrderStatus.DRAFT,
                ALL_STATUSES, null, null, null, null, true)
                .forEach(row -> roleByEmployee.putIfAbsent(row.getCreatorEmployeeId(), row.getCreatorRoleCode()));
            workOrderMapper.selectExpertAllTaskSplits(tenantId, employeeId, ALL_STATUSES,
                null, null, null, null, true)
                .forEach(row -> roleByEmployee.putIfAbsent(row.getCreatorEmployeeId(), row.getCreatorRoleCode()));
        } else if (EmployeeConstants.APP_ROLE_STASK_PRODUCTION_ADMIN.equals(roleCode)) {
            taskPackageMapper.selectManagerAllTasks(tenantId, employeeId, ALL_STATUSES,
                null, null, null, null)
                .forEach(row -> roleByEmployee.putIfAbsent(row.getCreatorEmployeeId(), row.getCreatorRoleCode()));
            workOrderMapper.selectManagerAllTaskSplits(tenantId, ALL_STATUSES,
                null, null, null, null)
                .forEach(row -> roleByEmployee.putIfAbsent(row.getCreatorEmployeeId(), row.getCreatorRoleCode()));
        } else if (EmployeeConstants.APP_ROLE_STASK_LEADER.equals(roleCode)) {
            taskPackageMapper.selectLeaderAdminAllTasks(tenantId, ALL_STATUSES,
                null, null, null, null)
                .forEach(row -> roleByEmployee.putIfAbsent(row.getCreatorEmployeeId(), row.getCreatorRoleCode()));
            workOrderMapper.selectAllTaskSplits(tenantId, ALL_STATUSES,
                null, null, null, null)
                .forEach(row -> roleByEmployee.putIfAbsent(row.getCreatorEmployeeId(), row.getCreatorRoleCode()));
        }
        roleByEmployee.remove(null);
        if (roleByEmployee.isEmpty()) {
            return List.of();
        }

        List<SysEmployeeVo> employees = employeeAccessor.queryBasicByIds(roleByEmployee.keySet());
        if (CollUtil.isEmpty(employees)) {
            employees = employeeAccessor.queryByIds(roleByEmployee.keySet());
        }
        String normalizedKeyword = StringUtils.trim(keyword);
        String keywordLower = normalizedKeyword == null ? "" : normalizedKeyword.toLowerCase(Locale.ROOT);
        List<SysEmployeeVo> visibleEmployees = new ArrayList<>();
        for (SysEmployeeVo employee : CollUtil.emptyIfNull(employees)) {
            if (employee == null || employee.getEmployeeId() == null
                || !roleByEmployee.containsKey(employee.getEmployeeId())) {
                continue;
            }
            String name = Objects.toString(employee.getName(), "");
            if (keywordLower.isEmpty() || name.toLowerCase(Locale.ROOT).contains(keywordLower)) {
                visibleEmployees.add(employee);
            }
        }
        Map<String, String> roleNames = roleNameReader.load(roleByEmployee.values());
        return visibleEmployees.stream()
            .map(employee -> toOption(employee, roleByEmployee.get(employee.getEmployeeId()), roleNames))
            .sorted(Comparator.comparing(SfStaskTaskCreatorOptionVo::getEmployeeName,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(SfStaskTaskCreatorOptionVo::getEmployeeId,
                    Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();
    }

    private SfStaskTaskCreatorOptionVo toOption(SysEmployeeVo employee, String roleCode,
        Map<String, String> roleNames) {
        SfStaskTaskCreatorOptionVo vo = new SfStaskTaskCreatorOptionVo();
        vo.setEmployeeId(employee.getEmployeeId());
        vo.setEmployeeName(employee.getName());
        vo.setCreatorRoleCode(roleCode);
        vo.setCreatorRoleName(roleNameReader.resolve(roleNames, roleCode));
        return vo;
    }
}
