package com.ym.agriculture.farmtask.worker.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farming.field.dao.SfFieldMapper;
import com.ym.agriculture.farming.field.model.entity.SfField;
import com.ym.agriculture.farmtask.assignment.dao.SfFarmWorkAssignmentMapper;
import com.ym.agriculture.farmtask.assignment.model.entity.SfFarmWorkAssignment;
import com.ym.agriculture.farmtask.worker.model.bo.SfStaskWorkerListQueryBo;
import com.ym.agriculture.farmtask.worker.model.constants.StaskWorkerSkillLevel;
import com.ym.agriculture.farmtask.worker.model.vo.SfStaskLeaderManagedFarmWorkVo;
import com.ym.agriculture.farmtask.worker.model.vo.SfStaskManagedGreenhouseVo;
import com.ym.agriculture.farmtask.worker.model.vo.SfStaskWorkerDetailVo;
import com.ym.agriculture.farmtask.worker.model.vo.SfStaskWorkerListItemVo;
import com.ym.agriculture.farmtask.worker.model.vo.SfStaskWorkerRecentTaskVo;
import com.ym.agriculture.farmtask.worker.model.vo.SfStaskWorkerSkillDetailVo;
import com.ym.agriculture.farmtask.worker.model.vo.SfStaskWorkerSkillTagVo;
import com.ym.agriculture.farmtask.worker.model.vo.SfStaskWorkerSkillVo;
import com.ym.agriculture.farmtask.worker.service.ISfStaskWorkerDirectoryService;
import com.ym.agriculture.farmtask.worker.service.ISfStaskWorkerSkillService;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskAcceptanceMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskDispatchMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderMapper;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskAcceptance;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskDispatch;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeAccessor;
import com.ym.agriculture.farmtask.workorder.support.SfStaskWorkOrderAssembler;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;
import com.ym.agriculture.farmtask.employee.model.bo.SysEmployeeBo;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * stask 小程序人员目录服务实现。
 */
@Service
@RequiredArgsConstructor
public class SfStaskWorkerDirectoryServiceImpl implements ISfStaskWorkerDirectoryService {

    private final com.ym.agriculture.shared.i18n.StaskMessageResolver messages;
    private final SfStaskEmployeeAccessor employeeAccessor;
    private final ISfStaskWorkerSkillService workerSkillService;
    private final SfStaskDispatchMapper dispatchMapper;
    private final SfStaskWorkOrderMapper workOrderMapper;
    private final SfStaskAcceptanceMapper acceptanceMapper;
    private final SfFarmWorkAssignmentMapper assignmentMapper;
    private final SfFieldMapper fieldMapper;

    @Override
    public PageResult<SfStaskWorkerListItemVo> managerWorkers(SfStaskWorkerListQueryBo bo, PageQuery pageQuery) {
        return managerWorkers(bo, pageQuery, requireTenantId());
    }

    @Override
    public PageResult<SfStaskWorkerListItemVo> leaderWorkers(SfStaskWorkerListQueryBo bo, PageQuery pageQuery) {
        return leaderWorkers(bo, pageQuery, requireTenantId());
    }

    @Override
    public SfStaskWorkerDetailVo managerWorkerDetail(Long employeeId) {
        return managerWorkerDetail(employeeId, requireTenantId());
    }

    @Override
    public SfStaskWorkerDetailVo leaderWorkerDetail(Long employeeId) {
        return leaderWorkerDetail(employeeId, requireTenantId());
    }

    /**
     * 生产管理员视角分页查询人员列表。
     */
    public PageResult<SfStaskWorkerListItemVo> managerWorkers(SfStaskWorkerListQueryBo bo, PageQuery pageQuery,
        String tenantId) {
        SfStaskWorkerListQueryBo queryBo = bo == null ? new SfStaskWorkerListQueryBo() : bo;
        SysEmployeeBo employeeBo = new SysEmployeeBo();
        employeeBo.setKeyword(StrUtil.trim(queryBo.getKeyword()));
        employeeBo.setAppRoleCode(StrUtil.trim(queryBo.getAppRoleCode()));
        employeeBo.setStatus(StrUtil.trim(queryBo.getStatus()));
        return buildWorkerPage(employeeAccessor.queryEmployeePage(employeeBo, pageQuery), tenantId);
    }

    /**
     * 组长视角分页查询在职工人列表。
     */
    public PageResult<SfStaskWorkerListItemVo> leaderWorkers(SfStaskWorkerListQueryBo bo, PageQuery pageQuery,
        String tenantId) {
        SfStaskWorkerListQueryBo queryBo = bo == null ? new SfStaskWorkerListQueryBo() : bo;
        SysEmployeeBo employeeBo = new SysEmployeeBo();
        employeeBo.setKeyword(StrUtil.trim(queryBo.getKeyword()));
        employeeBo.setAppRoleCode(EmployeeConstants.APP_ROLE_STASK_WORKER);
        employeeBo.setStatus(SystemConstants.NORMAL);
        return buildWorkerPage(employeeAccessor.queryEmployeePage(employeeBo, pageQuery), tenantId);
    }

    /**
     * 生产管理员视角查询人员详情。
     */
    public SfStaskWorkerDetailVo managerWorkerDetail(Long employeeId, String tenantId) {
        SysEmployeeVo employee = loadEmployee(employeeId);
        return buildDetail(employee, tenantId);
    }

    /**
     * 组长视角查询在职工人详情。
     */
    public SfStaskWorkerDetailVo leaderWorkerDetail(Long employeeId, String tenantId) {
        SysEmployeeVo employee = loadEmployee(employeeId);
        if (!EmployeeConstants.APP_ROLE_STASK_WORKER.equals(employee.getAppRoleCode())
            || !SystemConstants.NORMAL.equals(employee.getStatus())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_WORKER_ACTIVE_DETAIL_ONLY);
        }
        return buildDetail(employee, tenantId);
    }

    private PageResult<SfStaskWorkerListItemVo> buildWorkerPage(PageResult<SysEmployeeVo> employeePage,
        String tenantId) {
        List<SysEmployeeVo> employees = employeePage.getRows() == null
            ? List.of() : new ArrayList<>(employeePage.getRows());
        List<Long> employeeIds = employees.stream()
            .map(SysEmployeeVo::getEmployeeId)
            .filter(id -> id != null)
            .toList();
        Map<Long, List<SfStaskWorkerSkillVo>> skillMap = workerSkillService.queryByEmployeeIds(tenantId, employeeIds);
        Set<Long> unfinishedWorkerIds = new LinkedHashSet<>(dispatchMapper.selectUnfinishedWorkerIds(tenantId, employeeIds));
        List<SfStaskWorkerListItemVo> rows = employees.stream()
            .map(employee -> toItemVo(employee, skillMap.get(employee.getEmployeeId()), unfinishedWorkerIds))
            .toList();
        PageResult<SfStaskWorkerListItemVo> result = new PageResult<>();
        result.setTotal(employeePage.getTotal());
        result.setRows(rows);
        return result;
    }

    private SysEmployeeVo loadEmployee(Long employeeId) {
        if (employeeId == null) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_ASSIGNMENT_EMPLOYEE_ID_REQUIRED);
        }
        List<SysEmployeeVo> employees = employeeAccessor.queryByIds(List.of(employeeId));
        if (CollUtil.isEmpty(employees)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_ASSIGNMENT_EMPLOYEE_NOT_FOUND);
        }
        return employees.get(0);
    }

    private SfStaskWorkerDetailVo buildDetail(SysEmployeeVo employee, String tenantId) {
        List<SfStaskWorkerSkillVo> skills = workerSkillService.queryByEmployeeIds(tenantId, List.of(employee.getEmployeeId()))
            .getOrDefault(employee.getEmployeeId(), List.of());
        SfStaskWorkerDetailVo detail = toDetailVo(employee, skills);
        if (EmployeeConstants.APP_ROLE_STASK_WORKER.equals(employee.getAppRoleCode())) {
            detail.setRecentTasks(buildRecentTasks(tenantId, employee.getEmployeeId()));
        }
        if (EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER.equals(employee.getAppRoleCode())) {
            detail.setManagedFarmWorks(buildManagedFarmWorks(tenantId, employee.getEmployeeId()));
        }
        return detail;
    }

    private SfStaskWorkerDetailVo toDetailVo(SysEmployeeVo employee, List<SfStaskWorkerSkillVo> skills) {
        SfStaskWorkerDetailVo vo = new SfStaskWorkerDetailVo();
        vo.setEmployeeId(employee.getEmployeeId());
        vo.setEmployeeName(employee.getName());
        vo.setName(employee.getName());
        vo.setPhone(employee.getPhone());
        vo.setPhoto(employee.getPhoto());
        vo.setGender(employee.getGender());
        vo.setGenderName(genderName(employee.getGender()));
        vo.setBirthDate(employee.getBirthDate());
        vo.setAge(employee.getBirthDate() == null ? null : DateUtil.ageOfNow(employee.getBirthDate()));
        vo.setPersonType(employee.getPersonType());
        vo.setPersonTypeName(personTypeName(employee.getPersonType()));
        vo.setAppRoleCode(employee.getAppRoleCode());
        vo.setAppRoleName(localizedRoleName(employee));
        vo.setStatus(employee.getStatus());
        vo.setStatusName(statusName(employee.getStatus()));
        vo.setSkills(toSkillDetails(skills));
        vo.setRecentTasks(List.of());
        vo.setManagedFarmWorks(List.of());
        return vo;
    }

    private SfStaskWorkerListItemVo toItemVo(SysEmployeeVo employee, List<SfStaskWorkerSkillVo> skills,
        Set<Long> unfinishedWorkerIds) {
        SfStaskWorkerListItemVo vo = new SfStaskWorkerListItemVo();
        vo.setEmployeeId(employee.getEmployeeId());
        vo.setEmployeeName(employee.getName());
        vo.setName(employee.getName());
        vo.setPhone(employee.getPhone());
        vo.setPhoto(employee.getPhoto());
        vo.setGender(employee.getGender());
        vo.setGenderName(genderName(employee.getGender()));
        vo.setBirthDate(employee.getBirthDate());
        vo.setAge(employee.getBirthDate() == null ? null : DateUtil.ageOfNow(employee.getBirthDate()));
        vo.setPersonType(employee.getPersonType());
        vo.setPersonTypeName(personTypeName(employee.getPersonType()));
        vo.setAppRoleCode(employee.getAppRoleCode());
        vo.setAppRoleName(localizedRoleName(employee));
        vo.setStatus(employee.getStatus());
        vo.setStatusName(statusName(employee.getStatus()));
        vo.setHasUnfinishedTask(unfinishedWorkerIds.contains(employee.getEmployeeId()));
        vo.setSkills(toSkillTags(skills));
        vo.setTags(buildTags(vo));
        return vo;
    }

    private List<SfStaskWorkerSkillTagVo> toSkillTags(List<SfStaskWorkerSkillVo> skills) {
        if (CollUtil.isEmpty(skills)) {
            return List.of();
        }
        return skills.stream().map(skill -> {
            SfStaskWorkerSkillTagVo tag = new SfStaskWorkerSkillTagVo();
            tag.setWorkItemId(skill.getWorkItemId());
            tag.setWorkItemName(skill.getWorkItemName());
            tag.setCropType(skill.getCropType());
            tag.setCropTypeName(skill.getCropTypeName());
            tag.setSkillLevel(skill.getSkillLevel());
            tag.setSkillLevelName(skillLevelName(skill.getSkillLevel()));
            tag.setWorkCount(skill.getWorkCount());
            return tag;
        }).toList();
    }

    private String localizedRoleName(SysEmployeeVo employee) {
        return SfStaskWorkOrderAssembler.localizedRoleName(
            employee.getAppRoleCode(), employee.getAppRoleName(), messages);
    }

    private List<SfStaskWorkerSkillDetailVo> toSkillDetails(List<SfStaskWorkerSkillVo> skills) {
        if (CollUtil.isEmpty(skills)) {
            return List.of();
        }
        return skills.stream().map(skill -> {
            SfStaskWorkerSkillDetailVo detail = new SfStaskWorkerSkillDetailVo();
            detail.setWorkItemId(skill.getWorkItemId());
            detail.setWorkItemName(skill.getWorkItemName());
            detail.setCropType(skill.getCropType());
            detail.setCropTypeName(skill.getCropTypeName());
            detail.setSkillLevel(skill.getSkillLevel());
            detail.setSkillLevelName(skillLevelName(skill.getSkillLevel()));
            detail.setWorkCount(skill.getWorkCount());
            detail.setAverageScore(skill.getAverageScore());
            detail.setLastWorkDate(skill.getLastWorkDate());
            return detail;
        }).toList();
    }

    private List<SfStaskWorkerRecentTaskVo> buildRecentTasks(String tenantId, Long workerId) {
        List<SfStaskDispatch> dispatches = dispatchMapper.selectAcceptedByWorkerId(tenantId, workerId);
        if (CollUtil.isEmpty(dispatches)) {
            return List.of();
        }
        List<Long> orderIds = dispatches.stream()
            .map(SfStaskDispatch::getOrderId)
            .filter(id -> id != null)
            .distinct()
            .toList();
        Map<Long, SfStaskWorkOrder> orderMap = workOrderMapper.selectByIds(tenantId, orderIds).stream()
            .filter(order -> StaskOrderStatus.ACCEPTANCE_PASSED.equals(order.getStatus()))
            .collect(Collectors.toMap(SfStaskWorkOrder::getOrderId, Function.identity(), (left, right) -> left));
        if (orderMap.isEmpty()) {
            return List.of();
        }
        List<Long> acceptedOrderIds = new ArrayList<>(orderMap.keySet());
        Map<Long, SfStaskAcceptance> acceptanceMap = acceptanceMapper.selectByOrderIds(tenantId, acceptedOrderIds)
            .stream()
            .collect(Collectors.toMap(SfStaskAcceptance::getOrderId, Function.identity(), (left, right) -> left));
        Set<Long> leaderIds = dispatches.stream()
            .map(dispatch -> resolveLeaderId(dispatch, orderMap.get(dispatch.getOrderId())))
            .filter(id -> id != null)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, SysEmployeeVo> leaderMap = leaderIds.isEmpty() ? Map.of() : employeeAccessor.queryByIds(List.copyOf(leaderIds))
            .stream()
            .collect(Collectors.toMap(SysEmployeeVo::getEmployeeId, Function.identity(), (left, right) -> left));
        return dispatches.stream()
            .map(dispatch -> toRecentTask(dispatch, orderMap.get(dispatch.getOrderId()), acceptanceMap, leaderMap))
            .filter(task -> task != null)
            .sorted(Comparator.comparing(SfStaskWorkerRecentTaskVo::getCompletedAt,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(5)
            .toList();
    }

    private SfStaskWorkerRecentTaskVo toRecentTask(SfStaskDispatch dispatch, SfStaskWorkOrder order,
        Map<Long, SfStaskAcceptance> acceptanceMap, Map<Long, SysEmployeeVo> leaderMap) {
        if (order == null) {
            return null;
        }
        SfStaskAcceptance acceptance = acceptanceMap.get(order.getOrderId());
        if (acceptance == null) {
            return null;
        }
        Long leaderId = resolveLeaderId(dispatch, order);
        SysEmployeeVo leader = leaderId == null ? null : leaderMap.get(leaderId);
        SfStaskWorkerRecentTaskVo task = new SfStaskWorkerRecentTaskVo();
        task.setDispatchId(dispatch.getDispatchId());
        task.setOrderId(order.getOrderId());
        task.setOrderNo(order.getOrderNo());
        task.setGreenhouseId(order.getGreenhouseId());
        task.setGreenhouseName(order.getGreenhouseNameSnapshot());
        task.setWorkItemId(order.getWorkItemId());
        task.setWorkItemName(order.getWorkItemNameSnapshot());
        task.setPlanDate(order.getPlanDate());
        task.setCompletedAt(acceptance.getAcceptedAt());
        task.setLeaderId(leaderId);
        task.setLeaderName(leader == null ? null : leader.getName());
        task.setLeaderEvaluation(dispatch.getLeaderEvaluation());
        task.setLeaderEvaluationLabel(leaderEvaluationLabel(dispatch.getLeaderEvaluation()));
        return task;
    }

    private List<SfStaskLeaderManagedFarmWorkVo> buildManagedFarmWorks(String tenantId, Long leaderId) {
        List<SfFarmWorkAssignment> assignments = assignmentMapper.selectByLeaderId(tenantId, leaderId);
        if (CollUtil.isEmpty(assignments)) {
            return List.of();
        }
        List<Long> greenhouseIds = assignments.stream()
            .map(SfFarmWorkAssignment::getGreenhouseId)
            .filter(id -> id != null)
            .distinct()
            .toList();
        Map<Long, SfField> fieldMap = fieldMapper.selectNormalEntitiesByFieldIds(greenhouseIds).stream()
            .collect(Collectors.toMap(SfField::getFieldId, Function.identity(), (left, right) -> left));
        Map<String, SfStaskLeaderManagedFarmWorkVo> groupMap = new LinkedHashMap<>();
        for (SfFarmWorkAssignment assignment : assignments) {
            String key = assignment.getWorkItemId() + ":" + assignment.getCategoryIdSnapshot();
            SfStaskLeaderManagedFarmWorkVo farmWork = groupMap.computeIfAbsent(key, ignored -> {
                SfStaskLeaderManagedFarmWorkVo vo = new SfStaskLeaderManagedFarmWorkVo();
                vo.setWorkItemId(assignment.getWorkItemId());
                vo.setWorkItemName(assignment.getWorkItemNameSnapshot());
                vo.setCategoryId(assignment.getCategoryIdSnapshot());
                vo.setCategoryName(assignment.getCategoryNameSnapshot());
                vo.setGreenhouses(new ArrayList<>());
                return vo;
            });
            farmWork.getGreenhouses().add(toManagedGreenhouse(assignment, fieldMap));
        }
        return new ArrayList<>(groupMap.values());
    }

    private SfStaskManagedGreenhouseVo toManagedGreenhouse(SfFarmWorkAssignment assignment, Map<Long, SfField> fieldMap) {
        SfField field = fieldMap.get(assignment.getGreenhouseId());
        SfStaskManagedGreenhouseVo greenhouse = new SfStaskManagedGreenhouseVo();
        greenhouse.setGreenhouseId(assignment.getGreenhouseId());
        greenhouse.setGreenhouseName(field == null ? null : field.getFieldName());
        greenhouse.setGreenhouseCode(field == null ? null : field.getFieldCode());
        return greenhouse;
    }

    private List<String> buildTags(SfStaskWorkerListItemVo vo) {
        List<String> tags = new ArrayList<>();
        if (StrUtil.isNotBlank(vo.getAppRoleName())) {
            tags.add(vo.getAppRoleName());
        }
        if (StrUtil.isNotBlank(vo.getStatusName())) {
            tags.add(vo.getStatusName());
        }
        if (Boolean.TRUE.equals(vo.getHasUnfinishedTask())) {
            tags.add(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKER_HAS_UNFINISHED_TASK));
        }
        for (SfStaskWorkerSkillTagVo skill : vo.getSkills()) {
            if (StrUtil.isNotBlank(skill.getWorkItemName())) {
                tags.add(skill.getWorkItemName());
            }
        }
        return tags;
    }

    private String genderName(String gender) {
        if ("0".equals(gender)) {
            return messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_COMMON_GENDER_MALE);
        }
        if ("1".equals(gender)) {
            return messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_COMMON_GENDER_FEMALE);
        }
        return messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_COMMON_UNKNOWN);
    }

    private String personTypeName(String personType) {
        if (EmployeeConstants.PERSON_TYPE_INTERNAL.equals(personType)) {
            return messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKER_INTERNAL);
        }
        if (EmployeeConstants.PERSON_TYPE_EXTERNAL.equals(personType)) {
            return messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKER_EXTERNAL);
        }
        return null;
    }

    private String statusName(String status) {
        if (SystemConstants.NORMAL.equals(status)) {
            return messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKER_EMPLOYED);
        }
        if (SystemConstants.DISABLE.equals(status)) {
            return messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKER_DEPARTED);
        }
        return null;
    }

    private String skillLevelName(String skillLevel) {
        if (StaskWorkerSkillLevel.ADVANCED.equals(skillLevel)) {
            return messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKER_SKILL_SENIOR);
        }
        if (StaskWorkerSkillLevel.MEDIUM.equals(skillLevel)) {
            return messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKER_SKILL_INTERMEDIATE);
        }
        if (StaskWorkerSkillLevel.JUNIOR.equals(skillLevel)) {
            return messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKER_SKILL_JUNIOR);
        }
        return null;
    }

    private static Long resolveLeaderId(SfStaskDispatch dispatch, SfStaskWorkOrder order) {
        if (dispatch != null && dispatch.getLeaderId() != null) {
            return dispatch.getLeaderId();
        }
        return order == null ? null : order.getLeaderId();
    }

    private String leaderEvaluationLabel(String leaderEvaluation) {
        if (StrUtil.isBlank(leaderEvaluation)) {
            return messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKER_EVALUATION_PENDING);
        }
        return switch (leaderEvaluation) {
            case "EXCELLENT" -> messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKER_EVALUATION_EXCELLENT);
            case "QUALIFIED" -> messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKER_EVALUATION_QUALIFIED);
            case "REWORK" -> messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKER_EVALUATION_REWORK);
            default -> leaderEvaluation;
        };
    }

    private String requireTenantId() {
        String tenantId = TenantHelper.getTenantId();
        if (StrUtil.isBlank(tenantId)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_COMMON_TENANT_CONTEXT_MISSING);
        }
        return tenantId;
    }
}
