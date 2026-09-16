package com.ym.agriculture.farmtask.workorder.service.query;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskAcceptanceMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskClockRecordMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskCompletionMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskFlowLogMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskTaskPackageMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderGreenhouseMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderItemMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderMapper;
import com.ym.agriculture.farmtask.leaderlabor.dao.SfStaskLeaderLaborRecordMapper;
import com.ym.agriculture.farmtask.leaderlabor.model.entity.SfStaskLeaderLaborRecord;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskAllTaskQueryBo;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskCreatorRole;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderEvent;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskTaskScope;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskAcceptance;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskClockRecord;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskCompletion;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskFlowLog;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskTaskPackage;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrderGreenhouse;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrderItem;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskAllTaskItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskGreenhouseBriefVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskHomeItemSummaryVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkOrderVo;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeAccessor;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeQueryContext;
import com.ym.agriculture.farmtask.workorder.support.SfStaskRoleNameReader;
import com.ym.agriculture.farmtask.workorder.support.SfStaskWorkOrderAssembler;
import com.ym.agriculture.farmtask.workorder.support.StaskGreenhousePlantingBatchHelper;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.math.BigDecimal;
import java.time.ZoneId;

/**
 * stask 管理员、技术员与组长全部任务查询。
 */
@RequiredArgsConstructor
@Service
public class SfStaskAllTaskQueryService {

    private static final String CARD_PACKAGE = "PACKAGE";
    private static final String CARD_SPLIT = "SPLIT";
    private static final String ACTION_ACCEPT = "ACCEPT";
    private static final int PACKAGE_ITEM_PREVIEW_LIMIT = 2;

    private static final List<String> ALL_TASK_STATUSES = List.of(
        StaskOrderStatus.DRAFT, StaskOrderStatus.PENDING_TECH_CONFIRM, StaskOrderStatus.TECH_REJECTED,
        StaskOrderStatus.PENDING_LEADER_ACCEPT,
        StaskOrderStatus.ASSIGN_COMPLETE, StaskOrderStatus.LEADER_ARRIVED,
        StaskOrderStatus.PENDING_ACCEPTANCE, StaskOrderStatus.ACCEPTANCE_PASSED,
        StaskOrderStatus.ACCEPTANCE_REJECTED, StaskOrderStatus.VOIDED, StaskOrderStatus.CANCELLED);
    private static final List<String> LEADER_TASK_STATUSES = List.of(
        StaskOrderStatus.PENDING_LEADER_ACCEPT,
        StaskOrderStatus.ASSIGN_COMPLETE, StaskOrderStatus.LEADER_ARRIVED,
        StaskOrderStatus.PENDING_ACCEPTANCE, StaskOrderStatus.ACCEPTANCE_PASSED,
        StaskOrderStatus.ACCEPTANCE_REJECTED, StaskOrderStatus.VOIDED);
    private static final List<String> LEADER_ADMIN_TASK_STATUSES = List.of(
        StaskOrderStatus.TECH_REJECTED, StaskOrderStatus.PENDING_TECH_CONFIRM,
        StaskOrderStatus.PENDING_LEADER_ACCEPT,
        StaskOrderStatus.ASSIGN_COMPLETE, StaskOrderStatus.LEADER_ARRIVED,
        StaskOrderStatus.PENDING_ACCEPTANCE, StaskOrderStatus.ACCEPTANCE_PASSED,
        StaskOrderStatus.ACCEPTANCE_REJECTED, StaskOrderStatus.VOIDED, StaskOrderStatus.CANCELLED);
    private static final Set<String> CARD_TYPES = Set.of(CARD_PACKAGE, CARD_SPLIT);

    private final com.ym.agriculture.shared.i18n.StaskMessageResolver messages;
    private final SfStaskWorkOrderMapper workOrderMapper;
    private final SfStaskTaskPackageMapper taskPackageMapper;
    private final SfStaskWorkOrderItemMapper itemMapper;
    private final SfStaskWorkOrderGreenhouseMapper greenhouseMapper;
    private final SfStaskClockRecordMapper clockRecordMapper;
    private final SfStaskCompletionMapper completionMapper;
    private final SfStaskAcceptanceMapper acceptanceMapper;
    private final SfStaskFlowLogMapper flowLogMapper;
    private final SfStaskEmployeeAccessor employeeAccessor;
    private final SfStaskRoleNameReader roleNameReader;
    private final StaskGreenhousePlantingBatchHelper plantingBatchHelper;
    private final SfStaskLeaderLaborRecordMapper laborRecordMapper;

    /**
     * 分页查询生产管理员全部任务。
     */
    public PageResult<SfStaskAllTaskItemVo> managerAllTasks(
        SfStaskAllTaskQueryBo bo, PageQuery pageQuery) {
        return buildAllTasks(requireTenantId(), LoginHelper.getUserId(), bo, pageQuery,
            EmployeeConstants.APP_ROLE_STASK_PRODUCTION_ADMIN);
    }

    /**
     * 分页查询技术员全部任务。
     */
    public PageResult<SfStaskAllTaskItemVo> technicianAllTasks(
        SfStaskAllTaskQueryBo bo, PageQuery pageQuery) {
        return buildAllTasks(requireTenantId(), LoginHelper.getUserId(), bo, pageQuery,
            EmployeeConstants.APP_ROLE_STASK_EXPERT);
    }

    /**
     * 分页查询组长全部任务。
     */
    public PageResult<SfStaskAllTaskItemVo> leaderAllTasks(
        SfStaskAllTaskQueryBo bo, PageQuery pageQuery) {
        String tenantId = requireTenantId();
        List<String> statuses = normalizeStatuses(bo, LEADER_TASK_STATUSES,
            com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKBENCH_INVALID_LEADER_STATUS_FILTER);
        List<SfStaskWorkOrder> splits = workOrderMapper.selectLeaderAllTaskSplits(
            tenantId, LoginHelper.getUserId(), statuses,
            bo == null ? null : bo.getGreenhouseIds(), bo == null ? null : bo.getWorkItemIds(),
            bo == null ? null : bo.getPlanStartDate(), bo == null ? null : bo.getPlanEndDate());
        List<SfStaskAllTaskItemVo> items = buildSplitItems(tenantId, splits);
        plantingBatchHelper.enrichAllTaskItems(items);
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(items, pageQuery.build());
    }

    /**
     * 分页查询领导视角的租户全量有效任务。
     */
    public PageResult<SfStaskAllTaskItemVo> leaderAdminAllTasks(
        SfStaskAllTaskQueryBo bo, PageQuery pageQuery) {
        String tenantId = requireTenantId();
        List<String> requestedStatuses = normalizeStatuses(bo, LEADER_ADMIN_TASK_STATUSES,
            com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKBENCH_INVALID_STATUS_FILTER);
        List<String> statuses = CollUtil.isEmpty(requestedStatuses) ? LEADER_ADMIN_TASK_STATUSES : requestedStatuses;
        Set<String> cardTypes = normalizeCardTypes(bo);
        List<SfStaskAllTaskItemVo> items = new ArrayList<>();
        Long creatorEmployeeId = bo == null ? null : bo.getCreatorEmployeeId();
        if (cardTypes.contains(CARD_PACKAGE)) {
            List<SfStaskTaskPackage> packages = creatorEmployeeId == null
                ? taskPackageMapper.selectLeaderAdminAllTasks(tenantId, statuses,
                    bo == null ? null : bo.getGreenhouseIds(), bo == null ? null : bo.getWorkItemIds(),
                    bo == null ? null : bo.getPlanStartDate(), bo == null ? null : bo.getPlanEndDate())
                : taskPackageMapper.selectLeaderAdminAllTasks(tenantId, statuses,
                    bo == null ? null : bo.getGreenhouseIds(), bo == null ? null : bo.getWorkItemIds(),
                    bo == null ? null : bo.getPlanStartDate(), bo == null ? null : bo.getPlanEndDate(),
                    creatorEmployeeId);
            items.addAll(buildPackageItems(tenantId, packages));
        }
        if (cardTypes.contains(CARD_SPLIT)) {
            List<SfStaskWorkOrder> splits = creatorEmployeeId == null
                ? workOrderMapper.selectAllTaskSplits(tenantId, statuses,
                    bo == null ? null : bo.getGreenhouseIds(), bo == null ? null : bo.getWorkItemIds(),
                    bo == null ? null : bo.getPlanStartDate(), bo == null ? null : bo.getPlanEndDate())
                : workOrderMapper.selectAllTaskSplits(tenantId, statuses,
                    bo == null ? null : bo.getGreenhouseIds(), bo == null ? null : bo.getWorkItemIds(),
                    bo == null ? null : bo.getPlanStartDate(), bo == null ? null : bo.getPlanEndDate(),
                    creatorEmployeeId);
            items.addAll(buildSplitItems(tenantId, splits));
        }
        items.forEach(item -> {
            item.setPrimaryAction(null);
            item.setClockInEnabled(false);
        });
        applyTaskPermissions(items, LoginHelper.getUserId(), EmployeeConstants.APP_ROLE_STASK_LEADER);
        items.sort(Comparator.comparing(SfStaskAllTaskItemVo::getEventTime,
            Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(SfStaskAllTaskItemVo::getPlanDate,
                Comparator.nullsLast(Comparator.reverseOrder())));
        plantingBatchHelper.enrichAllTaskItems(items);
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(items, pageQuery.build());
    }

    private PageResult<SfStaskAllTaskItemVo> buildAllTasks(String tenantId, Long employeeId,
        SfStaskAllTaskQueryBo bo, PageQuery pageQuery, String roleCode) {
        List<String> statuses = normalizeStatuses(bo, ALL_TASK_STATUSES,
            com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKBENCH_INVALID_STATUS_FILTER);
        Set<String> cardTypes = normalizeCardTypes(bo);
        boolean relatedOnly = EmployeeConstants.APP_ROLE_STASK_EXPERT.equals(roleCode)
            && (bo == null || bo.getTaskScope() == null || StaskTaskScope.RELATED_TO_ME.equals(bo.getTaskScope()));
        Long creatorEmployeeId = bo == null ? null : bo.getCreatorEmployeeId();
        List<SfStaskAllTaskItemVo> items = new ArrayList<>();
        if (cardTypes.contains(CARD_PACKAGE)) {
            List<SfStaskTaskPackage> packages;
            if (EmployeeConstants.APP_ROLE_STASK_EXPERT.equals(roleCode)) {
                packages = creatorEmployeeId == null
                    ? taskPackageMapper.selectTechnicianAllTasks(tenantId, employeeId, StaskOrderStatus.DRAFT,
                        statuses, bo == null ? null : bo.getGreenhouseIds(), bo == null ? null : bo.getWorkItemIds(),
                        bo == null ? null : bo.getPlanStartDate(), bo == null ? null : bo.getPlanEndDate(), relatedOnly)
                    : taskPackageMapper.selectTechnicianAllTasks(tenantId, employeeId, StaskOrderStatus.DRAFT,
                        statuses, bo == null ? null : bo.getGreenhouseIds(), bo == null ? null : bo.getWorkItemIds(),
                        bo == null ? null : bo.getPlanStartDate(), bo == null ? null : bo.getPlanEndDate(),
                        relatedOnly, creatorEmployeeId);
            } else {
                packages = creatorEmployeeId == null
                    ? taskPackageMapper.selectManagerAllTasks(tenantId, employeeId, statuses,
                        bo == null ? null : bo.getGreenhouseIds(), bo == null ? null : bo.getWorkItemIds(),
                        bo == null ? null : bo.getPlanStartDate(), bo == null ? null : bo.getPlanEndDate())
                    : taskPackageMapper.selectManagerAllTasks(tenantId, employeeId, statuses,
                        bo == null ? null : bo.getGreenhouseIds(), bo == null ? null : bo.getWorkItemIds(),
                        bo == null ? null : bo.getPlanStartDate(), bo == null ? null : bo.getPlanEndDate(),
                        creatorEmployeeId);
            }
            items.addAll(buildPackageItems(tenantId, packages));
        }
        if (cardTypes.contains(CARD_SPLIT)) {
            List<SfStaskWorkOrder> splits;
            if (EmployeeConstants.APP_ROLE_STASK_EXPERT.equals(roleCode)) {
                splits = creatorEmployeeId == null
                    ? workOrderMapper.selectExpertAllTaskSplits(tenantId, employeeId, statuses,
                        bo == null ? null : bo.getGreenhouseIds(), bo == null ? null : bo.getWorkItemIds(),
                        bo == null ? null : bo.getPlanStartDate(), bo == null ? null : bo.getPlanEndDate(), relatedOnly)
                    : workOrderMapper.selectExpertAllTaskSplits(tenantId, employeeId, statuses,
                        bo == null ? null : bo.getGreenhouseIds(), bo == null ? null : bo.getWorkItemIds(),
                        bo == null ? null : bo.getPlanStartDate(), bo == null ? null : bo.getPlanEndDate(),
                        relatedOnly, creatorEmployeeId);
            } else {
                splits = creatorEmployeeId == null
                    ? workOrderMapper.selectManagerAllTaskSplits(tenantId, statuses,
                        bo == null ? null : bo.getGreenhouseIds(), bo == null ? null : bo.getWorkItemIds(),
                        bo == null ? null : bo.getPlanStartDate(), bo == null ? null : bo.getPlanEndDate())
                    : workOrderMapper.selectManagerAllTaskSplits(tenantId, statuses,
                        bo == null ? null : bo.getGreenhouseIds(), bo == null ? null : bo.getWorkItemIds(),
                        bo == null ? null : bo.getPlanStartDate(), bo == null ? null : bo.getPlanEndDate(),
                        creatorEmployeeId);
            }
            items.addAll(buildSplitItems(tenantId, splits));
        }
        applyTaskPermissions(items, employeeId, roleCode);
        items.sort(Comparator.comparing(SfStaskAllTaskItemVo::getEventTime,
            Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(SfStaskAllTaskItemVo::getPlanDate,
                Comparator.nullsLast(Comparator.reverseOrder())));
        plantingBatchHelper.enrichAllTaskItems(items);
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(items, pageQuery.build());
    }

    private List<SfStaskAllTaskItemVo> buildPackageItems(String tenantId,
        List<SfStaskTaskPackage> packages) {
        if (CollUtil.isEmpty(packages)) {
            return List.of();
        }
        List<Long> packageIds = packages.stream().map(SfStaskTaskPackage::getPackageId)
            .filter(Objects::nonNull).distinct().toList();
        Set<Long> havingSplits = workOrderMapper.selectPackageIdsHavingSplits(tenantId, packageIds);
        List<SfStaskTaskPackage> visible = packages.stream()
            .filter(row -> row.getPackageId() == null || !havingSplits.contains(row.getPackageId()))
            .toList();
        if (visible.isEmpty()) {
            return List.of();
        }
        List<Long> visibleIds = visible.stream().map(SfStaskTaskPackage::getPackageId).toList();
        Map<Long, List<SfStaskWorkOrderItem>> itemGroups = itemMapper.selectByPackageIds(tenantId, visibleIds)
            .stream().collect(Collectors.groupingBy(SfStaskWorkOrderItem::getPackageId));
        Map<Long, List<SfStaskWorkOrderGreenhouse>> greenhouseRows = greenhouseMapper
            .selectByPackageIds(tenantId, visibleIds).stream()
            .collect(Collectors.groupingBy(SfStaskWorkOrderGreenhouse::getPackageId));
        Map<String, SfStaskFlowLog> flowLogs = SfStaskWorkOrderAssembler.indexLatestFlowLogs(
            flowLogMapper.selectByOrderIds(tenantId, visibleIds));
        SfStaskEmployeeQueryContext employees = employeeContext();
        employees.preload(visible.stream().flatMap(row -> Stream.of(
                row.getCreatorEmployeeId(), row.getHandlerTechnicianEmployeeId()))
            .filter(Objects::nonNull).distinct().toList());
        Map<String, String> roleNames = roleNameReader.load(visible.stream()
            .map(SfStaskTaskPackage::getCreatorRoleCode).toList());

        List<SfStaskAllTaskItemVo> result = new ArrayList<>(visible.size());
        for (SfStaskTaskPackage taskPackage : visible) {
            SfStaskAllTaskItemVo item = new SfStaskAllTaskItemVo();
            item.setCardType(CARD_PACKAGE);
            item.setOrderId(taskPackage.getPackageId());
            item.setPackageId(taskPackage.getPackageId());
            item.setPlanDate(taskPackage.getPlanDate());
            item.setStatus(taskPackage.getStatus());
            item.setStatusLabel(SfStaskWorkOrderAssembler.packageStatusLabel(taskPackage.getStatus(), messages));
            item.setPrimaryAction(SfStaskWorkOrderAssembler.packagePrimaryAction(taskPackage.getStatus()));
            item.setCreatorEmployeeId(taskPackage.getCreatorEmployeeId());
            item.setCreatorEmployeeName(employees.nameOf(taskPackage.getCreatorEmployeeId()));
            item.setCreatorRoleCode(taskPackage.getCreatorRoleCode());
            item.setCreatorRoleName(roleNameReader.resolve(roleNames, taskPackage.getCreatorRoleCode()));
            item.setHandlerTechnicianEmployeeId(taskPackage.getHandlerTechnicianEmployeeId());
            item.setHandlerTechnicianEmployeeName(StringUtils.isBlank(taskPackage.getHandlerTechnicianEmployeeNameSnapshot())
                ? employees.nameOf(taskPackage.getHandlerTechnicianEmployeeId())
                : taskPackage.getHandlerTechnicianEmployeeNameSnapshot());
            item.setCreateTime(com.ym.agriculture.shared.common.AgricultureTimes.toDate(taskPackage.getCreateTime()));
            fillPackageEvent(item, taskPackage, flowLogs);
            List<SfStaskWorkOrderItem> packageItems = itemGroups.getOrDefault(taskPackage.getPackageId(), List.of());
            List<SfStaskWorkOrderGreenhouse> packageGreenhouses = greenhouseRows.getOrDefault(
                taskPackage.getPackageId(), List.of());
            fillPackageSummaries(item, packageItems, packageGreenhouses);
            item.setGreenhouses(SfStaskWorkOrderAssembler.toDistinctGreenhouseBriefs(packageGreenhouses));
            result.add(item);
        }
        return result;
    }

    private List<SfStaskAllTaskItemVo> buildSplitItems(String tenantId, List<SfStaskWorkOrder> splits) {
        if (CollUtil.isEmpty(splits)) {
            return new ArrayList<>();
        }
        List<Long> orderIds = splits.stream().map(SfStaskWorkOrder::getOrderId)
            .filter(Objects::nonNull).distinct().toList();
        List<SfStaskFlowLog> logs = flowLogMapper.selectByOrderIds(tenantId, orderIds);
        Map<String, SfStaskFlowLog> latestLogs = SfStaskWorkOrderAssembler.indexLatestFlowLogs(logs);
        Map<Long, SfStaskFlowLog> issuedLogs = SfStaskWorkOrderAssembler.indexIssuedAtLogs(logs);
        Map<Long, SfStaskCompletion> completions = SfStaskWorkOrderAssembler.indexLatestCompletions(
            completionMapper.selectByOrderIds(tenantId, orderIds));
        Map<Long, SfStaskClockRecord> clocks = SfStaskWorkOrderAssembler.indexLatestClockRecords(
            clockRecordMapper.selectByOrderIds(tenantId, orderIds));
        Map<Long, SfStaskAcceptance> acceptances = SfStaskWorkOrderAssembler.indexLatestAcceptances(
            acceptanceMapper.selectByOrderIds(tenantId, orderIds));
        Map<Long, List<SfStaskGreenhouseBriefVo>> greenhouses = loadOrderGreenhouses(tenantId, orderIds);
        SfStaskEmployeeQueryContext employees = employeeContext();
        employees.preload(splits.stream().flatMap(row -> Stream.of(
                row.getLeaderId(), row.getCreatorEmployeeId(), row.getHandlerTechnicianEmployeeId()))
            .filter(Objects::nonNull).distinct().toList());
        Map<String, String> roleNames = roleNameReader.load(splits.stream()
            .map(SfStaskWorkOrder::getCreatorRoleCode).toList());
        Map<String, SfStaskLeaderLaborRecord> laborRecords = new HashMap<>();
        List<Long> leaderIds = splits.stream().map(SfStaskWorkOrder::getLeaderId).filter(Objects::nonNull).distinct().toList();
        List<java.time.LocalDate> planDates = splits.stream().filter(row -> row.getPlanDate() != null)
            .map(row -> row.getPlanDate().toInstant().atZone(ZoneId.of("Asia/Shanghai")).toLocalDate()).distinct().toList();
        for (SfStaskLeaderLaborRecord labor : laborRecordMapper.selectByLeadersAndPlanDates(tenantId, leaderIds, planDates)) {
            laborRecords.put(labor.getLeaderEmployeeId() + "_" + labor.getPlanDate(), labor);
        }

        List<SfStaskAllTaskItemVo> result = new ArrayList<>(splits.size());
        for (SfStaskWorkOrder order : splits) {
            SfStaskWorkOrderVo base = SfStaskWorkOrderAssembler.toWorkOrderVo(
                order, employees.employees(), greenhouses);
            SfStaskAllTaskItemVo item = new SfStaskAllTaskItemVo();
            BeanUtil.copyProperties(base, item);
            if (order.getLeaderId() != null && order.getPlanDate() != null) {
                SfStaskLeaderLaborRecord labor = laborRecords.get(order.getLeaderId() + "_"
                    + order.getPlanDate().toInstant().atZone(ZoneId.of("Asia/Shanghai")).toLocalDate());
                BigDecimal laborCount = labor == null ? null : labor.getLaborCount();
                item.setDailyLaborCount(laborCount);
                item.setRequiredWorkerCount(laborCount == null ? null : laborCount.doubleValue());
                item.setLaborRecordVersion(labor == null ? null : labor.getVersion());
            }
            item.setCardType(CARD_SPLIT);
            item.setTitle(SfStaskWorkOrderAssembler.splitTitle(
                order.getGreenhouseNameSnapshot(), order.getWorkItemNameSnapshot()));
            item.setWorkItemId(order.getWorkItemId());
            item.setStatusLabel(SfStaskWorkOrderAssembler.splitStatusLabel(order.getStatus(), messages));
            item.setCreatorRoleCode(order.getCreatorRoleCode());
            item.setCreatorRoleName(roleNameReader.resolve(roleNames, order.getCreatorRoleCode()));
            item.setCreateTime(com.ym.agriculture.shared.common.AgricultureTimes.toDate(order.getCreateTime()));
            fillSplitEvent(item, order, latestLogs, issuedLogs, completions, clocks, acceptances);
            result.add(item);
        }
        result.sort(Comparator.comparing(SfStaskAllTaskItemVo::getPlanDate,
            Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(SfStaskAllTaskItemVo::getEventTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    private static void fillPackageSummaries(SfStaskAllTaskItemVo target,
        List<SfStaskWorkOrderItem> items, List<SfStaskWorkOrderGreenhouse> greenhouses) {
        Map<Long, Long> greenhouseCounts = greenhouses.stream()
            .filter(row -> row.getItemId() != null)
            .collect(Collectors.groupingBy(SfStaskWorkOrderGreenhouse::getItemId, LinkedHashMap::new,
                Collectors.mapping(SfStaskWorkOrderGreenhouse::getGreenhouseId,
                    Collectors.collectingAndThen(Collectors.toSet(), set -> (long) set.size()))));
        target.setTotalItemCount(items.size());
        List<SfStaskHomeItemSummaryVo> summaries = items.stream().limit(PACKAGE_ITEM_PREVIEW_LIMIT)
            .map(row -> {
                SfStaskHomeItemSummaryVo summary = new SfStaskHomeItemSummaryVo();
                summary.setItemId(row.getItemId());
                summary.setWorkItemId(row.getWorkItemId());
                summary.setWorkItemName(row.getWorkItemNameSnapshot());
                summary.setGreenhouseCount(greenhouseCounts.getOrDefault(row.getItemId(), 0L).intValue());
                return summary;
            }).toList();
        target.setItemSummaries(summaries);
        target.setMoreItemCount(Math.max(items.size() - summaries.size(), 0));
    }

    private void fillPackageEvent(SfStaskAllTaskItemVo item, SfStaskTaskPackage taskPackage,
        Map<String, SfStaskFlowLog> logs) {
        String status = taskPackage.getStatus();
        if (StaskOrderStatus.DRAFT.equals(status)) {
            item.setEventTime(com.ym.agriculture.shared.common.AgricultureTimes.toDate(taskPackage.getCreateTime()));
            item.setEventTimeLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_CREATED_TIME));
            return;
        }
        String event = StaskOrderStatus.TECH_REJECTED.equals(status) ? StaskOrderEvent.TECH_REJECT
            : StaskOrderStatus.VOIDED.equals(status) ? StaskOrderEvent.VOID
            : StaskOrderStatus.CANCELLED.equals(status) ? StaskOrderEvent.CANCEL : null;
        SfStaskFlowLog log = event == null ? null : logs.get(taskPackage.getPackageId() + ":" + event);
        item.setEventTime(log == null
            ? com.ym.agriculture.shared.common.AgricultureTimes.toDate(taskPackage.getUpdateTime())
            : log.getCreateTime());
        item.setEventTimeLabel(StaskOrderStatus.PENDING_TECH_CONFIRM.equals(status) ? messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_PACKAGE_SUBMITTED_TIME)
            : StaskOrderStatus.TECH_REJECTED.equals(status) ? messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_PACKAGE_REJECTED_TIME)
            : StaskOrderStatus.VOIDED.equals(status) ? messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_CANCELLED_TIME) : messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_UPDATED_TIME));
    }

    private void fillSplitEvent(SfStaskAllTaskItemVo item, SfStaskWorkOrder order,
        Map<String, SfStaskFlowLog> logs, Map<Long, SfStaskFlowLog> issuedLogs,
        Map<Long, SfStaskCompletion> completions, Map<Long, SfStaskClockRecord> clocks,
        Map<Long, SfStaskAcceptance> acceptances) {
        Long orderId = order.getOrderId();
        SfStaskAcceptance acceptance = acceptances.get(orderId);
        if (acceptance != null) {
            item.setAcceptanceResult(acceptance.getResult());
            item.setAcceptedAt(acceptance.getAcceptedAt());
        }
        if (acceptance != null
            && (StaskOrderStatus.ACCEPTANCE_PASSED.equals(order.getStatus())
            || StaskOrderStatus.ACCEPTANCE_REJECTED.equals(order.getStatus()))) {
            item.setEventTime(acceptance.getAcceptedAt());
            item.setEventTimeLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_ACCEPTANCE_TIME));
            return;
        }
        SfStaskCompletion completion = completions.get(orderId);
        if (completion != null) {
            item.setCompletedAt(completion.getCompletedAt());
            item.setEventTime(completion.getCompletedAt());
            item.setEventTimeLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_COMPLETION_TIME));
        } else if (clocks.get(orderId) != null) {
            item.setEventTime(clocks.get(orderId).getClockTime());
            item.setEventTimeLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_ON_DUTY_TIME));
        } else if (issuedLogs.get(orderId) != null) {
            item.setEventTime(issuedLogs.get(orderId).getCreateTime());
            item.setEventTimeLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_ISSUED_TIME));
        }
        if (StaskOrderStatus.PENDING_ACCEPTANCE.equals(order.getStatus())) {
            item.setPrimaryAction(ACTION_ACCEPT);
        }
        if (item.getEventTime() == null) {
            SfStaskFlowLog voidLog = logs.get(orderId + ":" + StaskOrderEvent.VOID);
            item.setEventTime(voidLog == null
                ? com.ym.agriculture.shared.common.AgricultureTimes.toDate(order.getUpdateTime())
                : voidLog.getCreateTime());
        }
    }

    private Map<Long, List<SfStaskGreenhouseBriefVo>> loadOrderGreenhouses(
        String tenantId, Collection<Long> orderIds) {
        Map<Long, List<SfStaskGreenhouseBriefVo>> result = new HashMap<>();
        if (CollUtil.isEmpty(orderIds)) {
            return result;
        }
        greenhouseMapper.selectByOrderIds(tenantId, orderIds).stream()
            .collect(Collectors.groupingBy(SfStaskWorkOrderGreenhouse::getOrderId))
            .forEach((orderId, rows) -> result.put(
                orderId, SfStaskWorkOrderAssembler.toDistinctGreenhouseBriefs(rows)));
        return result;
    }

    private void applyTaskPermissions(List<SfStaskAllTaskItemVo> items, Long employeeId, String roleCode) {
        boolean technician = EmployeeConstants.APP_ROLE_STASK_EXPERT.equals(roleCode);
        boolean productionManager = EmployeeConstants.APP_ROLE_STASK_PRODUCTION_ADMIN.equals(roleCode);
        for (SfStaskAllTaskItemVo item : items) {
            boolean related = Objects.equals(item.getCreatorEmployeeId(), employeeId)
                || Objects.equals(item.getHandlerTechnicianEmployeeId(), employeeId);
            boolean pendingTechConfirm = StaskOrderStatus.PENDING_TECH_CONFIRM.equals(item.getStatus())
                && StaskCreatorRole.PRODUCTION_ADMIN.equals(item.getCreatorRoleCode());
            boolean pendingAcceptance = StaskOrderStatus.PENDING_ACCEPTANCE.equals(item.getStatus());
            item.setRelatedToCurrentUser(related);
            item.setCanTechConfirm(technician && pendingTechConfirm);
            item.setCanTechReject(technician && pendingTechConfirm);
            item.setCanAcceptance(pendingAcceptance
                && (productionManager || technician
                    && Objects.equals(item.getHandlerTechnicianEmployeeId(), employeeId)));
            if (Boolean.TRUE.equals(item.getCanAcceptance())) {
                item.setPrimaryAction(ACTION_ACCEPT);
            } else if (technician && !related && !pendingTechConfirm) {
                item.setReadonlyReason(messages.message(
                    com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_PRODUCTION_ADMIN_ACCEPTANCE_ONLY));
                item.setPrimaryAction(null);
            }
            if (EmployeeConstants.APP_ROLE_STASK_LEADER.equals(roleCode)) {
                item.setCanTechConfirm(false);
                item.setCanTechReject(false);
                item.setCanAcceptance(false);
                item.setPrimaryAction(null);
            }
        }
    }

    private List<String> normalizeStatuses(SfStaskAllTaskQueryBo bo,
        Collection<String> allowedStatuses, String errorKey) {
        if (bo == null || CollUtil.isEmpty(bo.getStatuses())) {
            return List.of();
        }
        Set<String> allowed = new HashSet<>(allowedStatuses);
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String status : bo.getStatuses()) {
            if (StringUtils.isBlank(status)) {
                continue;
            }
            String normalized = status.trim().toUpperCase();
            if (!allowed.contains(normalized)) {
                throw messages.exception(errorKey, status);
            }
            result.add(normalized);
        }
        return new ArrayList<>(result);
    }

    private Set<String> normalizeCardTypes(SfStaskAllTaskQueryBo bo) {
        if (bo == null || CollUtil.isEmpty(bo.getCardTypes())) {
            return CARD_TYPES;
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String cardType : bo.getCardTypes()) {
            if (StringUtils.isBlank(cardType)) {
                continue;
            }
            String normalized = cardType.trim().toUpperCase();
            if (!CARD_TYPES.contains(normalized)) {
                throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_INVALID_CARD_TYPE,
                    cardType);
            }
            result.add(normalized);
        }
        return result.isEmpty() ? CARD_TYPES : result;
    }

    private SfStaskEmployeeQueryContext employeeContext() {
        return new SfStaskEmployeeQueryContext(ids -> {
            List<SysEmployeeVo> rows = employeeAccessor.queryBasicByIds(ids);
            return rows != null ? rows : employeeAccessor.queryByIds(ids);
        });
    }

    private String requireTenantId() {
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_COMMON_TENANT_CONTEXT_MISSING);
        }
        return tenantId;
    }
}
