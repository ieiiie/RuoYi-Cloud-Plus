package com.ym.agriculture.farmtask.worker.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farming.crop.dao.SfCropSpeciesMapper;
import com.ym.agriculture.farming.farmwork.dao.SfFarmWorkDictMapper;
import com.ym.agriculture.farming.farmwork.model.constants.FarmWorkNodeType;
import com.ym.agriculture.farming.farmwork.model.entity.SfFarmWorkDict;
import com.ym.agriculture.farmtask.worker.dao.SfStaskWorkerSkillMapper;
import com.ym.agriculture.farmtask.worker.model.bo.SfStaskWorkerSkillBatchBo;
import com.ym.agriculture.farmtask.worker.model.bo.SfStaskWorkerSkillBo;
import com.ym.agriculture.farmtask.worker.model.constants.StaskWorkerSkillLevel;
import com.ym.agriculture.farmtask.worker.model.entity.SfStaskWorkerSkill;
import com.ym.agriculture.farmtask.worker.model.vo.SfStaskWorkerSkillVo;
import com.ym.agriculture.farmtask.worker.service.ISfStaskWorkerSkillService;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskDispatchMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderMapper;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskDispatch;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeAccessor;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * stask 工人农事技能服务实现。
 */
@RequiredArgsConstructor
@Service
public class SfStaskWorkerSkillServiceImpl implements ISfStaskWorkerSkillService {

    private static final String DEFAULT_CROP_TYPE = "ALL";

    private final com.ym.agriculture.shared.i18n.StaskMessageResolver messages;
    private final SfStaskWorkerSkillMapper workerSkillMapper;
    private final SfStaskDispatchMapper dispatchMapper;
    private final SfStaskWorkOrderMapper workOrderMapper;
    private final SfFarmWorkDictMapper farmWorkDictMapper;
    private final SfCropSpeciesMapper cropSpeciesMapper;
    private final SfStaskEmployeeAccessor employeeAccessor;

    @Override
    public List<SfStaskWorkerSkillVo> queryByEmployeeId(Long employeeId) {
        String tenantId = requireTenantId();
        List<SfStaskWorkerSkill> rows = workerSkillMapper.selectByEmployeeId(tenantId, employeeId);
        List<SfStaskWorkerSkillVo> vos = MapstructUtils.convert(rows, SfStaskWorkerSkillVo.class);
        fillWorkItemNames(tenantId, vos);
        fillCropTypeNames(tenantId, vos);
        fillEmployeeName(employeeId, vos);
        fillCompletedWorkCounts(tenantId, employeeId, vos);
        return vos;
    }

    @Override
    public Map<Long, List<SfStaskWorkerSkillVo>> queryByEmployeeIds(String tenantId, Collection<Long> employeeIds) {
        if (StrUtil.isBlank(tenantId) || CollUtil.isEmpty(employeeIds)) {
            return Map.of();
        }
        List<Long> distinctEmployeeIds = employeeIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (CollUtil.isEmpty(distinctEmployeeIds)) {
            return Map.of();
        }
        List<SfStaskWorkerSkillVo> vos = MapstructUtils.convert(
            workerSkillMapper.selectByEmployeeIds(tenantId, distinctEmployeeIds), SfStaskWorkerSkillVo.class);
        fillWorkItemNames(tenantId, vos);
        fillCropTypeNames(tenantId, vos);
        Map<Long, List<SfStaskWorkerSkillVo>> grouped = vos.stream()
            .filter(vo -> vo.getEmployeeId() != null)
            .collect(Collectors.groupingBy(SfStaskWorkerSkillVo::getEmployeeId));
        fillCompletedWorkCounts(tenantId, grouped);
        return grouped;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveEmployeeSkills(Long employeeId, SfStaskWorkerSkillBatchBo bo) {
        if (bo == null) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_WORKER_SKILL_LIST_REQUIRED);
        }
        String tenantId = requireTenantId();
        ensureWorker(employeeId);
        if (CollUtil.isEmpty(bo.getSkills())) {
            return workerSkillMapper.deleteByEmployeeId(tenantId, employeeId);
        }
        validateUniqueSkills(bo.getSkills());
        Map<Long, SfFarmWorkDict> workItemMap = queryWorkItemMap(tenantId, bo.getSkills().stream()
            .map(SfStaskWorkerSkillBo::getWorkItemId)
            .filter(Objects::nonNull)
            .toList());
        Map<String, SfStaskWorkerSkill> oldSkillMap = workerSkillMapper.selectByEmployeeId(tenantId, employeeId)
            .stream()
            .collect(Collectors.toMap(this::skillKey, Function.identity(), (a, b) -> a, HashMap::new));
        workerSkillMapper.deleteByEmployeeId(tenantId, employeeId);

        Date now = new Date();
        Long operatorId = LoginHelper.getUserId();
        Set<String> normalCropTypes = loadNormalCropTypes(tenantId, bo.getSkills());
        List<SfStaskWorkerSkill> rows = new java.util.ArrayList<>(bo.getSkills().size());
        for (SfStaskWorkerSkillBo skillBo : bo.getSkills()) {
            if (!workItemMap.containsKey(skillBo.getWorkItemId())) {
                throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_WORKER_WORK_ITEM_UNAVAILABLE);
            }
            validateSkillLevel(skillBo.getSkillLevel());
            String cropType = normalizeCropType(skillBo.getCropType(), normalCropTypes);
            SfStaskWorkerSkill row = new SfStaskWorkerSkill();
            row.setSkillId(IdWorker.getId());
            row.setTenantId(tenantId);
            row.setEmployeeId(employeeId);
            row.setWorkItemId(skillBo.getWorkItemId());
            row.setCropType(cropType);
            row.setSkillLevel(skillBo.getSkillLevel());
            SfStaskWorkerSkill oldSkill = oldSkillMap.get(skillKey(row));
            row.setWorkCount(oldSkill == null ? 0 : oldSkill.getWorkCount());
            row.setLastWorkDate(oldSkill == null ? null : oldSkill.getLastWorkDate());
            row.setAverageScore(oldSkill == null ? null : oldSkill.getAverageScore());
            row.setCreateBy(operatorId);
            row.setCreateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now));
            row.setUpdateBy(operatorId);
            row.setUpdateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now));
            rows.add(row);
        }
        return workerSkillMapper.insertBatch(rows) ? rows.size() : 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteById(Long skillId) {
        SfStaskWorkerSkill skill = workerSkillMapper.selectById(skillId);
        if (skill == null || !Objects.equals(skill.getTenantId(), requireTenantId())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_WORKER_SKILL_RECORD_NOT_FOUND);
        }
        return workerSkillMapper.deleteById(skillId);
    }

    @Override
    public Map<Long, SfStaskWorkerSkill> queryBestSkillMap(String tenantId, Collection<Long> employeeIds, Long workItemId) {
        if (CollUtil.isEmpty(employeeIds) || workItemId == null) {
            return Map.of();
        }
        return workerSkillMapper.selectByEmployeesAndWorkItem(tenantId, employeeIds, workItemId)
            .stream()
            .sorted(Comparator.comparingInt((SfStaskWorkerSkill row) -> skillLevelRank(row.getSkillLevel()))
                .thenComparing(row -> row.getWorkCount() == null ? 0 : row.getWorkCount(), Comparator.reverseOrder())
                .thenComparing(SfStaskWorkerSkill::getLastWorkDate, Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toMap(SfStaskWorkerSkill::getEmployeeId, Function.identity(), (a, b) -> a, HashMap::new));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordWorkCompletion(String tenantId, Long employeeId, Long workItemId, Date workDate) {
        if (employeeId == null) {
            return;
        }
        recordWorkCompletions(tenantId, List.of(employeeId), workItemId, workDate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordWorkCompletions(String tenantId, Collection<Long> employeeIds, Long workItemId, Date workDate) {
        if (StrUtil.isBlank(tenantId) || CollUtil.isEmpty(employeeIds) || workItemId == null) {
            return;
        }
        List<Long> distinctEmployeeIds = employeeIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (CollUtil.isEmpty(distinctEmployeeIds)) {
            return;
        }
        List<SfStaskWorkerSkill> skills = workerSkillMapper.selectByEmployeesAndWorkItem(
            tenantId, distinctEmployeeIds, workItemId);
        if (CollUtil.isEmpty(skills)) {
            return;
        }
        Map<Long, SfStaskWorkerSkill> bestSkillMap = skills.stream()
            .filter(skill -> skill.getEmployeeId() != null)
            .sorted(Comparator.comparingInt((SfStaskWorkerSkill row) -> skillLevelRank(row.getSkillLevel()))
                .thenComparing(row -> row.getWorkCount() == null ? 0 : row.getWorkCount(), Comparator.reverseOrder())
                .thenComparing(SfStaskWorkerSkill::getLastWorkDate, Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toMap(
                SfStaskWorkerSkill::getEmployeeId,
                Function.identity(),
                (first, ignored) -> first,
                HashMap::new));
        if (bestSkillMap.isEmpty()) {
            return;
        }

        Long updater = LoginHelper.getUserId();
        Date updateTime = new Date();
        for (SfStaskWorkerSkill skill : bestSkillMap.values()) {
            skill.setWorkCount((skill.getWorkCount() == null ? 0 : skill.getWorkCount()) + 1);
            if (workDate != null && (skill.getLastWorkDate() == null || workDate.after(skill.getLastWorkDate()))) {
                skill.setLastWorkDate(workDate);
            }
            skill.setUpdateBy(updater);
            skill.setUpdateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(updateTime));
        }
        workerSkillMapper.updateBatchById(bestSkillMap.values());
    }

    private void fillCompletedWorkCounts(String tenantId, Long employeeId, List<SfStaskWorkerSkillVo> vos) {
        if (CollUtil.isEmpty(vos) || employeeId == null) {
            return;
        }
        Map<Long, Integer> countMap = loadCompletedCountByWorkItem(tenantId, employeeId);
        for (SfStaskWorkerSkillVo vo : vos) {
            vo.setWorkCount(countMap.getOrDefault(vo.getWorkItemId(), 0));
        }
    }

    private void fillCompletedWorkCounts(String tenantId, Map<Long, List<SfStaskWorkerSkillVo>> skillsByEmployee) {
        if (skillsByEmployee.isEmpty()) {
            return;
        }
        Map<Long, Map<Long, Integer>> countByEmployeeAndWorkItem =
            loadCompletedCountByEmployeeAndWorkItem(tenantId, skillsByEmployee.keySet());
        skillsByEmployee.forEach((employeeId, skills) -> {
            Map<Long, Integer> countByWorkItem = countByEmployeeAndWorkItem.getOrDefault(employeeId, Map.of());
            for (SfStaskWorkerSkillVo skill : skills) {
                skill.setWorkCount(countByWorkItem.getOrDefault(skill.getWorkItemId(), 0));
            }
        });
    }

    private Map<Long, Integer> loadCompletedCountByWorkItem(String tenantId, Long employeeId) {
        return countCompletedWorkByEmployeeAndWorkItem(
            tenantId, dispatchMapper.selectAcceptedByWorkerId(tenantId, employeeId), employeeId)
            .getOrDefault(employeeId, Map.of());
    }

    private Map<Long, Map<Long, Integer>> loadCompletedCountByEmployeeAndWorkItem(
        String tenantId, Collection<Long> employeeIds) {
        return countCompletedWorkByEmployeeAndWorkItem(
            tenantId, dispatchMapper.selectAcceptedByWorkerIds(tenantId, employeeIds));
    }

    private Map<Long, Map<Long, Integer>> countCompletedWorkByEmployeeAndWorkItem(
        String tenantId, List<SfStaskDispatch> dispatches) {
        return countCompletedWorkByEmployeeAndWorkItem(tenantId, dispatches, null);
    }

    private Map<Long, Map<Long, Integer>> countCompletedWorkByEmployeeAndWorkItem(
        String tenantId, List<SfStaskDispatch> dispatches, Long defaultWorkerId) {
        if (CollUtil.isEmpty(dispatches)) {
            return Map.of();
        }
        List<Long> orderIds = dispatches.stream()
            .map(SfStaskDispatch::getOrderId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (CollUtil.isEmpty(orderIds)) {
            return Map.of();
        }
        Map<Long, SfStaskWorkOrder> orderMap = workOrderMapper.selectByIds(tenantId, orderIds).stream()
            .filter(Objects::nonNull)
            .filter(order -> order.getOrderId() != null)
            .collect(Collectors.toMap(SfStaskWorkOrder::getOrderId, Function.identity(), (first, ignored) -> first));
        Map<Long, Map<Long, Integer>> countMap = new HashMap<>();
        Set<WorkerOrderKey> countedWorkerOrders = new HashSet<>();
        for (SfStaskDispatch dispatch : dispatches) {
            Long workerId = dispatch == null || dispatch.getWorkerId() == null
                ? defaultWorkerId : dispatch.getWorkerId();
            if (dispatch == null || workerId == null || dispatch.getOrderId() == null
                || !countedWorkerOrders.add(new WorkerOrderKey(workerId, dispatch.getOrderId()))) {
                continue;
            }
            SfStaskWorkOrder order = orderMap.get(dispatch.getOrderId());
            if (order == null || !StaskOrderStatus.ACCEPTANCE_PASSED.equals(order.getStatus())
                || order.getWorkItemId() == null) {
                continue;
            }
            countMap.computeIfAbsent(workerId, ignored -> new HashMap<>())
                .merge(order.getWorkItemId(), 1, Integer::sum);
        }
        return countMap;
    }

    private void ensureWorker(Long employeeId) {
        List<SysEmployeeVo> employees = employeeAccessor.queryByIds(List.of(employeeId));
        if (CollUtil.isEmpty(employees) || !EmployeeConstants.APP_ROLE_STASK_WORKER.equals(employees.get(0).getAppRoleCode())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_DISPATCH_WORKER_INVALID);
        }
    }

    private void validateUniqueSkills(List<SfStaskWorkerSkillBo> skills) {
        Set<String> keys = new LinkedHashSet<>();
        for (SfStaskWorkerSkillBo skill : skills) {
            String key = skill.getWorkItemId() + ":" + StrUtil.blankToDefault(skill.getCropType(), DEFAULT_CROP_TYPE).trim();
            if (!keys.add(key)) {
                throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_WORKER_DUPLICATE_SKILL);
            }
        }
    }

    private Set<String> loadNormalCropTypes(String tenantId, List<SfStaskWorkerSkillBo> skills) {
        List<String> cropTypes = skills.stream()
            .map(SfStaskWorkerSkillBo::getCropType)
            .map(value -> StrUtil.blankToDefault(value, DEFAULT_CROP_TYPE).trim())
            .filter(value -> !DEFAULT_CROP_TYPE.equals(value))
            .distinct()
            .toList();
        return cropSpeciesMapper.selectVoListByCodes(tenantId, cropTypes).stream()
            .map(row -> row.getSpeciesCode())
            .collect(Collectors.toSet());
    }

    private String normalizeCropType(String cropType, Set<String> normalCropTypes) {
        String normalized = StrUtil.blankToDefault(cropType, DEFAULT_CROP_TYPE).trim();
        if (DEFAULT_CROP_TYPE.equals(normalized)) {
            return DEFAULT_CROP_TYPE;
        }
        if (!normalCropTypes.contains(normalized)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_WORKER_CROP_TYPE_UNAVAILABLE);
        }
        return normalized;
    }

    private Map<Long, SfFarmWorkDict> queryWorkItemMap(String tenantId, Collection<Long> workItemIds) {
        if (CollUtil.isEmpty(workItemIds)) {
            return Map.of();
        }
        return farmWorkDictMapper.selectNormalByIds(tenantId, workItemIds)
            .stream()
            .filter(row -> FarmWorkNodeType.isItem(row.getNodeType()))
            .filter(row -> SystemConstants.NORMAL.equals(row.getStatus()))
            .collect(Collectors.toMap(SfFarmWorkDict::getDictId, Function.identity(), (a, b) -> a));
    }

    private void fillWorkItemNames(String tenantId, List<SfStaskWorkerSkillVo> vos) {
        if (CollUtil.isEmpty(vos)) {
            return;
        }
        Map<Long, SfFarmWorkDict> dictMap = farmWorkDictMapper.selectNormalByIds(tenantId, vos.stream()
                .map(SfStaskWorkerSkillVo::getWorkItemId)
                .filter(Objects::nonNull)
                .toList())
            .stream()
            .collect(Collectors.toMap(SfFarmWorkDict::getDictId, Function.identity(), (a, b) -> a));
        for (SfStaskWorkerSkillVo vo : vos) {
            SfFarmWorkDict dict = dictMap.get(vo.getWorkItemId());
            vo.setWorkItemName(dict == null ? null : dict.getDictName());
        }
    }

    private void fillCropTypeNames(String tenantId, List<SfStaskWorkerSkillVo> vos) {
        if (CollUtil.isEmpty(vos)) {
            return;
        }
        List<String> codes = vos.stream()
            .map(SfStaskWorkerSkillVo::getCropType)
            .filter(code -> StrUtil.isNotBlank(code) && !DEFAULT_CROP_TYPE.equals(code))
            .distinct()
            .toList();
        Map<String, String> nameMap = cropSpeciesMapper.selectVoListByCodes(tenantId, codes)
            .stream()
            .collect(Collectors.toMap(
                row -> row.getSpeciesCode(),
                row -> row.getSpeciesName(),
                (a, b) -> a,
                HashMap::new));
        for (SfStaskWorkerSkillVo vo : vos) {
            if (DEFAULT_CROP_TYPE.equals(vo.getCropType())) {
                vo.setCropTypeName(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKER_ALL_CROPS));
            } else {
                vo.setCropTypeName(nameMap.get(vo.getCropType()));
            }
        }
    }

    private void fillEmployeeName(Long employeeId, List<SfStaskWorkerSkillVo> vos) {
        if (CollUtil.isEmpty(vos) || employeeId == null) {
            return;
        }
        List<SysEmployeeVo> employees = employeeAccessor.queryByIds(List.of(employeeId));
        String employeeName = CollUtil.isEmpty(employees) ? null : employees.get(0).getName();
        for (SfStaskWorkerSkillVo vo : vos) {
            vo.setEmployeeName(employeeName);
        }
    }

    private String skillKey(SfStaskWorkerSkill skill) {
        return skill.getWorkItemId() + ":" + skill.getCropType();
    }

    private void validateSkillLevel(String skillLevel) {
        if (!StaskWorkerSkillLevel.ADVANCED.equals(skillLevel)
            && !StaskWorkerSkillLevel.MEDIUM.equals(skillLevel)
            && !StaskWorkerSkillLevel.JUNIOR.equals(skillLevel)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_WORKER_SKILL_LEVEL_INVALID);
        }
    }

    public static int skillLevelRank(String skillLevel) {
        if (StaskWorkerSkillLevel.ADVANCED.equals(skillLevel)) {
            return 0;
        }
        if (StaskWorkerSkillLevel.MEDIUM.equals(skillLevel)) {
            return 1;
        }
        if (StaskWorkerSkillLevel.JUNIOR.equals(skillLevel)) {
            return 2;
        }
        return 9;
    }

    private String requireTenantId() {
        String tenantId = TenantHelper.getTenantId();
        if (StrUtil.isBlank(tenantId)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_COMMON_TENANT_CONTEXT_MISSING);
        }
        return tenantId;
    }

    private record WorkerOrderKey(Long workerId, Long orderId) {
    }
}
