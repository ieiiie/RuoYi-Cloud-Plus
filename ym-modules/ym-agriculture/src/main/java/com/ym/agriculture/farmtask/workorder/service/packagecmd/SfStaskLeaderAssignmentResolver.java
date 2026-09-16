package com.ym.agriculture.farmtask.workorder.service.packagecmd;

import cn.hutool.core.collection.CollUtil;
import com.ym.common.core.constant.SystemConstants;
import com.ym.agriculture.farmtask.assignment.dao.SfFarmWorkAssignmentMapper;
import com.ym.agriculture.farmtask.assignment.model.entity.SfFarmWorkAssignment;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskLeaderAssignMode;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrderGreenhouse;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrderItem;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeAccessor;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * 任务包组长分配解析器。
 *
 * <p>集中批量读取农事分工和员工快照，供内容规划与拆单过程复用。</p>
 */
@RequiredArgsConstructor
@Component
public class SfStaskLeaderAssignmentResolver {

    private final SfFarmWorkAssignmentMapper assignmentMapper;
    private final SfStaskEmployeeAccessor employeeAccessor;

    /**
     * 批量查询指定大棚和农事项的有效分工记录。
     *
     * @param tenantId   租户ID
     * @param greenhouseIds 大棚ID集合
     * @param workItemIds 农事项ID集合
     * @return 分工记录
     */
    public List<SfFarmWorkAssignment> selectAssignments(String tenantId, Collection<Long> greenhouseIds,
        Collection<Long> workItemIds) {
        if (CollUtil.isEmpty(greenhouseIds) || CollUtil.isEmpty(workItemIds)) {
            return List.of();
        }
        return assignmentMapper.selectByGreenhousesAndWorkItems(tenantId, greenhouseIds, workItemIds);
    }

    /**
     * 为拆单批量加载仍需动态解析的自动分工索引。
     *
     * @param tenantId   租户ID
     * @param items      任务包农事项
     * @param greenhouses 任务包大棚关系
     * @return 以“大棚ID:农事项ID”为键的分工索引
     */
    public Map<String, SfFarmWorkAssignment> loadAssignmentIndex(String tenantId,
        List<SfStaskWorkOrderItem> items, List<SfStaskWorkOrderGreenhouse> greenhouses) {
        List<Long> autoWorkItemIds = items.stream()
            .filter(item -> item.getLeaderIdSnapshot() == null)
            .filter(item -> !StaskLeaderAssignMode.MANUAL.equals(
                StaskLeaderAssignMode.normalize(item.getLeaderAssignMode())))
            .map(SfStaskWorkOrderItem::getWorkItemId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        List<Long> greenhouseIds = greenhouses.stream()
            .map(SfStaskWorkOrderGreenhouse::getGreenhouseId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        return indexAssignments(selectAssignments(tenantId, greenhouseIds, autoWorkItemIds));
    }

    /**
     * 批量加载员工基础信息并建立ID索引。
     *
     * @param employeeIds 员工ID集合
     * @return 员工索引
     */
    public Map<Long, SysEmployeeVo> loadEmployeeMap(Collection<Long> employeeIds) {
        if (CollUtil.isEmpty(employeeIds)) {
            return Map.of();
        }
        List<Long> ids = employeeIds.stream().filter(Objects::nonNull).distinct().toList();
        if (CollUtil.isEmpty(ids)) {
            return Map.of();
        }
        List<SysEmployeeVo> employees = employeeAccessor.queryBasicByIds(ids);
        if (employees == null) {
            employees = employeeAccessor.queryByIds(ids);
        }
        if (CollUtil.isEmpty(employees)) {
            return Map.of();
        }
        return employees.stream()
            .filter(Objects::nonNull)
            .filter(employee -> employee.getEmployeeId() != null)
            .collect(java.util.stream.Collectors.toMap(SysEmployeeVo::getEmployeeId, Function.identity(),
                (a, b) -> a, HashMap::new));
    }

    /**
     * 将分工记录转换为请求内查询索引。
     *
     * @param assignments 分工记录
     * @return 分工索引
     */
    public static Map<String, SfFarmWorkAssignment> indexAssignments(List<SfFarmWorkAssignment> assignments) {
        return CollUtil.emptyIfNull(assignments).stream()
            .collect(java.util.stream.Collectors.toMap(
                row -> assignmentIndexKey(row.getGreenhouseId(), row.getWorkItemId()),
                Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    /**
     * 判断员工是否为可用的任务组长。
     *
     * @param leader 员工信息
     * @return 是否可承担组长任务
     */
    public static boolean isValidLeader(SysEmployeeVo leader) {
        return leader != null
            && EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER.equals(leader.getAppRoleCode())
            && SystemConstants.NORMAL.equals(leader.getStatus());
    }

    /**
     * 构造分工索引键。
     *
     * @param greenhouseId 大棚ID
     * @param workItemId   农事项ID
     * @return 索引键
     */
    public static String assignmentIndexKey(Long greenhouseId, Long workItemId) {
        return greenhouseId + ":" + workItemId;
    }
}
