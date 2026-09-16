package com.ym.agriculture.farmtask.assignment.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farming.field.dao.SfFieldMapper;
import com.ym.agriculture.farming.field.model.constants.FieldType;
import com.ym.agriculture.farming.field.model.entity.SfField;
import com.ym.agriculture.farmtask.i18n.StaskI18nResourceRegistrar;
import com.ym.agriculture.farmtask.assignment.dao.SfFarmWorkAssignmentMapper;
import com.ym.agriculture.farmtask.assignment.model.bo.SfFarmAssignBatchBo;
import com.ym.agriculture.farmtask.assignment.model.bo.SfFarmAssignCancelBatchBo;
import com.ym.agriculture.farmtask.assignment.model.bo.SfFarmAssignPageBo;
import com.ym.agriculture.farmtask.assignment.model.entity.SfFarmWorkAssignment;
import com.ym.agriculture.farmtask.assignment.model.vo.SfFarmAssignDetailVo;
import com.ym.agriculture.farmtask.assignment.model.vo.SfFarmAssignGreenhouseVo;
import com.ym.agriculture.farmtask.assignment.model.vo.SfFarmAssignLeaderGroupVo;
import com.ym.agriculture.farmtask.assignment.model.vo.SfFarmAssignWorkItemVo;
import com.ym.agriculture.farmtask.assignment.service.ISfFarmWorkAssignmentService;
import com.ym.agriculture.farmtask.assignment.support.SfFarmAssignEmployeeAccessor;
import com.ym.agriculture.farmtask.workorder.support.SfStaskWorkOrderAssembler;
import com.ym.agriculture.farming.farmwork.dao.SfFarmWorkDictMapper;
import com.ym.agriculture.farming.farmwork.model.constants.FarmWorkNodeType;
import com.ym.agriculture.farming.farmwork.model.entity.SfFarmWorkDict;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeLeaderOptionVo;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import com.ym.agriculture.farmtask.employee.service.IEmployeeAppRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * stask 农事分配服务实现。
 */
@RequiredArgsConstructor
@Service
public class SfFarmWorkAssignmentServiceImpl implements ISfFarmWorkAssignmentService {

    private final SfFarmWorkAssignmentMapper assignmentMapper;
    private final SfFieldMapper fieldMapper;
    private final SfFarmWorkDictMapper farmWorkDictMapper;
    private final com.ym.agriculture.shared.i18n.StaskMessageResolver messages;
    private final SfFarmAssignEmployeeAccessor employeeAccessor;
    private final IEmployeeAppRoleService employeeAppRoleService;
    @Autowired
    private StaskI18nResourceRegistrar i18nResourceRegistrar;

    @Override
    public PageResult<SfFarmAssignGreenhouseVo> queryGreenhousePage(SfFarmAssignPageBo bo, PageQuery pageQuery) {
        String tenantId = requireTenantId();
        List<SfField> greenhouses = queryGreenhouses(tenantId, bo);
        List<SfFarmWorkDict> enabledItems = queryEnabledWorkItems(tenantId);
        long totalWorkItemCount = enabledItems.size();

        List<Long> greenhouseIds = greenhouses.stream().map(SfField::getFieldId).toList();
        Map<Long, List<SfFarmWorkAssignment>> assignmentsByGreenhouse = assignmentMapper
            .selectByGreenhouseIds(tenantId, greenhouseIds)
            .stream()
            .collect(Collectors.groupingBy(SfFarmWorkAssignment::getGreenhouseId));

        List<SfFarmAssignGreenhouseVo> rows = greenhouses.stream()
            .map(field -> toGreenhouseVo(field, assignmentsByGreenhouse.get(field.getFieldId()), totalWorkItemCount))
            .filter(row -> !Boolean.TRUE.equals(bo.getOnlyUnassigned()) || row.getUnassignedWorkItemCount() > 0)
            .toList();
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(rows, pageQuery.build());
    }

    @Override
    public SfFarmAssignDetailVo queryDetail(Long greenhouseId) {
        String tenantId = requireTenantId();
        SfField greenhouse = requireGreenhouse(tenantId, greenhouseId);
        List<SfFarmWorkDict> enabledItems = queryEnabledWorkItems(tenantId);
        Map<Long, SfFarmWorkDict> itemMap = enabledItems.stream()
            .collect(Collectors.toMap(SfFarmWorkDict::getDictId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Map<Long, String> categoryNameMap = queryCategoryNameMap(tenantId, enabledItems);
        List<SfFarmWorkAssignment> assignments = assignmentMapper.selectByGreenhouseId(tenantId, greenhouseId);
        Set<Long> assignedItemIds = assignments.stream()
            .map(SfFarmWorkAssignment::getWorkItemId)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        SfFarmAssignDetailVo detail = new SfFarmAssignDetailVo();
        detail.setGreenhouseId(greenhouse.getFieldId());
        detail.setGreenhouseCode(greenhouse.getFieldCode());
        detail.setGreenhouseName(greenhouse.getFieldName());
        detail.setTotalWorkItemCount(enabledItems.size());
        detail.setAssignedWorkItemCount(assignedItemIds.size());
        detail.setUnassignedWorkItemCount(Math.max(enabledItems.size() - assignedItemIds.size(), 0));
        detail.setAssignedGroups(buildLeaderGroups(assignments, itemMap, categoryNameMap));
        detail.setUnassignedWorkItems(enabledItems.stream()
            .filter(item -> !assignedItemIds.contains(item.getDictId()))
            .map(item -> toWorkItemVo(item, null, categoryNameMap))
            .toList());
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchAssign(SfFarmAssignBatchBo bo) {
        String tenantId = requireTenantId();
        requireGreenhouse(tenantId, bo.getGreenhouseId());
        validateLeader(bo.getLeaderId());
        List<Long> workItemIds = distinctIds(bo.getWorkItemIds(), messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_ASSIGNMENT_SELECT_WORK_ITEMS));
        Map<Long, SfFarmWorkDict> enabledItemMap = queryEnabledWorkItemMap(tenantId, workItemIds);
        validateEnabledWorkItems(workItemIds, enabledItemMap);
        Map<Long, String> categoryNameMap = queryCategoryNameMap(tenantId, new ArrayList<>(enabledItemMap.values()));
        if (CollUtil.isNotEmpty(assignmentMapper.selectExistingWorkItems(tenantId, bo.getGreenhouseId(), workItemIds))) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_ASSIGNMENT_PARTIALLY_ASSIGNED_REFRESH);
        }

        Date now = new Date();
        Long userId = LoginHelper.getUserId();
        try {
            List<SfFarmWorkAssignment> rows = new ArrayList<>(workItemIds.size());
            for (Long workItemId : workItemIds) {
                SfFarmWorkDict item = enabledItemMap.get(workItemId);
                SfFarmWorkAssignment assignment = new SfFarmWorkAssignment();
                assignment.setAssignmentId(IdWorker.getId());
                assignment.setTenantId(tenantId);
                assignment.setGreenhouseId(bo.getGreenhouseId());
                assignment.setWorkItemId(workItemId);
                assignment.setWorkItemNameSnapshot(item.getDictName());
                assignment.setWorkItemCodeSnapshot(item.getDictCode());
                assignment.setCategoryIdSnapshot(item.getParentId());
                assignment.setCategoryNameSnapshot(categoryNameMap.get(item.getParentId()));
                assignment.setLeaderId(bo.getLeaderId());
                assignment.setAssignedAt(now);
                assignment.setCreateBy(userId);
                assignment.setCreateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now));
                assignment.setUpdateBy(userId);
                assignment.setUpdateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now));
                rows.add(assignment);
            }
            int inserted = assignmentMapper.insertBatch(rows) ? rows.size() : 0;
            if (inserted > 0 && i18nResourceRegistrar != null) {
                i18nResourceRegistrar.registerAssignments(tenantId, rows);
            }
            return inserted;
        } catch (DuplicateKeyException e) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_ASSIGNMENT_PARTIALLY_ASSIGNED_REFRESH);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int remove(Long assignmentId) {
        if (assignmentId == null) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_ASSIGNMENT_RECORD_ID_REQUIRED);
        }
        int rows = assignmentMapper.deleteNormalById(requireTenantId(), assignmentId);
        if (rows <= 0) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_ASSIGNMENT_RECORD_MISSING_OR_CANCELLED);
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchRemove(SfFarmAssignCancelBatchBo bo) {
        String tenantId = requireTenantId();
        requireGreenhouse(tenantId, bo.getGreenhouseId());
        List<Long> assignmentIds = distinctIds(bo.getAssignmentIds(), messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_ASSIGNMENT_SELECT_CANCEL_WORK_ITEMS));
        int rows = assignmentMapper.deleteBatchByScope(tenantId, bo.getGreenhouseId(), bo.getLeaderId(), assignmentIds);
        if (rows != assignmentIds.size()) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_ASSIGNMENT_RECORDS_PARTIALLY_MISSING);
        }
        return rows;
    }

    @Override
    public List<SysEmployeeLeaderOptionVo> queryLeaderOptions(Long greenhouseId, String keyword) {
        String tenantId = requireTenantId();
        requireGreenhouse(tenantId, greenhouseId);
        List<Long> joinedLeaderIds = assignmentMapper.selectByGreenhouseId(tenantId, greenhouseId)
            .stream()
            .map(SfFarmWorkAssignment::getLeaderId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        List<SysEmployeeLeaderOptionVo> leaders = employeeAccessor.queryLeaderOptions(keyword, joinedLeaderIds);
        Map<String, String> roleNames = employeeAppRoleService.getRoleNameMap(leaders.stream()
            .map(SysEmployeeLeaderOptionVo::getAppRoleCode)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .toList());
        roleNames = roleNames == null ? Map.of() : roleNames;
        for (SysEmployeeLeaderOptionVo leader : leaders) {
            leader.setAppRoleName(SfStaskWorkOrderAssembler.localizedRoleName(
                leader.getAppRoleCode(), roleNames.get(leader.getAppRoleCode()), messages));
        }
        return leaders;
    }

    private List<SfField> queryGreenhouses(String tenantId, SfFarmAssignPageBo bo) {
        return fieldMapper.selectList(Wrappers.<SfField>lambdaQuery()
            .eq(SfField::getTenantId, tenantId)
            .eq(SfField::getFieldType, FieldType.GREENHOUSE)
            .eq(SfField::getDelFlag, SystemConstants.NORMAL)
            .like(StringUtils.isNotBlank(bo.getGreenhouseCode()), SfField::getFieldCode, bo.getGreenhouseCode())
            .like(StringUtils.isNotBlank(bo.getGreenhouseName()), SfField::getFieldName, bo.getGreenhouseName())
            .orderByDesc(SfField::getFieldId));
    }

    private SfField requireGreenhouse(String tenantId, Long greenhouseId) {
        if (greenhouseId == null) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_ASSIGNMENT_GREENHOUSE_ID_REQUIRED);
        }
        SfField field = fieldMapper.selectOne(Wrappers.<SfField>lambdaQuery()
            .eq(SfField::getTenantId, tenantId)
            .eq(SfField::getFieldId, greenhouseId)
            .eq(SfField::getFieldType, FieldType.GREENHOUSE)
            .eq(SfField::getDelFlag, SystemConstants.NORMAL));
        if (field == null) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_GREENHOUSE_NOT_FOUND);
        }
        return field;
    }

    private List<SfFarmWorkDict> queryEnabledWorkItems(String tenantId) {
        return farmWorkDictMapper.selectList(Wrappers.<SfFarmWorkDict>lambdaQuery()
            .eq(SfFarmWorkDict::getTenantId, tenantId)
            .eq(SfFarmWorkDict::getNodeType, FarmWorkNodeType.ITEM.name())
            .eq(SfFarmWorkDict::getStatus, SystemConstants.NORMAL)
            .eq(SfFarmWorkDict::getDelFlag, SystemConstants.NORMAL)
            .orderByAsc(SfFarmWorkDict::getParentId)
            .orderByAsc(SfFarmWorkDict::getSortOrder)
            .orderByAsc(SfFarmWorkDict::getDictId));
    }

    private Map<Long, String> queryCategoryNameMap(String tenantId, List<SfFarmWorkDict> items) {
        List<Long> categoryIds = items.stream()
            .map(SfFarmWorkDict::getParentId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (CollUtil.isEmpty(categoryIds)) {
            return Map.of();
        }
        return farmWorkDictMapper.selectNormalByIds(tenantId, categoryIds)
            .stream()
            .collect(Collectors.toMap(SfFarmWorkDict::getDictId, SfFarmWorkDict::getDictName, (a, b) -> a));
    }

    private Map<Long, SfFarmWorkDict> queryEnabledWorkItemMap(String tenantId, Collection<Long> workItemIds) {
        if (CollUtil.isEmpty(workItemIds)) {
            return Map.of();
        }
        return farmWorkDictMapper.selectList(Wrappers.<SfFarmWorkDict>lambdaQuery()
                .eq(SfFarmWorkDict::getTenantId, tenantId)
                .eq(SfFarmWorkDict::getNodeType, FarmWorkNodeType.ITEM.name())
                .eq(SfFarmWorkDict::getStatus, SystemConstants.NORMAL)
                .eq(SfFarmWorkDict::getDelFlag, SystemConstants.NORMAL)
                .in(SfFarmWorkDict::getDictId, workItemIds))
            .stream()
            .collect(Collectors.toMap(SfFarmWorkDict::getDictId, Function.identity(), (a, b) -> a));
    }

    private SfFarmAssignGreenhouseVo toGreenhouseVo(SfField field, List<SfFarmWorkAssignment> assignments, long totalWorkItemCount) {
        List<SfFarmWorkAssignment> rows = assignments == null ? List.of() : assignments;
        long assignedCount = rows.stream().map(SfFarmWorkAssignment::getWorkItemId).distinct().count();
        SfFarmAssignGreenhouseVo vo = new SfFarmAssignGreenhouseVo();
        vo.setGreenhouseId(field.getFieldId());
        vo.setGreenhouseCode(field.getFieldCode());
        vo.setGreenhouseName(field.getFieldName());
        vo.setAssignedWorkItemCount(assignedCount);
        vo.setUnassignedWorkItemCount(Math.max(totalWorkItemCount - assignedCount, 0));
        vo.setLeaderCount(rows.stream().map(SfFarmWorkAssignment::getLeaderId).distinct().count());
        return vo;
    }

    private List<SfFarmAssignLeaderGroupVo> buildLeaderGroups(List<SfFarmWorkAssignment> assignments,
        Map<Long, SfFarmWorkDict> itemMap, Map<Long, String> categoryNameMap) {
        if (CollUtil.isEmpty(assignments)) {
            return List.of();
        }
        Map<Long, List<SfFarmWorkAssignment>> assignmentsByLeader = assignments.stream()
            .collect(Collectors.groupingBy(SfFarmWorkAssignment::getLeaderId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, SysEmployeeVo> leaderMap = employeeAccessor.queryEmployeesByIds(assignmentsByLeader.keySet())
            .stream()
            .filter(Objects::nonNull)
            .filter(leader -> leader.getEmployeeId() != null)
            .collect(Collectors.toMap(SysEmployeeVo::getEmployeeId, Function.identity(), (a, b) -> a));
        List<SfFarmAssignLeaderGroupVo> groups = new ArrayList<>();
        for (Map.Entry<Long, List<SfFarmWorkAssignment>> entry : assignmentsByLeader.entrySet()) {
            Long leaderId = entry.getKey();
            SysEmployeeVo leader = leaderMap.get(leaderId);
            SfFarmAssignLeaderGroupVo group = new SfFarmAssignLeaderGroupVo();
            group.setLeaderId(leaderId);
            group.setLeaderName(leader == null || StringUtils.isBlank(leader.getName())
                ? messages.message(
                    com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_ASSIGNMENT_EMPLOYEE_ID, leaderId)
                : leader.getName());
            group.setLeaderPhone(leader == null ? null : leader.getPhone());
            group.setWorkItems(entry.getValue().stream()
                .sorted(Comparator.comparing(SfFarmWorkAssignment::getAssignedAt, Comparator.nullsLast(Date::compareTo)))
                .map(assignment -> toAssignedWorkItemVo(assignment, itemMap, categoryNameMap))
                .toList());
            groups.add(group);
        }
        return groups;
    }

    private SfFarmAssignWorkItemVo toAssignedWorkItemVo(SfFarmWorkAssignment assignment,
        Map<Long, SfFarmWorkDict> itemMap, Map<Long, String> categoryNameMap) {
        SfFarmWorkDict item = itemMap.get(assignment.getWorkItemId());
        if (item == null) {
            if (hasWorkItemSnapshot(assignment)) {
                return toSnapshotWorkItemVo(assignment);
            }
            SfFarmAssignWorkItemVo vo = new SfFarmAssignWorkItemVo();
            vo.setAssignmentId(assignment.getAssignmentId());
            vo.setWorkItemId(assignment.getWorkItemId());
            vo.setWorkItemName(messages.message(
                com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_ASSIGNMENT_WORK_ITEM_ID,
                assignment.getWorkItemId()));
            return vo;
        }
        return toWorkItemVo(item, assignment.getAssignmentId(), categoryNameMap);
    }

    private static boolean hasWorkItemSnapshot(SfFarmWorkAssignment assignment) {
        return assignment != null && StringUtils.isNotBlank(assignment.getWorkItemNameSnapshot());
    }

    private static SfFarmAssignWorkItemVo toSnapshotWorkItemVo(SfFarmWorkAssignment assignment) {
        SfFarmAssignWorkItemVo vo = new SfFarmAssignWorkItemVo();
        vo.setAssignmentId(assignment.getAssignmentId());
        vo.setWorkItemId(assignment.getWorkItemId());
        vo.setWorkItemName(assignment.getWorkItemNameSnapshot());
        vo.setWorkItemCode(assignment.getWorkItemCodeSnapshot());
        vo.setCategoryId(assignment.getCategoryIdSnapshot());
        vo.setCategoryName(assignment.getCategoryNameSnapshot());
        return vo;
    }

    private SfFarmAssignWorkItemVo toWorkItemVo(SfFarmWorkDict item, Long assignmentId, Map<Long, String> categoryNameMap) {
        SfFarmAssignWorkItemVo vo = new SfFarmAssignWorkItemVo();
        vo.setAssignmentId(assignmentId);
        vo.setWorkItemId(item.getDictId());
        vo.setWorkItemName(item.getDictName());
        vo.setWorkItemCode(item.getDictCode());
        vo.setCategoryId(item.getParentId());
        vo.setCategoryName(categoryNameMap.get(item.getParentId()));
        return vo;
    }

    private void validateEnabledWorkItems(List<Long> workItemIds, Map<Long, SfFarmWorkDict> enabledItemMap) {
        boolean allEnabled = workItemIds.stream().allMatch(enabledItemMap::containsKey);
        if (!allEnabled) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_WORK_ITEM_UNAVAILABLE);
        }
    }

    private void validateLeader(Long leaderId) {
        if (leaderId == null) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_ASSIGNMENT_LEADER_ID_REQUIRED);
        }
        SysEmployeeVo leader = employeeAccessor.queryEmployeeById(leaderId);
        if (ObjectUtil.isNull(leader)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_ASSIGNMENT_LEADER_NOT_FOUND);
        }
        if (!SystemConstants.NORMAL.equals(leader.getStatus())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_ASSIGNMENT_LEADER_DISABLED);
        }
        if (!EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER.equals(leader.getAppRoleCode())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_ASSIGNMENT_LEADER_ROLE_REQUIRED);
        }
        if (StringUtils.isNotBlank(leader.getReviewStatus())
            && !EmployeeConstants.REVIEW_APPROVED.equals(leader.getReviewStatus())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_ASSIGNMENT_LEADER_NOT_APPROVED);
        }
    }

    private static List<Long> distinctIds(List<Long> ids, String emptyMessage) {
        if (CollUtil.isEmpty(ids)) {
            throw new ServiceException(emptyMessage);
        }
        List<Long> distinctIds = ids.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
        if (CollUtil.isEmpty(distinctIds)) {
            throw new ServiceException(emptyMessage);
        }
        return distinctIds;
    }

    private String requireTenantId() {
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_COMMON_TENANT_CONTEXT_MISSING);
        }
        return tenantId;
    }
}
