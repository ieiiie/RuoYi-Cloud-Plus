package com.ym.agriculture.farmtask.workorder.service.packagecmd;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.farming.field.dao.SfFieldMapper;
import com.ym.agriculture.farming.field.model.constants.FieldType;
import com.ym.agriculture.farming.field.model.entity.SfField;
import com.ym.agriculture.farming.farmwork.dao.SfFarmWorkDictMapper;
import com.ym.agriculture.farming.farmwork.model.constants.FarmWorkNodeType;
import com.ym.agriculture.farming.farmwork.model.entity.SfFarmWorkDict;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderGreenhouseMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderItemMapper;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskTechConfirmBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskTechInstructionBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskWorkItemCreateBo;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskLeaderAssignMode;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrderGreenhouse;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 任务包内容写入器。
 *
 * <p>负责农事项、大棚关系和技术指导的批量持久化，不负责事务、权限和状态流转。</p>
 */
@RequiredArgsConstructor
@Component
public class SfStaskPackageContentWriter {

    private final com.ym.agriculture.shared.i18n.StaskMessageResolver messages;
    private final SfStaskWorkOrderItemMapper itemMapper;
    private final SfStaskWorkOrderGreenhouseMapper greenhouseMapper;
    private final SfFarmWorkDictMapper farmWorkDictMapper;
    private final SfFieldMapper fieldMapper;

    /**
     * 保存规划后的任务包农事项和大棚关系。
     *
     * @param tenantId      租户ID
     * @param packageId     任务包ID
     * @param workItems     规范化农事项
     * @param now           统一写入时间
     * @param preservedTech 需保留的技术指导
     * @param allowTechFields 是否允许使用请求中的技术字段
     * @return 已保存农事项实体
     */
    public List<SfStaskWorkOrderItem> writePackageContent(String tenantId, Long packageId,
        List<SfStaskPackageContentPlanner.NormalizedWorkItem> workItems, Date now,
        PreservedTechInstructions preservedTech, boolean allowTechFields) {
        List<SfStaskWorkOrderItem> items = insertPackageItems(
            tenantId, packageId, workItems, now, preservedTech, allowTechFields);
        insertPackageGreenhouses(tenantId, packageId, items, workItems, now);
        return items;
    }

    /**
     * 按原有顺序删除任务包旧农事项和大棚关系。
     *
     * @param tenantId  租户ID
     * @param packageId 任务包ID
     */
    public void deletePackageContent(String tenantId, Long packageId) {
        itemMapper.deleteByPackageId(tenantId, packageId);
        greenhouseMapper.deleteByPackageId(tenantId, packageId);
    }

    /**
     * 读取旧明细中每个农事项的首条技术指导，用于管理员退回后重提。
     *
     * @param tenantId  租户ID
     * @param packageId 任务包ID
     * @return 技术指导快照
     */
    public PreservedTechInstructions buildPreservedTechInstructions(String tenantId, Long packageId) {
        List<SfStaskWorkOrderItem> oldItems = itemMapper.selectByPackageId(tenantId, packageId);
        if (CollUtil.isEmpty(oldItems)) {
            return PreservedTechInstructions.empty();
        }
        Map<Long, SfStaskWorkOrderItem> canonicalByWorkItemId = new LinkedHashMap<>();
        oldItems.stream()
            .filter(item -> StringUtils.isNotBlank(item.getTechInstruction())
                || StringUtils.isNotBlank(item.getTechPhotos()))
            .forEach(item -> canonicalByWorkItemId.putIfAbsent(item.getWorkItemId(), item));
        return canonicalByWorkItemId.isEmpty()
            ? PreservedTechInstructions.empty() : new PreservedTechInstructions(canonicalByWorkItemId);
    }

    /**
     * 批量写入任务包农事项。
     *
     * @param tenantId      租户ID
     * @param packageId     任务包ID
     * @param workItems     规范化农事项
     * @param now           写入时间
     * @param preservedTech 保留技术指导
     * @param allowTechFields 是否允许请求技术字段
     * @return 已写入农事项
     */
    public List<SfStaskWorkOrderItem> insertPackageItems(String tenantId, Long packageId,
        List<SfStaskPackageContentPlanner.NormalizedWorkItem> workItems, Date now,
        PreservedTechInstructions preservedTech, boolean allowTechFields) {
        if (CollUtil.isEmpty(workItems)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_WORK_ITEM_REQUIRED);
        }
        Map<Long, SfFarmWorkDict> workItemMap = queryWorkItemMap(tenantId, workItems.stream()
            .map(SfStaskPackageContentPlanner.NormalizedWorkItem::workItem)
            .map(SfStaskWorkItemCreateBo::getWorkItemId)
            .toList());
        Map<Long, String> categoryNameMap = queryCategoryNameMap(tenantId, workItemMap.values());
        List<SfStaskWorkOrderItem> rows = new ArrayList<>(workItems.size());
        int sort = 0;
        for (SfStaskPackageContentPlanner.NormalizedWorkItem normalized : workItems) {
            rows.add(buildPackageItem(tenantId, packageId, normalized, workItemMap,
                categoryNameMap, now, preservedTech, allowTechFields, sort++));
        }
        if (!itemMapper.insertBatch(rows)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_WORK_ITEM_SAVE_FAILED);
        }
        return rows;
    }

    /**
     * 批量写入任务包农事项与大棚的关系快照。
     *
     * @param tenantId  租户ID
     * @param packageId 任务包ID
     * @param savedItems 已保存农事项
     * @param workItems  规范化农事项
     * @param now        写入时间
     */
    public void insertPackageGreenhouses(String tenantId, Long packageId,
        List<SfStaskWorkOrderItem> savedItems,
        List<SfStaskPackageContentPlanner.NormalizedWorkItem> workItems, Date now) {
        List<Long> greenhouseIds = workItems.stream()
            .flatMap(item -> item.workItem().getGreenhouseIds().stream())
            .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
        if (CollUtil.isEmpty(greenhouseIds)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_GREENHOUSE_REQUIRED);
        }
        if (savedItems.size() != workItems.size()) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_WORK_ITEM_DETAIL_INCOMPLETE);
        }
        Map<Long, SfField> fieldMap = queryGreenhouseMap(tenantId, greenhouseIds);
        List<SfStaskWorkOrderGreenhouse> rows = buildGreenhouseRows(
            tenantId, packageId, savedItems, workItems, now, fieldMap);
        if (!greenhouseMapper.insertBatch(rows)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_GREENHOUSE_DETAIL_SAVE_FAILED);
        }
    }

    /**
     * 将技术确认请求按农事项首条内容批量回写到全部重复明细。
     *
     * @param items 已保存农事项
     * @param bo    技术确认请求
     */
    public void applyTechInstructions(List<SfStaskWorkOrderItem> items, SfStaskTechConfirmBo bo) {
        if (CollUtil.isEmpty(items)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_WORK_ITEM_EMPTY);
        }
        if (bo == null || CollUtil.isEmpty(bo.getInstructions())) {
            return;
        }
        Map<Long, SfStaskTechInstructionBo> instructionMap = bo.getInstructions().stream()
            .collect(Collectors.toMap(SfStaskTechInstructionBo::getItemId, Function.identity(), (a, b) -> a));
        Map<Long, SfStaskTechInstructionBo> canonicalByWorkItemId = new LinkedHashMap<>();
        for (SfStaskWorkOrderItem item : items) {
            SfStaskTechInstructionBo instruction = instructionMap.get(item.getItemId());
            if (instruction != null) {
                canonicalByWorkItemId.putIfAbsent(item.getWorkItemId(), instruction);
            }
        }
        List<SfStaskWorkOrderItem> updates = buildTechInstructionUpdates(items, canonicalByWorkItemId);
        if (CollUtil.isNotEmpty(updates) && !itemMapper.updateBatchById(updates)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_TECH_INSTRUCTION_SAVE_FAILED);
        }
    }

    private SfStaskWorkOrderItem buildPackageItem(String tenantId, Long packageId,
        SfStaskPackageContentPlanner.NormalizedWorkItem normalized,
        Map<Long, SfFarmWorkDict> workItemMap, Map<Long, String> categoryNameMap, Date now,
        PreservedTechInstructions preservedTech, boolean allowTechFields, int sort) {
        SfStaskWorkItemCreateBo bo = normalized.workItem();
        SfFarmWorkDict dict = workItemMap.get(bo.getWorkItemId());
        if (dict == null) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_WORK_ITEM_UNAVAILABLE);
        }
        SfStaskWorkOrderItem item = new SfStaskWorkOrderItem();
        item.setItemId(IdWorker.getId());
        item.setPackageId(packageId);
        item.setTenantId(tenantId);
        item.setWorkItemId(bo.getWorkItemId());
        item.setWorkItemNameSnapshot(dict.getDictName());
        item.setWorkItemCodeSnapshot(dict.getDictCode());
        item.setCategoryIdSnapshot(dict.getParentId());
        item.setCategoryNameSnapshot(categoryNameMap.get(dict.getParentId()));
        item.setManagerRequirement(bo.getManagerRequirement());
        item.setManagerPhotos(normalizePhotoJson(bo.getManagerPhotos(), false, messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_PACKAGE_APPLICANT_PHOTO)));
        fillTechFields(item, bo, preservedTech, allowTechFields);
        fillLeaderAssignmentFields(item, normalized);
        item.setSortOrder(sort);
        item.setCreateTime(now);
        item.setUpdateTime(now);
        return item;
    }

    private void fillTechFields(SfStaskWorkOrderItem item, SfStaskWorkItemCreateBo bo,
        PreservedTechInstructions preservedTech, boolean allowTechFields) {
        SfStaskWorkOrderItem preserved = preservedTech.take(bo.getWorkItemId());
        if (preserved != null) {
            item.setTechInstruction(preserved.getTechInstruction());
            item.setTechPhotos(preserved.getTechPhotos());
            return;
        }
        if (allowTechFields) {
            item.setTechInstruction(bo.getTechInstruction());
            item.setTechPhotos(normalizePhotoJson(bo.getTechPhotos(), false, messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_PACKAGE_TECHNICIAN_REFERENCE_PHOTO)));
        }
    }

    private static void fillLeaderAssignmentFields(SfStaskWorkOrderItem item,
        SfStaskPackageContentPlanner.NormalizedWorkItem normalized) {
        SfStaskWorkItemCreateBo bo = normalized.workItem();
        String mode = StaskLeaderAssignMode.normalize(bo.getLeaderAssignMode());
        item.setLeaderAssignMode(mode);
        if (StaskLeaderAssignMode.AUTO.equals(mode)) {
            item.setManualLeaderId(null);
            item.setManualLeaderNameSnapshot(null);
        } else {
            item.setManualLeaderId(bo.getManualLeaderId());
            item.setManualLeaderNameSnapshot(normalized.leaderName());
        }
        item.setLeaderIdSnapshot(normalized.leaderId());
        item.setLeaderNameSnapshot(normalized.leaderName());
    }

    private List<SfStaskWorkOrderGreenhouse> buildGreenhouseRows(String tenantId, Long packageId,
        List<SfStaskWorkOrderItem> savedItems,
        List<SfStaskPackageContentPlanner.NormalizedWorkItem> workItems, Date now,
        Map<Long, SfField> fieldMap) {
        List<SfStaskWorkOrderGreenhouse> rows = new ArrayList<>();
        for (int i = 0; i < workItems.size(); i++) {
            SfStaskWorkItemCreateBo workItem = workItems.get(i).workItem();
            SfStaskWorkOrderItem savedItem = savedItems.get(i);
            for (Long greenhouseId : workItem.getGreenhouseIds()) {
                SfField field = fieldMap.get(greenhouseId);
                if (field == null) {
                    throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_GREENHOUSE_NOT_FOUND);
                }
                rows.add(buildGreenhouseRow(
                    tenantId, packageId, savedItem, workItem, greenhouseId, field, now));
            }
        }
        return rows;
    }

    private static SfStaskWorkOrderGreenhouse buildGreenhouseRow(String tenantId, Long packageId,
        SfStaskWorkOrderItem savedItem, SfStaskWorkItemCreateBo workItem, Long greenhouseId,
        SfField field, Date now) {
        SfStaskWorkOrderGreenhouse row = new SfStaskWorkOrderGreenhouse();
        row.setGreenhouseItemId(IdWorker.getId());
        row.setPackageId(packageId);
        row.setTenantId(tenantId);
        row.setItemId(savedItem.getItemId());
        row.setWorkItemId(workItem.getWorkItemId());
        row.setGreenhouseId(greenhouseId);
        row.setGreenhouseCodeSnapshot(field.getFieldCode());
        row.setGreenhouseNameSnapshot(field.getFieldName());
        row.setCreateTime(now);
        return row;
    }

    private List<SfStaskWorkOrderItem> buildTechInstructionUpdates(
        List<SfStaskWorkOrderItem> items, Map<Long, SfStaskTechInstructionBo> canonicalByWorkItemId) {
        List<SfStaskWorkOrderItem> updates = new ArrayList<>();
        Date now = new Date();
        for (SfStaskWorkOrderItem item : items) {
            SfStaskTechInstructionBo instruction = canonicalByWorkItemId.get(item.getWorkItemId());
            if (instruction == null) {
                continue;
            }
            item.setTechInstruction(instruction.getTechInstruction());
            item.setTechPhotos(normalizePhotoJson(instruction.getTechPhotos(), false, messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_PACKAGE_TECHNICIAN_REFERENCE_PHOTO)));
            item.setUpdateTime(now);
            updates.add(item);
        }
        return updates;
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

    private Map<Long, String> queryCategoryNameMap(String tenantId, Collection<SfFarmWorkDict> workItems) {
        List<Long> categoryIds = (workItems == null ? Stream.<SfFarmWorkDict>empty() : workItems.stream())
            .map(SfFarmWorkDict::getParentId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (CollUtil.isEmpty(categoryIds)) {
            return Map.of();
        }
        return farmWorkDictMapper.selectNormalByIds(tenantId, categoryIds).stream()
            .collect(Collectors.toMap(SfFarmWorkDict::getDictId, SfFarmWorkDict::getDictName, (a, b) -> a));
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

    private String normalizePhotoJson(String photoJson, boolean required, String fieldName) {
        if (StringUtils.isBlank(photoJson)) {
            if (required) {
                throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_UPLOAD_FIELD_REQUIRED,
                    fieldName);
            }
            return null;
        }
        try {
            if (JSON.parseArray(photoJson).isEmpty() && required) {
                throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_UPLOAD_FIELD_REQUIRED,
                    fieldName);
            }
            return photoJson;
        } catch (JSONException e) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_FIELD_JSON_ARRAY,
                fieldName);
        }
    }

    /**
     * 按农事项ID保存的首条技术指导快照。
     */
    public static final class PreservedTechInstructions {
        private final Map<Long, SfStaskWorkOrderItem> canonicalByWorkItemId;

        private PreservedTechInstructions(Map<Long, SfStaskWorkOrderItem> canonicalByWorkItemId) {
            this.canonicalByWorkItemId = canonicalByWorkItemId;
        }

        /**
         * 创建不包含技术指导的快照。
         *
         * @return 空快照
         */
        public static PreservedTechInstructions empty() {
            return new PreservedTechInstructions(Map.of());
        }

        /**
         * 取得农事项的首条技术指导；重复明细共享同一内容且不会消费移除。
         *
         * @param workItemId 农事项ID
         * @return 技术指导来源明细
         */
        public SfStaskWorkOrderItem take(Long workItemId) {
            return canonicalByWorkItemId.get(workItemId);
        }
    }
}
