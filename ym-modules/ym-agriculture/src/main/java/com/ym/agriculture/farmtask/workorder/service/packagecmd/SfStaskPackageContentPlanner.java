package com.ym.agriculture.farmtask.workorder.service.packagecmd;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.farming.field.dao.SfFieldMapper;
import com.ym.agriculture.farming.field.model.constants.FieldType;
import com.ym.agriculture.farming.field.model.entity.SfField;
import com.ym.agriculture.farmtask.assignment.model.entity.SfFarmWorkAssignment;
import com.ym.agriculture.farming.farmwork.dao.SfFarmWorkDictMapper;
import com.ym.agriculture.farming.farmwork.model.constants.FarmWorkNodeType;
import com.ym.agriculture.farming.farmwork.model.entity.SfFarmWorkDict;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderGreenhouseMapper;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskTaskScopeBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskWorkItemCreateBo;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskLeaderAssignMode;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrderItem;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskTaskScopeConflictVo;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 任务包内容规划器。
 *
 * <p>负责请求范围校验、重复任务检查以及按最终有效组长生成规范化农事项计划，不写数据库。</p>
 */
@RequiredArgsConstructor
@Component
public class SfStaskPackageContentPlanner {

    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");

    private static final List<String> DUPLICATE_BLOCKING_PACKAGE_STATUSES = List.of(
        StaskOrderStatus.PENDING_TECH_CONFIRM,
        StaskOrderStatus.TECH_REJECTED
    );

    private static final List<String> DUPLICATE_BLOCKING_ORDER_STATUSES = List.of(
        StaskOrderStatus.PENDING_LEADER_ACCEPT,
        StaskOrderStatus.ASSIGN_COMPLETE,
        StaskOrderStatus.LEADER_ARRIVED,
        StaskOrderStatus.PENDING_ACCEPTANCE
    );

    private final com.ym.agriculture.shared.i18n.StaskMessageResolver messages;
    private final SfStaskWorkOrderGreenhouseMapper greenhouseMapper;
    private final SfFarmWorkDictMapper farmWorkDictMapper;
    private final SfFieldMapper fieldMapper;
    private final SfStaskLeaderAssignmentResolver assignmentResolver;

    /**
     * 校验同一请求内的农事项和大棚范围不可重复。
     *
     * @param workItems 请求农事项
     */
    public void validateNoDuplicateRequestScopes(List<SfStaskWorkItemCreateBo> workItems) {
        if (CollUtil.isEmpty(workItems)) {
            return;
        }
        Set<String> seen = new HashSet<>();
        for (SfStaskWorkItemCreateBo workItem : workItems) {
            if (workItem == null || workItem.getWorkItemId() == null
                || CollUtil.isEmpty(workItem.getGreenhouseIds())) {
                continue;
            }
            for (Long greenhouseId : workItem.getGreenhouseIds()) {
                if (greenhouseId != null && !seen.add(workItem.getWorkItemId() + ":" + greenhouseId)) {
                    throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_DUPLICATE_GREENHOUSE_SELECTION);
                }
            }
        }
    }

    /**
     * 校验计划日期下不存在仍在进行中的同范围任务。
     *
     * @param tenantId        租户ID
     * @param planDate        计划日期
     * @param workItems       请求农事项
     * @param excludePackageId 编辑时排除的任务包ID
     */
    public void ensureNoDuplicateActiveTaskScope(String tenantId, Date planDate,
        List<SfStaskWorkItemCreateBo> workItems, Long excludePackageId) {
        if (planDate == null || CollUtil.isEmpty(workItems)) {
            return;
        }
        List<SfStaskTaskScopeBo> scopes = flattenTaskScopes(workItems);
        if (CollUtil.isEmpty(scopes)) {
            return;
        }
        List<SfStaskTaskScopeConflictVo> conflicts = new ArrayList<>();
        conflicts.addAll(greenhouseMapper.selectActiveScopeConflictsForPackages(
            tenantId, planDate, scopes, excludePackageId, DUPLICATE_BLOCKING_PACKAGE_STATUSES));
        conflicts.addAll(greenhouseMapper.selectActiveScopeConflictsForSplitOrders(
            tenantId, planDate, scopes, DUPLICATE_BLOCKING_ORDER_STATUSES));
        if (CollUtil.isEmpty(conflicts)) {
            return;
        }
        enrichTaskScopeConflictNames(tenantId, conflicts);
        throw new ServiceException(formatTaskScopeConflictMessage(planDate, conflicts.get(0)));
    }

    /**
     * 按最终有效组长归一化请求明细：手动指派保持请求粒度，自动指派按组长分组。
     *
     * @param tenantId                租户ID
     * @param workItems               请求农事项
     * @param validateLeaderAssignment 是否要求组长分配完整
     * @return 规范化农事项计划
     */
    public List<NormalizedWorkItem> normalizeWorkItems(String tenantId,
        List<SfStaskWorkItemCreateBo> workItems, boolean validateLeaderAssignment) {
        if (CollUtil.isEmpty(workItems) || workItems.stream().anyMatch(Objects::isNull)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_WORK_ITEM_REQUIRED);
        }
        Map<Long, WorkItemContent> canonicalContents = buildCanonicalWorkItemContents(workItems);
        List<Long> autoWorkItemIds = workItems.stream()
            .filter(item -> StaskLeaderAssignMode.AUTO.equals(
                StaskLeaderAssignMode.normalize(item.getLeaderAssignMode())))
            .map(SfStaskWorkItemCreateBo::getWorkItemId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        List<Long> greenhouseIds = workItems.stream()
            .flatMap(item -> distinctIds(item.getGreenhouseIds(), messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_PACKAGE_GREENHOUSE_REQUIRED)).stream())
            .distinct()
            .toList();
        List<SfFarmWorkAssignment> assignments = assignmentResolver.selectAssignments(
            tenantId, greenhouseIds, autoWorkItemIds);
        Map<String, SfFarmWorkAssignment> assignmentIndex =
            SfStaskLeaderAssignmentResolver.indexAssignments(assignments);
        List<Long> employeeIds = Stream.concat(
                CollUtil.emptyIfNull(assignments).stream().map(SfFarmWorkAssignment::getLeaderId),
                workItems.stream().map(SfStaskWorkItemCreateBo::getManualLeaderId))
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        Map<Long, SysEmployeeVo> employeeMap = assignmentResolver.loadEmployeeMap(employeeIds);

        List<NormalizedWorkItem> result = new ArrayList<>();
        for (SfStaskWorkItemCreateBo workItem : workItems) {
            normalizeWorkItem(workItem, validateLeaderAssignment, canonicalContents,
                assignmentIndex, employeeMap, result);
        }
        return result;
    }

    /**
     * 技术确认前校验已保存任务包中的手动组长仍然有效。
     *
     * @param items 已保存农事项
     */
    public void validatePackageLeaderAssignments(List<SfStaskWorkOrderItem> items) {
        List<Long> leaderIds = items.stream()
            .filter(item -> StaskLeaderAssignMode.MANUAL.equals(
                StaskLeaderAssignMode.normalize(item.getLeaderAssignMode())))
            .map(SfStaskWorkOrderItem::getManualLeaderId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        Map<Long, SysEmployeeVo> leaderMap = assignmentResolver.loadEmployeeMap(leaderIds);
        for (SfStaskWorkOrderItem item : items) {
            String mode = StaskLeaderAssignMode.normalize(item.getLeaderAssignMode());
            if (!StaskLeaderAssignMode.isSupported(mode)) {
                throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_ASSIGNMENT_INVALID_LEADER_MODE);
            }
            if (StaskLeaderAssignMode.MANUAL.equals(mode)
                && !SfStaskLeaderAssignmentResolver.isValidLeader(leaderMap.get(item.getManualLeaderId()))) {
                throw new ServiceException(item.getManualLeaderId() == null
                    ? messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_ASSIGNMENT_MANUAL_LEADER_REQUIRED) : messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_ASSIGNMENT_MANUAL_LEADER_UNAVAILABLE));
            }
        }
    }

    /**
     * 提交前批量校验所有自动和手动组长。
     *
     * @param tenantId 租户ID
     * @param workItems 请求农事项
     */
    public void validateWorkItemLeaderAssignments(String tenantId, List<SfStaskWorkItemCreateBo> workItems) {
        if (CollUtil.isEmpty(workItems)) {
            return;
        }
        List<Long> autoWorkItemIds = new ArrayList<>();
        List<Long> greenhouseIds = new ArrayList<>();
        List<Long> manualLeaderIds = new ArrayList<>();
        collectAssignmentScope(workItems, autoWorkItemIds, greenhouseIds, manualLeaderIds);
        List<SfFarmWorkAssignment> assignments = assignmentResolver.selectAssignments(
            tenantId, greenhouseIds, autoWorkItemIds);
        Map<String, SfFarmWorkAssignment> assignmentIndex =
            SfStaskLeaderAssignmentResolver.indexAssignments(assignments);
        List<Long> leaderIds = Stream.concat(manualLeaderIds.stream(),
                CollUtil.emptyIfNull(assignments).stream().map(SfFarmWorkAssignment::getLeaderId))
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        Map<Long, SysEmployeeVo> leaderMap = assignmentResolver.loadEmployeeMap(leaderIds);
        Map<Long, SfFarmWorkDict> workItemMap = queryWorkItemMap(tenantId, workItems.stream()
            .map(SfStaskWorkItemCreateBo::getWorkItemId).filter(Objects::nonNull).toList());

        for (SfStaskWorkItemCreateBo workItem : workItems) {
            validateWorkItemLeader(workItem, assignmentIndex, leaderMap, workItemMap);
        }
    }

    private void normalizeWorkItem(SfStaskWorkItemCreateBo workItem, boolean validateLeaderAssignment,
        Map<Long, WorkItemContent> canonicalContents, Map<String, SfFarmWorkAssignment> assignmentIndex,
        Map<Long, SysEmployeeVo> employeeMap, List<NormalizedWorkItem> result) {
        String mode = StaskLeaderAssignMode.normalize(workItem.getLeaderAssignMode());
        if (!StaskLeaderAssignMode.isSupported(mode)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_ASSIGNMENT_INVALID_LEADER_MODE);
        }
        List<Long> scopedGreenhouseIds = distinctIds(workItem.getGreenhouseIds(), messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_PACKAGE_GREENHOUSE_REQUIRED));
        if (StaskLeaderAssignMode.MANUAL.equals(mode)) {
            addManualWorkItem(workItem, scopedGreenhouseIds, mode, validateLeaderAssignment,
                canonicalContents, employeeMap, result);
            return;
        }
        addAutoWorkItems(workItem, scopedGreenhouseIds, mode, canonicalContents,
            assignmentIndex, employeeMap, result);
    }

    private void addManualWorkItem(SfStaskWorkItemCreateBo workItem, List<Long> greenhouseIds,
        String mode, boolean validateLeaderAssignment, Map<Long, WorkItemContent> canonicalContents,
        Map<Long, SysEmployeeVo> employeeMap, List<NormalizedWorkItem> result) {
        SysEmployeeVo leader = employeeMap.get(workItem.getManualLeaderId());
        if (validateLeaderAssignment && !SfStaskLeaderAssignmentResolver.isValidLeader(leader)) {
            throw new ServiceException(workItem.getManualLeaderId() == null
                ? messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_ASSIGNMENT_MANUAL_LEADER_REQUIRED) : messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_ASSIGNMENT_MANUAL_LEADER_UNAVAILABLE));
        }
        result.add(new NormalizedWorkItem(copyWorkItem(workItem, greenhouseIds, mode,
            canonicalContents.get(workItem.getWorkItemId())), workItem.getManualLeaderId(),
            leader == null ? null : leader.getName()));
    }

    private void addAutoWorkItems(SfStaskWorkItemCreateBo workItem, List<Long> greenhouseIds,
        String mode, Map<Long, WorkItemContent> canonicalContents,
        Map<String, SfFarmWorkAssignment> assignmentIndex, Map<Long, SysEmployeeVo> employeeMap,
        List<NormalizedWorkItem> result) {
        Map<LeaderSnapshotKey, List<Long>> greenhouseGroups = new LinkedHashMap<>();
        for (Long greenhouseId : greenhouseIds) {
            SfFarmWorkAssignment assignment = assignmentIndex.get(
                SfStaskLeaderAssignmentResolver.assignmentIndexKey(greenhouseId, workItem.getWorkItemId()));
            SysEmployeeVo leader = assignment == null ? null : employeeMap.get(assignment.getLeaderId());
            if (!SfStaskLeaderAssignmentResolver.isValidLeader(leader)) {
                leader = null;
            }
            LeaderSnapshotKey key = new LeaderSnapshotKey(
                leader == null ? null : leader.getEmployeeId(), leader == null ? null : leader.getName());
            greenhouseGroups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(greenhouseId);
        }
        for (Map.Entry<LeaderSnapshotKey, List<Long>> entry : greenhouseGroups.entrySet()) {
            LeaderSnapshotKey leader = entry.getKey();
            result.add(new NormalizedWorkItem(copyWorkItem(workItem, entry.getValue(), mode,
                canonicalContents.get(workItem.getWorkItemId())), leader.leaderId(), leader.leaderName()));
        }
    }

    private void collectAssignmentScope(List<SfStaskWorkItemCreateBo> workItems,
        List<Long> autoWorkItemIds, List<Long> greenhouseIds, List<Long> manualLeaderIds) {
        for (SfStaskWorkItemCreateBo workItem : workItems) {
            String mode = StaskLeaderAssignMode.normalize(workItem.getLeaderAssignMode());
            if (!StaskLeaderAssignMode.isSupported(mode)) {
                throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_ASSIGNMENT_INVALID_LEADER_MODE);
            }
            if (StaskLeaderAssignMode.MANUAL.equals(mode)) {
                if (workItem.getManualLeaderId() != null) {
                    manualLeaderIds.add(workItem.getManualLeaderId());
                }
                continue;
            }
            autoWorkItemIds.add(workItem.getWorkItemId());
            greenhouseIds.addAll(distinctIds(workItem.getGreenhouseIds(), messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_PACKAGE_GREENHOUSE_REQUIRED)));
        }
    }

    private void validateWorkItemLeader(SfStaskWorkItemCreateBo workItem,
        Map<String, SfFarmWorkAssignment> assignmentIndex, Map<Long, SysEmployeeVo> leaderMap,
        Map<Long, SfFarmWorkDict> workItemMap) {
        String mode = StaskLeaderAssignMode.normalize(workItem.getLeaderAssignMode());
        if (StaskLeaderAssignMode.MANUAL.equals(mode)) {
            if (!SfStaskLeaderAssignmentResolver.isValidLeader(leaderMap.get(workItem.getManualLeaderId()))) {
                throw new ServiceException(workItem.getManualLeaderId() == null
                    ? messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_ASSIGNMENT_MANUAL_LEADER_REQUIRED) : messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_ASSIGNMENT_MANUAL_LEADER_UNAVAILABLE));
            }
            return;
        }
        SfFarmWorkDict dict = workItemMap.get(workItem.getWorkItemId());
        String workItemName = dict == null ? messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_COMMON_DEFAULT_WORK_ITEM) : dict.getDictName();
        for (Long greenhouseId : distinctIds(workItem.getGreenhouseIds(), messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_PACKAGE_GREENHOUSE_REQUIRED))) {
            SfFarmWorkAssignment assignment = assignmentIndex.get(
                SfStaskLeaderAssignmentResolver.assignmentIndexKey(greenhouseId, workItem.getWorkItemId()));
            SysEmployeeVo leader = assignment == null ? null : leaderMap.get(assignment.getLeaderId());
            if (!SfStaskLeaderAssignmentResolver.isValidLeader(leader)) {
                throw messages.exception(
                    com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_GREENHOUSE_LEADER_UNASSIGNED,
                    workItemName);
            }
        }
    }

    private static Map<Long, WorkItemContent> buildCanonicalWorkItemContents(
        List<SfStaskWorkItemCreateBo> workItems) {
        Map<Long, WorkItemContent> result = new LinkedHashMap<>();
        for (SfStaskWorkItemCreateBo workItem : workItems) {
            result.putIfAbsent(workItem.getWorkItemId(), WorkItemContent.from(workItem));
        }
        return result;
    }

    private static SfStaskWorkItemCreateBo copyWorkItem(SfStaskWorkItemCreateBo source,
        List<Long> greenhouseIds, String mode, WorkItemContent content) {
        SfStaskWorkItemCreateBo copy = new SfStaskWorkItemCreateBo();
        copy.setWorkItemId(source.getWorkItemId());
        copy.setGreenhouseIds(new ArrayList<>(greenhouseIds));
        copy.setManagerRequirement(content.managerRequirement());
        copy.setManagerPhotos(content.managerPhotos());
        copy.setTechInstruction(content.techInstruction());
        copy.setTechPhotos(content.techPhotos());
        copy.setMaterials(content.materials() == null ? List.of() : new ArrayList<>(content.materials()));
        copy.setLeaderAssignMode(mode);
        copy.setManualLeaderId(StaskLeaderAssignMode.MANUAL.equals(mode) ? source.getManualLeaderId() : null);
        return copy;
    }

    private static List<SfStaskTaskScopeBo> flattenTaskScopes(List<SfStaskWorkItemCreateBo> workItems) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<SfStaskTaskScopeBo> scopes = new ArrayList<>();
        for (SfStaskWorkItemCreateBo workItem : workItems) {
            if (workItem == null || workItem.getWorkItemId() == null
                || CollUtil.isEmpty(workItem.getGreenhouseIds())) {
                continue;
            }
            for (Long greenhouseId : workItem.getGreenhouseIds()) {
                if (greenhouseId != null && seen.add(greenhouseId + ":" + workItem.getWorkItemId())) {
                    scopes.add(new SfStaskTaskScopeBo(greenhouseId, workItem.getWorkItemId()));
                }
            }
        }
        return scopes;
    }

    private void enrichTaskScopeConflictNames(String tenantId, List<SfStaskTaskScopeConflictVo> conflicts) {
        Map<Long, SfField> fieldMap = queryGreenhouseMap(tenantId, conflicts.stream()
            .map(SfStaskTaskScopeConflictVo::getGreenhouseId).filter(Objects::nonNull).distinct().toList());
        Map<Long, SfFarmWorkDict> workItemMap = queryWorkItemMap(tenantId, conflicts.stream()
            .map(SfStaskTaskScopeConflictVo::getWorkItemId).filter(Objects::nonNull).distinct().toList());
        for (SfStaskTaskScopeConflictVo conflict : conflicts) {
            if (StringUtils.isBlank(conflict.getGreenhouseNameSnapshot())) {
                SfField field = fieldMap.get(conflict.getGreenhouseId());
                if (field != null) {
                    conflict.setGreenhouseNameSnapshot(field.getFieldName());
                }
            }
            if (StringUtils.isBlank(conflict.getWorkItemNameSnapshot())) {
                SfFarmWorkDict workItem = workItemMap.get(conflict.getWorkItemId());
                if (workItem != null) {
                    conflict.setWorkItemNameSnapshot(workItem.getDictName());
                }
            }
        }
    }

    private String formatTaskScopeConflictMessage(Date planDate, SfStaskTaskScopeConflictVo conflict) {
        String planDateLabel = planDate.toInstant().atZone(CHINA_ZONE).toLocalDate().toString();
        String greenhouseName = StringUtils.isNotBlank(conflict.getGreenhouseNameSnapshot())
            ? conflict.getGreenhouseNameSnapshot() : String.valueOf(conflict.getGreenhouseId());
        String workItemName = StringUtils.isNotBlank(conflict.getWorkItemNameSnapshot())
            ? conflict.getWorkItemNameSnapshot() : String.valueOf(conflict.getWorkItemId());
        return messages.message(
            com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_PACKAGE_DUPLICATE_RUNNING_TASK,
            greenhouseName, planDateLabel, workItemName);
    }

    private Map<Long, SfFarmWorkDict> queryWorkItemMap(String tenantId, Collection<Long> workItemIds) {
        if (CollUtil.isEmpty(workItemIds)) {
            return Map.of();
        }
        return farmWorkDictMapper.selectList(Wrappers.<SfFarmWorkDict>lambdaQuery()
                .eq(SfFarmWorkDict::getTenantId, tenantId)
                .eq(SfFarmWorkDict::getNodeType, FarmWorkNodeType.ITEM)
                .eq(SfFarmWorkDict::getStatus, SystemConstants.NORMAL)
                .in(SfFarmWorkDict::getDictId, workItemIds))
            .stream()
            .collect(Collectors.toMap(SfFarmWorkDict::getDictId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, SfField> queryGreenhouseMap(String tenantId, Collection<Long> greenhouseIds) {
        if (CollUtil.isEmpty(greenhouseIds)) {
            return Map.of();
        }
        return fieldMapper.selectList(Wrappers.<SfField>lambdaQuery()
                .eq(SfField::getTenantId, tenantId)
                .eq(SfField::getFieldType, FieldType.GREENHOUSE)
                .eq(SfField::getDelFlag, SystemConstants.NORMAL)
                .in(SfField::getFieldId, greenhouseIds))
            .stream()
            .collect(Collectors.toMap(SfField::getFieldId, Function.identity(), (a, b) -> a));
    }

    private static List<Long> distinctIds(List<Long> ids, String emptyMessage) {
        if (CollUtil.isEmpty(ids)) {
            throw new ServiceException(emptyMessage);
        }
        List<Long> result = ids.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
        if (CollUtil.isEmpty(result)) {
            throw new ServiceException(emptyMessage);
        }
        return result;
    }

    /**
     * 单条规范化农事项及其最终组长快照。
     *
     * @param workItem  农事项请求
     * @param leaderId  最终组长ID，可为空
     * @param leaderName 最终组长名称，可为空
     */
    public record NormalizedWorkItem(SfStaskWorkItemCreateBo workItem, Long leaderId, String leaderName) {
    }

    private record LeaderSnapshotKey(Long leaderId, String leaderName) {
    }

    private record WorkItemContent(String managerRequirement, String managerPhotos,
        String techInstruction, String techPhotos,
        List<com.ym.agriculture.farmtask.workorder.model.bo.SfStaskTaskMaterialBo> materials) {
        private static WorkItemContent from(SfStaskWorkItemCreateBo item) {
            return new WorkItemContent(item.getManagerRequirement(), item.getManagerPhotos(),
                item.getTechInstruction(), item.getTechPhotos(),
                item.getMaterials() == null ? List.of() : new ArrayList<>(item.getMaterials()));
        }
    }
}
