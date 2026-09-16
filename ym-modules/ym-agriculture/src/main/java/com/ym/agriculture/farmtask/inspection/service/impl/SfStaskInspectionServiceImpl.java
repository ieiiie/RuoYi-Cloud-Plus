package com.ym.agriculture.farmtask.inspection.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farming.batch.model.constants.PlantingBatchStatus;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchVo;
import com.ym.agriculture.farming.batch.service.ISfPlantingBatchService;
import com.ym.agriculture.farming.field.dao.SfFieldMapper;
import com.ym.agriculture.farming.field.layout.model.vo.GreenhouseLayoutVo;
import com.ym.agriculture.farming.field.layout.service.IGreenhouseLayoutService;
import com.ym.agriculture.farming.field.model.constants.FieldType;
import com.ym.agriculture.farming.field.model.entity.SfField;
import com.ym.agriculture.farming.field.model.vo.SfFieldVo;
import com.ym.agriculture.shared.i18n.StaskErrorCodes;
import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import com.ym.agriculture.shared.i18n.StaskMessageResolver;
import com.ym.agriculture.shared.i18n.StaskMiniappException;
import com.ym.agriculture.farmtask.inspection.dao.SfStaskInspectionMapper;
import com.ym.agriculture.farmtask.inspection.model.bo.SfStaskInspectionCreateBo;
import com.ym.agriculture.farmtask.inspection.model.bo.SfStaskInspectionDeleteBo;
import com.ym.agriculture.farmtask.inspection.model.bo.SfStaskInspectionGreenhouseGroupQueryBo;
import com.ym.agriculture.farmtask.inspection.model.bo.SfStaskInspectionHandleBo;
import com.ym.agriculture.farmtask.inspection.model.bo.SfStaskInspectionOrderQueryBo;
import com.ym.agriculture.farmtask.inspection.model.bo.SfStaskInspectionQueryBo;
import com.ym.agriculture.farmtask.inspection.model.bo.SfStaskInspectionRejectBo;
import com.ym.agriculture.farmtask.inspection.model.bo.SfStaskInspectionUpdateBo;
import com.ym.agriculture.farmtask.inspection.model.constants.SfStaskInspectionSeverity;
import com.ym.agriculture.farmtask.inspection.model.constants.SfStaskInspectionStatus;
import com.ym.agriculture.farmtask.inspection.model.entity.SfStaskInspection;
import com.ym.agriculture.farmtask.inspection.model.vo.SfStaskInspectionBadgeVo;
import com.ym.agriculture.farmtask.inspection.model.vo.SfStaskInspectionDetailVo;
import com.ym.agriculture.farmtask.inspection.model.vo.SfStaskInspectionEmployeeOptionVo;
import com.ym.agriculture.farmtask.inspection.model.vo.SfStaskInspectionGreenhouseFieldVo;
import com.ym.agriculture.farmtask.inspection.model.vo.SfStaskInspectionGreenhouseGroupVo;
import com.ym.agriculture.farmtask.inspection.model.vo.SfStaskInspectionListVo;
import com.ym.agriculture.farmtask.inspection.model.vo.SfStaskInspectionMutationVo;
import com.ym.agriculture.farmtask.inspection.model.vo.SfStaskInspectionOrderOptionVo;
import com.ym.agriculture.farmtask.inspection.model.vo.SfStaskInspectionSnapshotVo;
import com.ym.agriculture.farmtask.inspection.service.ISfStaskInspectionService;
import com.ym.agriculture.farmtask.inspection.support.StaskInspectionPhotoReferenceNormalizer;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderMapper;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskCreatorRole;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeAccessor;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeLeaderOptionVo;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
 * stask 农事抽检服务。
 *
 * <p>抽检是独立聚合，不直接复用工单命令服务；关联工单只在创建/编辑时读取当前快照，
 * 后续工单状态变化不会反向修改抽检记录。</p>
 */
@Service
@RequiredArgsConstructor
public class SfStaskInspectionServiceImpl implements ISfStaskInspectionService {

    private static final String DELETED = "1";
    private static final int MAX_TEXT_LENGTH = 500;
    private static final int DEFAULT_OPTION_PAGE_SIZE = 20;

    private static final Set<String> LINKABLE_ORDER_STATUSES = Set.of(
        StaskOrderStatus.PENDING_TECH_CONFIRM,
        StaskOrderStatus.TECH_REJECTED,
        StaskOrderStatus.PENDING_LEADER_ACCEPT,
        StaskOrderStatus.PENDING_LEADER_ASSIGN,
        StaskOrderStatus.ASSIGN_COMPLETE,
        StaskOrderStatus.LEADER_ARRIVED,
        StaskOrderStatus.PENDING_ACCEPTANCE,
        StaskOrderStatus.ACCEPTANCE_PASSED,
        StaskOrderStatus.ACCEPTANCE_REJECTED
    );

    private final SfStaskInspectionMapper inspectionMapper;
    private final SfFieldMapper fieldMapper;
    private final ISfPlantingBatchService plantingBatchService;
    private final IGreenhouseLayoutService greenhouseLayoutService;
    private final SfStaskWorkOrderMapper workOrderMapper;
    private final SfStaskEmployeeAccessor employeeAccessor;
    private final StaskInspectionPhotoReferenceNormalizer photoNormalizer;
    private final StaskMessageResolver messages;

    @Override
    public SfStaskInspectionBadgeVo badgeCount() {
        String tenantId = requireTenantId();
        ActorCapabilities actor = currentCapabilities();
        SfStaskInspectionBadgeVo vo = new SfStaskInspectionBadgeVo();
        vo.setTechnicianUnprocessedCount(actor.technician()
            ? inspectionMapper.countUnprocessedByTechnician(tenantId, actor.employeeId(),
            SfStaskInspectionStatus.UNPROCESSED) : 0L);
        vo.setLeaderUnprocessedCount(actor.leader()
            ? inspectionMapper.countUnprocessedByLeader(tenantId, actor.employeeId(),
            SfStaskInspectionStatus.UNPROCESSED) : 0L);
        vo.setCanCreate(actor.productionAdmin());
        return vo;
    }

    @Override
    public PageResult<SfStaskInspectionListVo> page(SfStaskInspectionQueryBo bo, PageQuery pageQuery) {
        String tenantId = requireTenantId();
        SfStaskInspectionQueryBo query = bo == null ? new SfStaskInspectionQueryBo() : bo;
        validateQuery(tenantId, query);
        LambdaQueryWrapper<SfStaskInspection> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SfStaskInspection::getTenantId, tenantId)
            .eq(SfStaskInspection::getDelFlag, SystemConstants.NORMAL)
            .ge(query.getFoundStartDate() != null, SfStaskInspection::getFoundDate, query.getFoundStartDate())
            .le(query.getFoundEndDate() != null, SfStaskInspection::getFoundDate, query.getFoundEndDate())
            .eq(query.getGreenhouseId() != null, SfStaskInspection::getGreenhouseId, query.getGreenhouseId())
            .eq(query.getResponsibleTechnicianEmployeeId() != null,
                SfStaskInspection::getResponsibleTechnicianEmployeeId, query.getResponsibleTechnicianEmployeeId())
            .eq(query.getResponsibleLeaderEmployeeId() != null,
                SfStaskInspection::getResponsibleLeaderEmployeeId, query.getResponsibleLeaderEmployeeId())
            .eq(StringUtils.isNotBlank(query.getStatus()), SfStaskInspection::getStatus, query.getStatus())
            .orderByDesc(SfStaskInspection::getFoundDate)
            .orderByDesc(SfStaskInspection::getCreateTime)
            .orderByDesc(SfStaskInspection::getInspectionId);
        Page<SfStaskInspection> source = inspectionMapper.selectPage(buildPage(pageQuery), wrapper);
        ActorCapabilities actor = currentCapabilities();
        List<SfStaskInspectionListVo> rows = CollUtil.emptyIfNull(source.getRecords()).stream()
            .map(row -> toListVo(row, actor))
            .toList();
        Page<SfStaskInspectionListVo> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(rows, result);
    }

    @Override
    public SfStaskInspectionDetailVo detail(Long inspectionId) {
        String tenantId = requireTenantId();
        SfStaskInspection row = requireRecord(tenantId, inspectionId);
        ActorCapabilities actor = currentCapabilities();
        SfStaskInspectionDetailVo vo = new SfStaskInspectionDetailVo();
        BeanUtil.copyProperties(toListVo(row, actor), vo);
        vo.setProblemPhotoJson(photoNormalizer.render(tenantId, row.getProblemPhotoJson()));
        vo.setCreatorEmployeeId(row.getCreatorEmployeeId());
        vo.setCreatorNameSnapshot(row.getCreatorNameSnapshot());
        vo.setHandleCount(countOrZero(row.getHandleCount()));
        vo.setRejectCount(countOrZero(row.getRejectCount()));
        vo.setHasHandleHistory(hasHandleHistory(row));
        vo.setFirstHandle(toHandleVo(tenantId, row.getFirstHandleDescription(), row.getFirstHandlePhotoJson(),
            row.getFirstHandleEmployeeId(), row.getFirstHandleEmployeeNameSnapshot(), row.getFirstHandledAt()));
        vo.setCurrentHandle(toHandleVo(tenantId, row.getCurrentHandleDescription(), row.getCurrentHandlePhotoJson(),
            row.getCurrentHandleEmployeeId(), row.getCurrentHandleEmployeeNameSnapshot(), row.getCurrentHandledAt()));
        vo.setLatestReject(toRejectVo(row));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SfStaskInspectionMutationVo create(SfStaskInspectionCreateBo bo) {
        String tenantId = requireTenantId();
        ActorCapabilities actor = requireProductionAdmin();
        SfStaskInspectionCreateBo input = requireCreateBo(bo);
        LocalDate foundDate = input.getFoundDate() == null ? LocalDate.now() : input.getFoundDate();
        validateDate(foundDate);
        String severity = validateSeverity(input.getSeverity());
        String problemDescription = validateText(input.getProblemDescription(), "问题描述");
        String problemPhotoJson = photoNormalizer.normalize(tenantId, input.getProblemPhotoJson(), "问题照片");
        SfField greenhouse = requireGreenhouse(tenantId, input.getGreenhouseId());
        Relation relation = resolveRelation(tenantId, greenhouse, input.getOrderId(),
            input.getResponsibleTechnicianEmployeeId(), input.getResponsibleLeaderEmployeeId());
        SysEmployeeVo discoverer = requireEmployee(tenantId, actor.employeeId(), null);
        Date now = new Date();

        SfStaskInspection row = new SfStaskInspection();
        row.setTenantId(tenantId);
        row.setFoundDate(foundDate);
        row.setSeverity(severity);
        row.setStatus(SfStaskInspectionStatus.UNPROCESSED);
        row.setProblemDescription(problemDescription);
        row.setProblemPhotoJson(problemPhotoJson);
        applyRelation(row, relation);
        row.setDiscovererEmployeeId(discoverer.getEmployeeId());
        row.setDiscovererNameSnapshot(discoverer.getName());
        row.setCreatorEmployeeId(discoverer.getEmployeeId());
        row.setCreatorNameSnapshot(discoverer.getName());
        row.setHandleCount(0);
        row.setRejectCount(0);
        row.setVersion(0L);
        row.setDelFlag(SystemConstants.NORMAL);
        row.setCreateBy(discoverer.getEmployeeId());
        row.setCreateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now));
        row.setUpdateBy(discoverer.getEmployeeId());
        row.setUpdateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now));
        inspectionMapper.insert(row);
        return mutation(row.getInspectionId(), row.getStatus(), row.getHandleCount(), row.getVersion());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SfStaskInspectionMutationVo update(Long inspectionId, SfStaskInspectionUpdateBo bo) {
        String tenantId = requireTenantId();
        ActorCapabilities actor = requireProductionAdmin();
        SfStaskInspection row = requireRecord(tenantId, inspectionId);
        requireOwner(row, actor.employeeId());
        requireVersion(row, bo == null ? null : bo.getVersion());
        if (hasHandleHistory(row)) {
            ensureImmutableRelation(row, bo);
            String severity = bo.getSeverity() == null ? row.getSeverity() : validateSeverity(bo.getSeverity());
            String description = bo.getProblemDescription() == null
                ? row.getProblemDescription() : validateText(bo.getProblemDescription(), "问题描述");
            String photoJson = bo.getProblemPhotoJson() == null ? row.getProblemPhotoJson()
                : photoNormalizer.normalize(tenantId, bo.getProblemPhotoJson(), "问题照片");
            updateContent(tenantId, row, severity, description, photoJson, actor.employeeId());
            return mutation(row.getInspectionId(), row.getStatus(), countOrZero(row.getHandleCount()),
                nextVersion(row));
        }

        SfStaskInspectionUpdateBo input = bo == null ? new SfStaskInspectionUpdateBo() : bo;
        if (input.getFoundDate() == null || input.getGreenhouseId() == null
            || input.getResponsibleTechnicianEmployeeId() == null) {
            throw invalid("发现日期、大棚和负责技术员不能为空");
        }
        validateDate(input.getFoundDate());
        String severity = validateSeverity(input.getSeverity());
        String description = validateText(input.getProblemDescription(), "问题描述");
        String photoJson = photoNormalizer.normalize(tenantId, input.getProblemPhotoJson(), "问题照片");
        SfField greenhouse = requireGreenhouse(tenantId, input.getGreenhouseId());
        Relation relation = resolveRelation(tenantId, greenhouse, input.getOrderId(),
            input.getResponsibleTechnicianEmployeeId(), input.getResponsibleLeaderEmployeeId(),
            Objects.equals(input.getOrderId(), row.getOrderId()));
        Date now = new Date();
        LambdaUpdateWrapper<SfStaskInspection> wrapper = baseUpdateWrapper(tenantId, row, actor.employeeId());
        wrapper.set(SfStaskInspection::getFoundDate, input.getFoundDate())
            .set(SfStaskInspection::getSeverity, severity)
            .set(SfStaskInspection::getProblemDescription, description)
            .set(SfStaskInspection::getProblemPhotoJson, photoJson)
            .set(SfStaskInspection::getGreenhouseId, relation.greenhouse().getFieldId())
            .set(SfStaskInspection::getGreenhouseNameSnapshot, relation.greenhouse().getFieldName())
            .set(SfStaskInspection::getPlantingBatchIdSnapshot, batchId(relation.batch()))
            .set(SfStaskInspection::getCropIdSnapshot, cropId(relation.batch()))
            .set(SfStaskInspection::getCropNameSnapshot, cropName(relation.batch()))
            .set(SfStaskInspection::getCropVarietyIdSnapshot, varietyId(relation.batch()))
            .set(SfStaskInspection::getCropVarietyNameSnapshot, varietyName(relation.batch()))
            .set(SfStaskInspection::getOrderId, relation.order() == null ? null : relation.order().getOrderId())
            .set(SfStaskInspection::getWorkItemIdSnapshot,
                relation.order() == null ? null : relation.order().getWorkItemId())
            .set(SfStaskInspection::getWorkItemNameSnapshot,
                relation.order() == null ? null : relation.order().getWorkItemNameSnapshot())
            .set(SfStaskInspection::getResponsibleTechnicianEmployeeId,
                relation.technician().getEmployeeId())
            .set(SfStaskInspection::getResponsibleTechnicianNameSnapshot, relation.technician().getName())
            .set(SfStaskInspection::getResponsibleLeaderEmployeeId,
                relation.leader() == null ? null : relation.leader().getEmployeeId())
            .set(SfStaskInspection::getResponsibleLeaderNameSnapshot,
                relation.leader() == null ? null : relation.leader().getName())
            .set(SfStaskInspection::getVersion, nextVersion(row))
            .set(SfStaskInspection::getUpdateBy, actor.employeeId())
            .set(SfStaskInspection::getUpdateTime, now);
        ensureUpdated(tenantId, row.getInspectionId(), inspectionMapper.update(null, wrapper));
        return mutation(row.getInspectionId(), row.getStatus(), countOrZero(row.getHandleCount()), nextVersion(row));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long inspectionId, SfStaskInspectionDeleteBo bo) {
        String tenantId = requireTenantId();
        ActorCapabilities actor = requireProductionAdmin();
        SfStaskInspection row = requireRecord(tenantId, inspectionId);
        requireOwner(row, actor.employeeId());
        requireVersion(row, bo == null ? null : bo.getVersion());
        if (hasHandleHistory(row) || countOrZero(row.getRejectCount()) > 0) {
            throw messages.stableException(StaskErrorCodes.INSPECTION_HAS_HANDLE_HISTORY, 409,
                StaskMessageKeys.ERROR_INSPECTION_HAS_HANDLE_HISTORY);
        }
        if (!SfStaskInspectionStatus.UNPROCESSED.equals(row.getStatus())) {
            throw statusChanged();
        }
        LambdaUpdateWrapper<SfStaskInspection> wrapper = baseUpdateWrapper(tenantId, row, actor.employeeId());
        wrapper.set(SfStaskInspection::getDelFlag, DELETED)
            .set(SfStaskInspection::getDeletedBy, actor.employeeId())
            .set(SfStaskInspection::getDeletedAt, new Date())
            .set(SfStaskInspection::getVersion, nextVersion(row));
        ensureUpdated(tenantId, row.getInspectionId(), inspectionMapper.update(null, wrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SfStaskInspectionMutationVo handle(Long inspectionId, SfStaskInspectionHandleBo bo) {
        String tenantId = requireTenantId();
        ActorCapabilities actor = requireTechnician();
        SfStaskInspection row = requireRecord(tenantId, inspectionId);
        if (!Objects.equals(row.getResponsibleTechnicianEmployeeId(), actor.employeeId())) {
            throw messages.stableException(StaskErrorCodes.INSPECTION_NOT_RESPONSIBLE_TECHNICIAN, 403,
                StaskMessageKeys.ERROR_INSPECTION_NOT_RESPONSIBLE_TECHNICIAN);
        }
        requireVersion(row, bo == null ? null : bo.getVersion());
        if (!SfStaskInspectionStatus.UNPROCESSED.equals(row.getStatus())) {
            throw statusChanged();
        }
        SfStaskInspectionHandleBo input = bo == null ? new SfStaskInspectionHandleBo() : bo;
        String description = validateText(input.getHandleDescription(), "处理说明");
        String photoJson = photoNormalizer.normalize(tenantId, input.getHandlePhotoJson(), "处理照片");
        SysEmployeeVo employee = requireEmployee(tenantId, actor.employeeId(), EmployeeConstants.APP_ROLE_STASK_EXPERT);
        int handleCount = countOrZero(row.getHandleCount()) + 1;
        long version = nextVersion(row);
        Date now = new Date();
        LambdaUpdateWrapper<SfStaskInspection> wrapper = baseUpdateWrapper(tenantId, row, actor.employeeId());
        wrapper.set(SfStaskInspection::getStatus, SfStaskInspectionStatus.PROCESSED)
            .set(SfStaskInspection::getCurrentHandleDescription, description)
            .set(SfStaskInspection::getCurrentHandlePhotoJson, photoJson)
            .set(SfStaskInspection::getCurrentHandleEmployeeId, employee.getEmployeeId())
            .set(SfStaskInspection::getCurrentHandleEmployeeNameSnapshot, employee.getName())
            .set(SfStaskInspection::getCurrentHandledAt, now)
            .set(SfStaskInspection::getHandleCount, handleCount)
            .set(SfStaskInspection::getVersion, version);
        if (countOrZero(row.getHandleCount()) == 0) {
            wrapper.set(SfStaskInspection::getFirstHandleDescription, description)
                .set(SfStaskInspection::getFirstHandlePhotoJson, photoJson)
                .set(SfStaskInspection::getFirstHandleEmployeeId, employee.getEmployeeId())
                .set(SfStaskInspection::getFirstHandleEmployeeNameSnapshot, employee.getName())
                .set(SfStaskInspection::getFirstHandledAt, now);
        }
        ensureUpdated(tenantId, row.getInspectionId(), inspectionMapper.update(null, wrapper));
        return mutation(row.getInspectionId(), SfStaskInspectionStatus.PROCESSED, handleCount, version);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SfStaskInspectionMutationVo reject(Long inspectionId, SfStaskInspectionRejectBo bo) {
        String tenantId = requireTenantId();
        ActorCapabilities actor = requireProductionAdmin();
        SfStaskInspection row = requireRecord(tenantId, inspectionId);
        requireVersion(row, bo == null ? null : bo.getVersion());
        if (!SfStaskInspectionStatus.PROCESSED.equals(row.getStatus())) {
            throw statusChanged();
        }
        SfStaskInspectionRejectBo input = bo == null ? new SfStaskInspectionRejectBo() : bo;
        String reason = validateText(input.getRejectReason(), "打回原因");
        SysEmployeeVo employee = requireEmployee(tenantId, actor.employeeId(),
            EmployeeConstants.APP_ROLE_STASK_PRODUCTION_ADMIN);
        int rejectCount = countOrZero(row.getRejectCount()) + 1;
        long version = nextVersion(row);
        LambdaUpdateWrapper<SfStaskInspection> wrapper = baseUpdateWrapper(tenantId, row, actor.employeeId());
        wrapper.set(SfStaskInspection::getStatus, SfStaskInspectionStatus.UNPROCESSED)
            .set(SfStaskInspection::getLatestRejectReason, reason)
            .set(SfStaskInspection::getLatestRejectEmployeeId, employee.getEmployeeId())
            .set(SfStaskInspection::getLatestRejectEmployeeNameSnapshot, employee.getName())
            .set(SfStaskInspection::getLatestRejectedAt, new Date())
            .set(SfStaskInspection::getRejectCount, rejectCount)
            .set(SfStaskInspection::getVersion, version);
        ensureUpdated(tenantId, row.getInspectionId(), inspectionMapper.update(null, wrapper));
        return mutation(row.getInspectionId(), SfStaskInspectionStatus.UNPROCESSED,
            countOrZero(row.getHandleCount()), version);
    }

    @Override
    public List<SfStaskInspectionGreenhouseGroupVo> greenhouseGroups(
        SfStaskInspectionGreenhouseGroupQueryBo bo) {
        String tenantId = requireTenantId();
        String keyword = normalizeKeyword(bo == null ? null : bo.getKeyword());
        List<SfField> fields = queryGreenhouses(tenantId, keyword);
        if (fields.isEmpty()) {
            return List.of();
        }
        List<SfFieldVo> fieldVos = fields.stream().map(this::toFieldVo).toList();
        Map<Long, SfPlantingBatchVo> batchMap = activeBatchMap(fields.stream()
            .map(SfField::getFieldId).toList());
        GreenhouseLayoutVo layout = greenhouseLayoutService.resolveLayout(fieldVos);
        Map<Long, String> groupIds = new LinkedHashMap<>();
        Map<Long, String> groupNames = new LinkedHashMap<>();
        Map<Long, Integer> groupOrders = new LinkedHashMap<>();
        if (layout != null) {
            for (GreenhouseLayoutVo.Column column : CollUtil.emptyIfNull(layout.getColumns())) {
                String groupId = "layout-column-" + column.getColumnOrder();
                for (GreenhouseLayoutVo.Row row : CollUtil.emptyIfNull(column.getRows())) {
                    groupIds.put(row.getFieldId(), groupId);
                    groupNames.put(row.getFieldId(), column.getColumnName());
                    groupOrders.put(row.getFieldId(), column.getColumnOrder());
                }
            }
            for (GreenhouseLayoutVo.Row row : CollUtil.emptyIfNull(layout.getUnplacedRows())) {
                groupIds.put(row.getFieldId(), "ungrouped");
                groupNames.put(row.getFieldId(), messages.message(StaskMessageKeys.LABEL_INSPECTION_UNGROUPED));
                groupOrders.put(row.getFieldId(), Integer.MAX_VALUE);
            }
        }
        Map<String, SfStaskInspectionGreenhouseGroupVo> groups = new LinkedHashMap<>();
        for (SfField field : fields) {
            Long fieldId = field.getFieldId();
            String groupId = groupIds.getOrDefault(fieldId, "ungrouped");
            String groupName = groupNames.getOrDefault(fieldId,
                messages.message(StaskMessageKeys.LABEL_INSPECTION_UNGROUPED));
            int groupOrder = groupOrders.getOrDefault(fieldId, Integer.MAX_VALUE);
            SfStaskInspectionGreenhouseGroupVo group = groups.computeIfAbsent(groupId, key -> {
                SfStaskInspectionGreenhouseGroupVo value = new SfStaskInspectionGreenhouseGroupVo();
                value.setGroupId(key);
                value.setGroupName(groupName);
                value.setGroupOrder(groupOrder);
                value.setGreenhouses(new ArrayList<>());
                return value;
            });
            SfStaskInspectionGreenhouseFieldVo item = new SfStaskInspectionGreenhouseFieldVo();
            item.setGreenhouseId(fieldId);
            item.setGreenhouseCode(field.getFieldCode());
            item.setGreenhouseName(field.getFieldName());
            SfPlantingBatchVo batch = batchMap.get(fieldId);
            item.setCropId(batch == null ? null : batch.getSpeciesId());
            item.setCropName(batch == null ? null : batch.getSpeciesName());
            group.getGreenhouses().add(item);
        }
        return groups.values().stream()
            .sorted(Comparator.comparing(SfStaskInspectionGreenhouseGroupVo::getGroupOrder)
                .thenComparing(SfStaskInspectionGreenhouseGroupVo::getGroupId))
            .toList();
    }

    @Override
    public SfStaskInspectionSnapshotVo greenhouseSnapshot(Long greenhouseId) {
        String tenantId = requireTenantId();
        SfField field = requireGreenhouse(tenantId, greenhouseId);
        SfPlantingBatchVo batch = findActiveBatch(greenhouseId);
        SfStaskInspectionSnapshotVo vo = new SfStaskInspectionSnapshotVo();
        vo.setGreenhouseId(field.getFieldId());
        vo.setGreenhouseName(field.getFieldName());
        vo.setPlantingId(batchId(batch));
        vo.setCropId(cropId(batch));
        vo.setCropName(cropName(batch));
        vo.setVarietyId(varietyId(batch));
        vo.setVarietyName(varietyName(batch));
        vo.setPlantingStatus(batch == null ? null : batch.getBatchStatus());
        return vo;
    }

    @Override
    public PageResult<SfStaskInspectionOrderOptionVo> orderOptions(
        SfStaskInspectionOrderQueryBo bo, PageQuery pageQuery) {
        String tenantId = requireTenantId();
        if (bo == null || bo.getGreenhouseId() == null) {
            throw invalid("大棚不能为空");
        }
        requireGreenhouse(tenantId, bo.getGreenhouseId());
        String keyword = normalizeKeyword(bo.getKeyword());
        LambdaQueryWrapper<SfStaskWorkOrder> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SfStaskWorkOrder::getTenantId, tenantId)
            .eq(SfStaskWorkOrder::getGreenhouseId, bo.getGreenhouseId())
            .in(SfStaskWorkOrder::getStatus, LINKABLE_ORDER_STATUSES)
            .and(w -> w.and(x -> x.eq(SfStaskWorkOrder::getCreatorRoleCode, StaskCreatorRole.PRODUCTION_ADMIN)
                    .isNotNull(SfStaskWorkOrder::getHandlerTechnicianEmployeeId))
                .or(x -> x.eq(SfStaskWorkOrder::getCreatorRoleCode, StaskCreatorRole.EXPERT)
                    .isNotNull(SfStaskWorkOrder::getCreatorEmployeeId)))
            .isNotNull(SfStaskWorkOrder::getLeaderId)
            .and(StringUtils.isNotBlank(keyword), w -> w.like(SfStaskWorkOrder::getWorkItemNameSnapshot, keyword)
                .or().like(SfStaskWorkOrder::getOrderNo, keyword))
            .orderByDesc(SfStaskWorkOrder::getPlanDate)
            .orderByDesc(SfStaskWorkOrder::getOrderId);
        List<SfStaskWorkOrder> orders = workOrderMapper.selectList(wrapper);
        Set<Long> employeeIds = orders.stream()
            .flatMap(order -> java.util.stream.Stream.of(effectiveTechnicianId(order), order.getLeaderId()))
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, SysEmployeeVo> employees = employeeMap(tenantId, employeeIds);
        List<SfStaskWorkOrder> validOrders = orders.stream()
            .filter(order -> validOrderResponsibility(order, employees))
            .toList();
        Page<?> requestedPage = buildPage(pageQuery);
        int from = Math.toIntExact(Math.min((requestedPage.getCurrent() - 1) * requestedPage.getSize(), validOrders.size()));
        int to = Math.min(from + Math.toIntExact(requestedPage.getSize()), validOrders.size());
        List<SfStaskInspectionOrderOptionVo> rows = validOrders.subList(from, to).stream()
            .map(order -> toOrderOption(order, employees))
            .toList();
        Page<SfStaskInspectionOrderOptionVo> result = new Page<>(requestedPage.getCurrent(), requestedPage.getSize(),
            validOrders.size());
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(rows, result);
    }

    @Override
    public List<SfStaskInspectionEmployeeOptionVo> technicianOptions(String keyword) {
        return employeeOptions(employeeAccessor.queryOptionsByRole(EmployeeConstants.APP_ROLE_STASK_EXPERT,
            normalizeKeyword(keyword), List.of()), EmployeeConstants.APP_ROLE_STASK_EXPERT);
    }

    @Override
    public List<SfStaskInspectionEmployeeOptionVo> leaderOptions(String keyword) {
        return employeeOptions(employeeAccessor.queryLeaderOptions(normalizeKeyword(keyword), SystemConstants.NORMAL),
            EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER);
    }

    private SfStaskInspectionCreateBo requireCreateBo(SfStaskInspectionCreateBo bo) {
        if (bo == null) {
            throw messages.stableException(StaskErrorCodes.INSPECTION_PARAM_INVALID, 400,
                StaskMessageKeys.ERROR_COMMON_REQUEST_REQUIRED, "请求体");
        }
        return bo;
    }

    private Relation resolveRelation(String tenantId, SfField greenhouse, Long orderId,
        Long requestedTechnicianId, Long requestedLeaderId) {
        return resolveRelation(tenantId, greenhouse, orderId, requestedTechnicianId, requestedLeaderId, false);
    }

    private Relation resolveRelation(String tenantId, SfField greenhouse, Long orderId,
        Long requestedTechnicianId, Long requestedLeaderId, boolean allowCancelledExistingOrder) {
        SfPlantingBatchVo batch = findActiveBatch(greenhouse.getFieldId());
        if (orderId == null) {
            SysEmployeeVo technician = requireEmployee(tenantId, requestedTechnicianId,
                EmployeeConstants.APP_ROLE_STASK_EXPERT);
            SysEmployeeVo leader = requestedLeaderId == null ? null
                : requireEmployee(tenantId, requestedLeaderId, EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER);
            return new Relation(greenhouse, batch, null, technician, leader);
        }
        SfStaskWorkOrder order = workOrderMapper.selectOne(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .eq(SfStaskWorkOrder::getOrderId, orderId));
        if (order == null) {
            throw messages.stableException(StaskErrorCodes.CROSS_TENANT_FORBIDDEN, 403,
                StaskMessageKeys.ERROR_INSPECTION_NOT_FOUND);
        }
        boolean cancelledExistingOrder = allowCancelledExistingOrder
            && (StaskOrderStatus.CANCELLED.equals(order.getStatus())
            || StaskOrderStatus.VOIDED.equals(order.getStatus()));
        if (!Objects.equals(order.getGreenhouseId(), greenhouse.getFieldId())
            || (!LINKABLE_ORDER_STATUSES.contains(order.getStatus()) && !cancelledExistingOrder)
            || order.getPackageId() == null) {
            throw messages.stableException(StaskErrorCodes.INSPECTION_ORDER_INVALID, 409,
                StaskMessageKeys.ERROR_INSPECTION_ORDER_INVALID);
        }
        Long technicianId = effectiveTechnicianId(order);
        if (technicianId == null || order.getLeaderId() == null) {
            throw messages.stableException(StaskErrorCodes.INSPECTION_ORDER_INVALID, 409,
                StaskMessageKeys.ERROR_INSPECTION_ORDER_INVALID);
        }
        SysEmployeeVo technician = requireEmployee(tenantId, technicianId, EmployeeConstants.APP_ROLE_STASK_EXPERT);
        SysEmployeeVo leader = requireEmployee(tenantId, order.getLeaderId(), EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER);
        if ((requestedTechnicianId != null && !Objects.equals(requestedTechnicianId, technicianId))
            || (requestedLeaderId != null && !Objects.equals(requestedLeaderId, order.getLeaderId()))) {
            throw messages.stableException(StaskErrorCodes.INSPECTION_OWNER_MISMATCH, 409,
                StaskMessageKeys.ERROR_INSPECTION_OWNER_MISMATCH);
        }
        return new Relation(greenhouse, batch, order, technician, leader);
    }

    private void applyRelation(SfStaskInspection row, Relation relation) {
        row.setGreenhouseId(relation.greenhouse().getFieldId());
        row.setGreenhouseNameSnapshot(relation.greenhouse().getFieldName());
        row.setPlantingBatchIdSnapshot(batchId(relation.batch()));
        row.setCropIdSnapshot(cropId(relation.batch()));
        row.setCropNameSnapshot(cropName(relation.batch()));
        row.setCropVarietyIdSnapshot(varietyId(relation.batch()));
        row.setCropVarietyNameSnapshot(varietyName(relation.batch()));
        row.setOrderId(relation.order() == null ? null : relation.order().getOrderId());
        row.setWorkItemIdSnapshot(relation.order() == null ? null : relation.order().getWorkItemId());
        row.setWorkItemNameSnapshot(relation.order() == null ? null : relation.order().getWorkItemNameSnapshot());
        row.setResponsibleTechnicianEmployeeId(relation.technician().getEmployeeId());
        row.setResponsibleTechnicianNameSnapshot(relation.technician().getName());
        row.setResponsibleLeaderEmployeeId(relation.leader() == null ? null : relation.leader().getEmployeeId());
        row.setResponsibleLeaderNameSnapshot(relation.leader() == null ? null : relation.leader().getName());
    }

    private void ensureImmutableRelation(SfStaskInspection row, SfStaskInspectionUpdateBo bo) {
        if (bo == null) {
            return;
        }
        boolean changed = (bo.getFoundDate() != null && !Objects.equals(bo.getFoundDate(), row.getFoundDate()))
            || (bo.getGreenhouseId() != null && !Objects.equals(bo.getGreenhouseId(), row.getGreenhouseId()))
            || (bo.getOrderId() != null && !Objects.equals(bo.getOrderId(), row.getOrderId()))
            || (bo.getResponsibleTechnicianEmployeeId() != null
            && !Objects.equals(bo.getResponsibleTechnicianEmployeeId(), row.getResponsibleTechnicianEmployeeId()))
            || (bo.getResponsibleLeaderEmployeeId() != null
            && !Objects.equals(bo.getResponsibleLeaderEmployeeId(), row.getResponsibleLeaderEmployeeId()));
        if (changed) {
            throw messages.stableException(StaskErrorCodes.INSPECTION_HAS_HANDLE_HISTORY, 409,
                StaskMessageKeys.ERROR_INSPECTION_HAS_HANDLE_HISTORY);
        }
    }

    private void updateContent(String tenantId, SfStaskInspection row, String severity, String description,
        String photoJson, Long actorId) {
        LambdaUpdateWrapper<SfStaskInspection> wrapper = baseUpdateWrapper(tenantId, row, actorId)
            .set(SfStaskInspection::getSeverity, severity)
            .set(SfStaskInspection::getProblemDescription, description)
            .set(SfStaskInspection::getProblemPhotoJson, photoJson)
            .set(SfStaskInspection::getVersion, nextVersion(row));
        ensureUpdated(tenantId, row.getInspectionId(), inspectionMapper.update(null, wrapper));
    }

    private LambdaUpdateWrapper<SfStaskInspection> baseUpdateWrapper(String tenantId, SfStaskInspection row,
        Long actorId) {
        return Wrappers.<SfStaskInspection>lambdaUpdate()
            .eq(SfStaskInspection::getTenantId, tenantId)
            .eq(SfStaskInspection::getInspectionId, row.getInspectionId())
            .eq(SfStaskInspection::getDelFlag, SystemConstants.NORMAL)
            .eq(SfStaskInspection::getVersion, currentVersion(row));
    }

    private void ensureUpdated(String tenantId, Long inspectionId, int affectedRows) {
        if (affectedRows == 0) {
            SfStaskInspection current = inspectionMapper.selectIncludingDeleted(tenantId, inspectionId);
            if (current == null) {
                throw messages.stableException(StaskErrorCodes.INSPECTION_NOT_FOUND, 404,
                    StaskMessageKeys.ERROR_INSPECTION_NOT_FOUND);
            }
            if (DELETED.equals(current.getDelFlag())) {
                throw messages.stableException(StaskErrorCodes.INSPECTION_DELETED, 410,
                    StaskMessageKeys.ERROR_INSPECTION_DELETED);
            }
            throw statusChanged();
        }
    }

    private SfStaskInspection requireRecord(String tenantId, Long inspectionId) {
        if (inspectionId == null) {
            throw messages.stableException(StaskErrorCodes.INSPECTION_PARAM_INVALID, 400,
                StaskMessageKeys.ERROR_INSPECTION_PARAM_INVALID, "抽检ID");
        }
        SfStaskInspection row = inspectionMapper.selectOne(Wrappers.<SfStaskInspection>lambdaQuery()
            .eq(SfStaskInspection::getTenantId, tenantId)
            .eq(SfStaskInspection::getInspectionId, inspectionId)
            .eq(SfStaskInspection::getDelFlag, SystemConstants.NORMAL));
        if (row != null) {
            return row;
        }
        SfStaskInspection deletedRow = inspectionMapper.selectIncludingDeleted(tenantId, inspectionId);
        if (deletedRow != null && DELETED.equals(deletedRow.getDelFlag())) {
            throw messages.stableException(StaskErrorCodes.INSPECTION_DELETED, 410,
                StaskMessageKeys.ERROR_INSPECTION_DELETED);
        }
        throw messages.stableException(StaskErrorCodes.INSPECTION_NOT_FOUND, 404,
            StaskMessageKeys.ERROR_INSPECTION_NOT_FOUND);
    }

    private SfField requireGreenhouse(String tenantId, Long greenhouseId) {
        if (greenhouseId == null) {
            throw invalid("大棚不能为空");
        }
        SfField field = fieldMapper.selectOne(Wrappers.<SfField>lambdaQuery()
            .eq(SfField::getTenantId, tenantId)
            .eq(SfField::getFieldId, greenhouseId)
            .eq(SfField::getFieldType, FieldType.GREENHOUSE)
            .eq(SfField::getStatus, SystemConstants.NORMAL)
            .eq(SfField::getDelFlag, SystemConstants.NORMAL));
        if (field == null) {
            throw messages.stableException(StaskErrorCodes.INSPECTION_GREENHOUSE_CHANGED, 409,
                StaskMessageKeys.ERROR_INSPECTION_GREENHOUSE_CHANGED);
        }
        return field;
    }

    private List<SfField> queryGreenhouses(String tenantId, String keyword) {
        return fieldMapper.selectList(Wrappers.<SfField>lambdaQuery()
            .eq(SfField::getTenantId, tenantId)
            .eq(SfField::getFieldType, FieldType.GREENHOUSE)
            .eq(SfField::getStatus, SystemConstants.NORMAL)
            .eq(SfField::getDelFlag, SystemConstants.NORMAL)
            .and(StringUtils.isNotBlank(keyword), w -> w.like(SfField::getFieldName, keyword)
                .or().like(SfField::getFieldCode, keyword))
            .orderByAsc(SfField::getSortOrder)
            .orderByAsc(SfField::getFieldId));
    }

    private SfPlantingBatchVo findActiveBatch(Long greenhouseId) {
        Map<Long, SfPlantingBatchVo> map = activeBatchMap(List.of(greenhouseId));
        return map.get(greenhouseId);
    }

    private Map<Long, SfPlantingBatchVo> activeBatchMap(Collection<Long> greenhouseIds) {
        if (greenhouseIds == null || greenhouseIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<SfPlantingBatchVo>> source = plantingBatchService.mapActiveVoByFieldIds(greenhouseIds);
        Map<Long, SfPlantingBatchVo> result = new LinkedHashMap<>();
        for (Long greenhouseId : greenhouseIds) {
            List<SfPlantingBatchVo> batches = source == null ? List.of() : source.getOrDefault(greenhouseId, List.of());
            batches.stream()
                .filter(batch -> PlantingBatchStatus.isActive(batch.getBatchStatus()))
                .min(Comparator.comparing(SfPlantingBatchVo::getBatchStatus, PlantingBatchStatus.ACTIVE_PRIORITY)
                    .thenComparing(SfPlantingBatchVo::getBatchId,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .ifPresent(batch -> result.put(greenhouseId, batch));
        }
        return result;
    }

    private Map<Long, SysEmployeeVo> employeeMap(String tenantId, Collection<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return Map.of();
        }
        return CollUtil.emptyIfNull(employeeAccessor.queryByIds(employeeIds)).stream()
            .filter(employee -> Objects.equals(employee.getTenantId(), tenantId))
            .collect(Collectors.toMap(SysEmployeeVo::getEmployeeId, Function.identity(), (left, right) -> left,
                LinkedHashMap::new));
    }

    private SysEmployeeVo requireEmployee(String tenantId, Long employeeId, String roleCode) {
        if (employeeId == null) {
            throw messages.stableException(StaskErrorCodes.INSPECTION_PARAM_INVALID, 400,
                StaskMessageKeys.ERROR_INSPECTION_PARAM_INVALID, "负责人员");
        }
        SysEmployeeVo employee = employeeMap(tenantId, List.of(employeeId)).get(employeeId);
        if (employee == null || !isActiveEmployee(employee)
            || (roleCode != null && !Objects.equals(roleCode, employee.getAppRoleCode()))) {
            throw messages.stableException(StaskErrorCodes.INSPECTION_ACTION_FORBIDDEN, 403,
                StaskMessageKeys.ERROR_INSPECTION_EMPLOYEE_INVALID);
        }
        return employee;
    }

    private void validateQuery(String tenantId, SfStaskInspectionQueryBo query) {
        if (query.getFoundStartDate() != null && query.getFoundEndDate() != null
            && query.getFoundStartDate().isAfter(query.getFoundEndDate())) {
            throw messages.stableException(StaskErrorCodes.INSPECTION_PARAM_INVALID, 400,
                StaskMessageKeys.ERROR_INSPECTION_DATE_INVALID);
        }
        if (StringUtils.isNotBlank(query.getStatus())
            && !Set.of(SfStaskInspectionStatus.UNPROCESSED, SfStaskInspectionStatus.PROCESSED)
            .contains(query.getStatus())) {
            throw messages.stableException(StaskErrorCodes.INSPECTION_PARAM_INVALID, 400,
                StaskMessageKeys.ERROR_INSPECTION_PARAM_INVALID, "处理状态");
        }
        if (query.getGreenhouseId() != null) {
            validateGreenhouseFilter(tenantId, query.getGreenhouseId());
        }
        validateEmployeeFilter(tenantId, query.getResponsibleTechnicianEmployeeId());
        validateEmployeeFilter(tenantId, query.getResponsibleLeaderEmployeeId());
    }

    private void validateGreenhouseFilter(String tenantId, Long greenhouseId) {
        SfField field = fieldMapper.selectOne(Wrappers.<SfField>lambdaQuery()
            .eq(SfField::getTenantId, tenantId)
            .eq(SfField::getFieldId, greenhouseId)
            .eq(SfField::getDelFlag, SystemConstants.NORMAL));
        if (field == null) {
            throw messages.stableException(StaskErrorCodes.CROSS_TENANT_FORBIDDEN, 403,
                StaskMessageKeys.ERROR_INSPECTION_NOT_FOUND);
        }
        if (!FieldType.GREENHOUSE.equals(field.getFieldType())) {
            throw invalid("大棚");
        }
    }

    private void validateEmployeeFilter(String tenantId, Long employeeId) {
        if (employeeId == null) {
            return;
        }
        if (employeeMap(tenantId, List.of(employeeId)).isEmpty()) {
            throw messages.stableException(StaskErrorCodes.CROSS_TENANT_FORBIDDEN, 403,
                StaskMessageKeys.ERROR_INSPECTION_NOT_FOUND);
        }
    }

    private boolean validOrderResponsibility(SfStaskWorkOrder order, Map<Long, SysEmployeeVo> employees) {
        Long technicianId = effectiveTechnicianId(order);
        SysEmployeeVo technician = employees.get(technicianId);
        SysEmployeeVo leader = employees.get(order.getLeaderId());
        return technician != null && leader != null
            && isActiveEmployee(technician)
            && isActiveEmployee(leader)
            && EmployeeConstants.APP_ROLE_STASK_EXPERT.equals(technician.getAppRoleCode())
            && EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER.equals(leader.getAppRoleCode());
    }

    private Long effectiveTechnicianId(SfStaskWorkOrder order) {
        if (order == null) {
            return null;
        }
        if (StaskCreatorRole.PRODUCTION_ADMIN.equals(order.getCreatorRoleCode())) {
            return order.getHandlerTechnicianEmployeeId();
        }
        if (StaskCreatorRole.EXPERT.equals(order.getCreatorRoleCode())) {
            return order.getCreatorEmployeeId();
        }
        return null;
    }

    private boolean isActiveEmployee(SysEmployeeVo employee) {
        return employee != null
            && SystemConstants.NORMAL.equals(employee.getStatus())
            && (employee.getReviewStatus() == null
            || EmployeeConstants.REVIEW_APPROVED.equals(employee.getReviewStatus()));
    }

    private SfStaskInspectionOrderOptionVo toOrderOption(SfStaskWorkOrder order,
        Map<Long, SysEmployeeVo> employees) {
        SfStaskInspectionOrderOptionVo vo = new SfStaskInspectionOrderOptionVo();
        vo.setOrderId(order.getOrderId());
        vo.setOrderName(StringUtils.isBlank(order.getOrderNo())
            ? String.valueOf(order.getOrderId()) : order.getOrderNo());
        vo.setWorkItemId(order.getWorkItemId());
        vo.setWorkItemName(order.getWorkItemNameSnapshot());
        Long technicianId = effectiveTechnicianId(order);
        SysEmployeeVo technician = employees.get(technicianId);
        SysEmployeeVo leader = employees.get(order.getLeaderId());
        vo.setResponsibleTechnicianEmployeeId(technicianId);
        vo.setResponsibleTechnicianName(technician.getName());
        vo.setResponsibleLeaderEmployeeId(order.getLeaderId());
        vo.setResponsibleLeaderName(leader.getName());
        vo.setOrderStatus(order.getStatus());
        vo.setOrderStatusLabel(orderStatusLabel(order.getStatus()));
        vo.setPlannedDate(order.getPlanDate());
        return vo;
    }

    private List<SfStaskInspectionEmployeeOptionVo> employeeOptions(
        List<SysEmployeeLeaderOptionVo> source, String roleCode) {
        return CollUtil.emptyIfNull(source).stream().map(employee -> {
            SfStaskInspectionEmployeeOptionVo vo = new SfStaskInspectionEmployeeOptionVo();
            vo.setEmployeeId(employee.getEmployeeId());
            vo.setEmployeeName(employee.getName());
            vo.setRoleCode(roleCode);
            vo.setRoleName(roleLabel(roleCode));
            vo.setActive(SystemConstants.NORMAL.equals(employee.getStatus()));
            return vo;
        }).toList();
    }

    private SfStaskInspectionListVo toListVo(SfStaskInspection row, ActorCapabilities actor) {
        SfStaskInspectionListVo vo = new SfStaskInspectionListVo();
        vo.setInspectionId(row.getInspectionId());
        vo.setFoundDate(row.getFoundDate());
        vo.setGreenhouseId(row.getGreenhouseId());
        vo.setGreenhouseNameSnapshot(row.getGreenhouseNameSnapshot());
        vo.setPlantingBatchIdSnapshot(row.getPlantingBatchIdSnapshot());
        vo.setCropIdSnapshot(row.getCropIdSnapshot());
        vo.setCropNameSnapshot(row.getCropNameSnapshot());
        vo.setCropVarietyIdSnapshot(row.getCropVarietyIdSnapshot());
        vo.setCropVarietyNameSnapshot(row.getCropVarietyNameSnapshot());
        vo.setOrderId(row.getOrderId());
        vo.setWorkItemIdSnapshot(row.getWorkItemIdSnapshot());
        vo.setWorkItemNameSnapshot(row.getWorkItemNameSnapshot());
        vo.setResponsibleTechnicianEmployeeId(row.getResponsibleTechnicianEmployeeId());
        vo.setResponsibleTechnicianNameSnapshot(row.getResponsibleTechnicianNameSnapshot());
        vo.setResponsibleLeaderEmployeeId(row.getResponsibleLeaderEmployeeId());
        vo.setResponsibleLeaderNameSnapshot(row.getResponsibleLeaderNameSnapshot());
        vo.setDiscovererEmployeeId(row.getDiscovererEmployeeId());
        vo.setDiscovererNameSnapshot(row.getDiscovererNameSnapshot());
        vo.setSeverity(row.getSeverity());
        vo.setSeverityLabel(severityLabel(row.getSeverity()));
        vo.setStatus(row.getStatus());
        vo.setStatusLabel(statusLabel(row.getStatus()));
        vo.setProblemDescription(row.getProblemDescription());
        vo.setProblemPhotoCount(photoNormalizer.count(row.getProblemPhotoJson()));
        vo.setUpdateTime(com.ym.agriculture.shared.common.AgricultureTimes.toDate(
            row.getUpdateTime() == null ? row.getCreateTime() : row.getUpdateTime()));
        vo.setVersion(currentVersion(row));
        boolean owner = actor.productionAdmin() && Objects.equals(actor.employeeId(), row.getCreatorEmployeeId());
        boolean history = hasHandleHistory(row);
        vo.setCanCreate(actor.productionAdmin());
        vo.setCanEdit(owner);
        vo.setCanDelete(owner && !history && SfStaskInspectionStatus.UNPROCESSED.equals(row.getStatus())
            && countOrZero(row.getRejectCount()) == 0);
        vo.setCanHandle(actor.technician()
            && Objects.equals(actor.employeeId(), row.getResponsibleTechnicianEmployeeId())
            && SfStaskInspectionStatus.UNPROCESSED.equals(row.getStatus()));
        vo.setCanReject(actor.productionAdmin() && SfStaskInspectionStatus.PROCESSED.equals(row.getStatus()));
        vo.setReadonlyReason(readonlyReason(actor, row, owner, history));
        return vo;
    }

    private String readonlyReason(ActorCapabilities actor, SfStaskInspection row, boolean owner, boolean history) {
        if (actor.productionAdmin() && owner) {
            return history ? messages.message(StaskMessageKeys.LABEL_INSPECTION_HISTORY_IMMUTABLE) : null;
        }
        if (actor.productionAdmin() && SfStaskInspectionStatus.PROCESSED.equals(row.getStatus())) {
            return null;
        }
        if (actor.technician() && Objects.equals(actor.employeeId(), row.getResponsibleTechnicianEmployeeId())
            && SfStaskInspectionStatus.UNPROCESSED.equals(row.getStatus())) {
            return null;
        }
        if (actor.productionAdmin()) {
            return messages.message(StaskMessageKeys.LABEL_INSPECTION_OWNER_ONLY);
        }
        if (actor.technician() && !Objects.equals(actor.employeeId(), row.getResponsibleTechnicianEmployeeId())) {
            return messages.message(StaskMessageKeys.LABEL_INSPECTION_HANDLE_ONLY);
        }
        if (actor.technician() && SfStaskInspectionStatus.PROCESSED.equals(row.getStatus())) {
            return messages.message(StaskMessageKeys.LABEL_INSPECTION_ALREADY_PROCESSED);
        }
        if (actor.leader()) {
            return messages.message(StaskMessageKeys.LABEL_INSPECTION_READONLY);
        }
        return messages.message(StaskMessageKeys.LABEL_INSPECTION_READONLY);
    }

    private SfStaskInspectionDetailVo.HandleVo toHandleVo(String tenantId, String description, String photoJson,
        Long employeeId, String employeeName, Date handledAt) {
        if (employeeId == null && StrUtil.isBlank(description) && StrUtil.isBlank(photoJson)) {
            return null;
        }
        SfStaskInspectionDetailVo.HandleVo vo = new SfStaskInspectionDetailVo.HandleVo();
        vo.setDescription(description);
        vo.setPhotoJson(photoNormalizer.render(tenantId, photoJson));
        vo.setEmployeeId(employeeId);
        vo.setEmployeeNameSnapshot(employeeName);
        vo.setHandledAt(handledAt);
        return vo;
    }

    private SfStaskInspectionDetailVo.RejectVo toRejectVo(SfStaskInspection row) {
        if (row.getLatestRejectEmployeeId() == null && StrUtil.isBlank(row.getLatestRejectReason())) {
            return null;
        }
        SfStaskInspectionDetailVo.RejectVo vo = new SfStaskInspectionDetailVo.RejectVo();
        vo.setReason(row.getLatestRejectReason());
        vo.setEmployeeId(row.getLatestRejectEmployeeId());
        vo.setEmployeeNameSnapshot(row.getLatestRejectEmployeeNameSnapshot());
        vo.setRejectedAt(row.getLatestRejectedAt());
        return vo;
    }

    private SfStaskInspectionMutationVo mutation(Long inspectionId, String status, Integer handleCount,
        Long version) {
        SfStaskInspectionMutationVo vo = new SfStaskInspectionMutationVo();
        vo.setInspectionId(inspectionId);
        vo.setStatus(status);
        vo.setStatusLabel(statusLabel(status));
        vo.setHandleCount(countOrZero(handleCount));
        vo.setVersion(version == null ? 0L : version);
        return vo;
    }

    private ActorCapabilities requireProductionAdmin() {
        ActorCapabilities actor = currentCapabilities();
        if (!actor.productionAdmin()) {
            throw messages.stableException(StaskErrorCodes.INSPECTION_ACTION_FORBIDDEN, 403,
                StaskMessageKeys.ERROR_INSPECTION_ACTION_FORBIDDEN);
        }
        return actor;
    }

    private ActorCapabilities requireTechnician() {
        ActorCapabilities actor = currentCapabilities();
        if (!actor.technician()) {
            throw messages.stableException(StaskErrorCodes.INSPECTION_ACTION_FORBIDDEN, 403,
                StaskMessageKeys.ERROR_INSPECTION_ACTION_FORBIDDEN);
        }
        return actor;
    }

    private ActorCapabilities currentCapabilities() {
        Long employeeId = LoginHelper.getUserId();
        if (employeeId == null) {
            return new ActorCapabilities(null, false, false, false);
        }
        return new ActorCapabilities(employeeId,
            employeeAccessor.hasAppRole(employeeId, EmployeeConstants.APP_ROLE_STASK_PRODUCTION_ADMIN),
            employeeAccessor.hasAppRole(employeeId, EmployeeConstants.APP_ROLE_STASK_EXPERT),
            employeeAccessor.hasAppRole(employeeId, EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER));
    }

    private void requireOwner(SfStaskInspection row, Long employeeId) {
        if (!Objects.equals(row.getCreatorEmployeeId(), employeeId)) {
            throw messages.stableException(StaskErrorCodes.INSPECTION_ACTION_FORBIDDEN, 403,
                StaskMessageKeys.ERROR_INSPECTION_ACTION_FORBIDDEN);
        }
    }

    private void requireVersion(SfStaskInspection row, Long version) {
        if (version == null) {
            throw invalid("version");
        }
        if (!Objects.equals(currentVersion(row), version)) {
            throw statusChanged();
        }
    }

    private StaskMiniappException statusChanged() {
        return messages.stableException(StaskErrorCodes.INSPECTION_STATUS_CHANGED, 409,
            StaskMessageKeys.ERROR_INSPECTION_STATUS_CHANGED);
    }

    private StaskMiniappException invalid(String detail) {
        return messages.stableException(StaskErrorCodes.INSPECTION_PARAM_INVALID, 400,
            StaskMessageKeys.ERROR_INSPECTION_PARAM_INVALID, detail);
    }

    private String validateText(String value, String fieldName) {
        String text = StrUtil.trim(value);
        if (StrUtil.isBlank(text)) {
            throw messages.stableException(StaskErrorCodes.INSPECTION_PARAM_INVALID, 400,
                StaskMessageKeys.ERROR_INSPECTION_TEXT_REQUIRED, fieldName);
        }
        if (text.length() > MAX_TEXT_LENGTH) {
            throw messages.stableException(StaskErrorCodes.INSPECTION_PARAM_INVALID, 400,
                StaskMessageKeys.ERROR_INSPECTION_TEXT_TOO_LONG, fieldName);
        }
        return text;
    }

    private String validateSeverity(String severity) {
        if (!Set.of(SfStaskInspectionSeverity.NORMAL, SfStaskInspectionSeverity.SERIOUS).contains(severity)) {
            throw messages.stableException(StaskErrorCodes.INSPECTION_PARAM_INVALID, 400,
                StaskMessageKeys.ERROR_INSPECTION_SEVERITY_INVALID);
        }
        return severity;
    }

    private void validateDate(LocalDate date) {
        if (date == null) {
            throw messages.stableException(StaskErrorCodes.INSPECTION_PARAM_INVALID, 400,
                StaskMessageKeys.ERROR_INSPECTION_DATE_INVALID);
        }
    }

    private String normalizeKeyword(String keyword) {
        String value = StrUtil.trim(keyword);
        if (value != null && value.length() > 50) {
            return value.substring(0, 50);
        }
        return value;
    }

    private String severityLabel(String severity) {
        return SfStaskInspectionSeverity.SERIOUS.equals(severity)
            ? messages.message(StaskMessageKeys.LABEL_INSPECTION_SEVERITY_SERIOUS)
            : messages.message(StaskMessageKeys.LABEL_INSPECTION_SEVERITY_NORMAL);
    }

    private String statusLabel(String status) {
        return SfStaskInspectionStatus.PROCESSED.equals(status)
            ? messages.message(StaskMessageKeys.LABEL_INSPECTION_STATUS_PROCESSED)
            : messages.message(StaskMessageKeys.LABEL_INSPECTION_STATUS_UNPROCESSED);
    }

    private String roleLabel(String roleCode) {
        return switch (roleCode) {
            case EmployeeConstants.APP_ROLE_STASK_PRODUCTION_ADMIN ->
                messages.message(StaskMessageKeys.LABEL_INSPECTION_PRODUCTION_ADMIN);
            case EmployeeConstants.APP_ROLE_STASK_EXPERT ->
                messages.message(StaskMessageKeys.LABEL_INSPECTION_TECHNICIAN);
            case EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER ->
                messages.message(StaskMessageKeys.LABEL_INSPECTION_GROUP_LEADER);
            default -> messages.message(StaskMessageKeys.LABEL_COMMON_UNKNOWN);
        };
    }

    private String orderStatusLabel(String status) {
        return switch (status) {
            case StaskOrderStatus.PENDING_TECH_CONFIRM ->
                messages.message(StaskMessageKeys.LABEL_PACKAGE_STATUS_PENDING_TECH_CONFIRM);
            case StaskOrderStatus.TECH_REJECTED -> messages.message(StaskMessageKeys.LABEL_PACKAGE_STATUS_TECH_REJECTED);
            case StaskOrderStatus.PENDING_LEADER_ACCEPT ->
                messages.message(StaskMessageKeys.LABEL_WORKORDER_STATUS_PENDING_LEADER_ACCEPT);
            case StaskOrderStatus.PENDING_LEADER_ASSIGN ->
                messages.message(StaskMessageKeys.LABEL_WORKORDER_STATUS_PENDING_DISPATCH);
            case StaskOrderStatus.ASSIGN_COMPLETE ->
                messages.message(StaskMessageKeys.LABEL_WORKORDER_STATUS_DISPATCH_COMPLETED);
            case StaskOrderStatus.LEADER_ARRIVED -> messages.message(StaskMessageKeys.LABEL_WORKORDER_STATUS_ARRIVED);
            case StaskOrderStatus.PENDING_ACCEPTANCE ->
                messages.message(StaskMessageKeys.LABEL_WORKORDER_STATUS_PENDING_ACCEPTANCE);
            case StaskOrderStatus.ACCEPTANCE_PASSED ->
                messages.message(StaskMessageKeys.LABEL_WORKORDER_ACCEPTANCE_PASSED);
            case StaskOrderStatus.ACCEPTANCE_REJECTED ->
                messages.message(StaskMessageKeys.LABEL_WORKORDER_ACCEPTANCE_REJECTED);
            default -> status;
        };
    }

    private <T> Page<T> buildPage(PageQuery pageQuery) {
        return pageQuery == null ? new Page<>(1, DEFAULT_OPTION_PAGE_SIZE) : pageQuery.build();
    }

    private String requireTenantId() {
        String tenantId = TenantHelper.getTenantId();
        if (StrUtil.isBlank(tenantId)) {
            throw messages.stableException(StaskErrorCodes.INSPECTION_ACTION_FORBIDDEN, 400,
                StaskMessageKeys.ERROR_COMMON_TENANT_CONTEXT_MISSING);
        }
        return tenantId;
    }

    private int countOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean hasHandleHistory(SfStaskInspection row) {
        return countOrZero(row.getHandleCount()) > 0;
    }

    private Long currentVersion(SfStaskInspection row) {
        return row.getVersion() == null ? 0L : row.getVersion();
    }

    private Long nextVersion(SfStaskInspection row) {
        return currentVersion(row) + 1L;
    }

    private SfFieldVo toFieldVo(SfField field) {
        SfFieldVo vo = new SfFieldVo();
        BeanUtil.copyProperties(field, vo);
        return vo;
    }

    private Long batchId(SfPlantingBatchVo batch) {
        return batch == null ? null : batch.getBatchId();
    }

    private Long cropId(SfPlantingBatchVo batch) {
        return batch == null ? null : batch.getSpeciesId();
    }

    private String cropName(SfPlantingBatchVo batch) {
        return batch == null ? null : batch.getSpeciesName();
    }

    private Long varietyId(SfPlantingBatchVo batch) {
        return batch == null ? null : batch.getVarietyId();
    }

    private String varietyName(SfPlantingBatchVo batch) {
        return batch == null ? null : batch.getVarietyName();
    }

    private record Relation(SfField greenhouse, SfPlantingBatchVo batch, SfStaskWorkOrder order,
                            SysEmployeeVo technician, SysEmployeeVo leader) {
    }

    private record ActorCapabilities(Long employeeId, boolean productionAdmin, boolean technician, boolean leader) {
    }
}
