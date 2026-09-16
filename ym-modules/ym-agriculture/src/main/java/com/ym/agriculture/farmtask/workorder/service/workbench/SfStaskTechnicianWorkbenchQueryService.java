package com.ym.agriculture.farmtask.workorder.service.workbench;

import cn.hutool.core.collection.CollUtil;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskTaskPackageMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderMapper;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskCreatorRole;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskTaskPackage;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskTechnicianWorkbenchItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskTechnicianWorkbenchSummaryVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskTechnicianWorkbenchTasksVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkbenchVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkOrderVo;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeQueryContext;
import com.ym.agriculture.farmtask.workorder.support.SfStaskWorkOrderAssembler;
import com.ym.agriculture.farmtask.workorder.support.StaskGreenhousePlantingBatchHelper;
import com.ym.agriculture.farmtask.workorder.support.workbench.SfStaskPackageWorkbenchAssembler;
import com.ym.agriculture.farmtask.workorder.support.workbench.SfStaskPackageWorkbenchBatchReader;
import com.ym.agriculture.farmtask.workorder.support.workbench.SfStaskSplitWorkbenchAssembler;
import com.ym.agriculture.farmtask.workorder.support.workbench.SfStaskSplitWorkbenchBatchReader;
import com.ym.agriculture.farmtask.workorder.support.workbench.SfStaskWorkbenchEmployeeContextFactory;
import com.ym.agriculture.farmtask.workorder.support.workbench.SfStaskWorkbenchRoleNameReader;
import com.ym.agriculture.farmtask.workorder.support.workbench.SfStaskWorkbenchTabSupport;
import com.ym.agriculture.farmtask.workorder.support.workbench.SfStaskWorkbenchTimeSupport;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 技术员工作台查询服务。
 */
@Service
@RequiredArgsConstructor
public class SfStaskTechnicianWorkbenchQueryService {

    private static final String TAB_PENDING = "pending";
    private static final String TAB_PROCESSING = "processing";
    private static final String TAB_COMPLETED_TODAY = "completed_today";

    private static final List<String> OWN_PENDING_PACKAGE_STATUSES = List.of(
        StaskOrderStatus.DRAFT, StaskOrderStatus.PENDING_TECH_CONFIRM);
    private static final List<String> PROCESSING_SPLIT_STATUSES = List.of(
        StaskOrderStatus.PENDING_LEADER_ACCEPT,
        StaskOrderStatus.ASSIGN_COMPLETE,
        StaskOrderStatus.LEADER_ARRIVED,
        StaskOrderStatus.ACCEPTANCE_REJECTED);
    private static final List<String> PENDING_ACCEPTANCE_STATUSES = List.of(
        StaskOrderStatus.PENDING_ACCEPTANCE);
    private static final List<String> COMPLETED_STATUSES = List.of(StaskOrderStatus.ACCEPTANCE_PASSED);
    private static final List<String> LEGACY_STATUSES = List.of(
        StaskOrderStatus.DRAFT,
        StaskOrderStatus.PENDING_TECH_CONFIRM,
        StaskOrderStatus.TECH_REJECTED,
        StaskOrderStatus.PENDING_LEADER_ACCEPT,
        StaskOrderStatus.ASSIGN_COMPLETE,
        StaskOrderStatus.LEADER_ARRIVED,
        StaskOrderStatus.PENDING_ACCEPTANCE,
        StaskOrderStatus.VOIDED);

    private final com.ym.agriculture.shared.i18n.StaskMessageResolver messages;
    private final SfStaskWorkOrderMapper workOrderMapper;
    private final SfStaskTaskPackageMapper taskPackageMapper;
    private final SfStaskPackageWorkbenchBatchReader packageReader;
    private final SfStaskSplitWorkbenchBatchReader splitReader;
    private final SfStaskWorkbenchEmployeeContextFactory employeeContextFactory;
    private final SfStaskWorkbenchRoleNameReader roleNameReader;
    private final SfStaskPackageWorkbenchAssembler packageAssembler;
    private final SfStaskSplitWorkbenchAssembler splitAssembler;
    private final StaskGreenhousePlantingBatchHelper plantingBatchHelper;

    /**
     * 查询技术员工作台统计。
     *
     * @param tenantId  当前租户
     * @param employeeId 当前员工
     * @return 工作台统计
     */
    public SfStaskTechnicianWorkbenchSummaryVo summary(String tenantId, Long employeeId) {
        long ownDraft = taskPackageMapper.countByCreatorAndStatuses(
            tenantId, employeeId, List.of(StaskOrderStatus.DRAFT));
        long ownPending = taskPackageMapper.countByCreatorAndStatuses(
            tenantId, employeeId, List.of(StaskOrderStatus.PENDING_TECH_CONFIRM));
        long reviewPool = taskPackageMapper.countByStatusAndCreatorRole(
            tenantId, StaskOrderStatus.PENDING_TECH_CONFIRM, StaskCreatorRole.PRODUCTION_ADMIN);
        long pendingAcceptance = workOrderMapper.countExpertSplitOrdersByStatuses(
            tenantId, employeeId, PENDING_ACCEPTANCE_STATUSES);
        long processing = workOrderMapper.countExpertSplitOrdersByStatuses(
            tenantId, employeeId, PROCESSING_SPLIT_STATUSES);
        Date[] todayRange = SfStaskWorkbenchTimeSupport.todayRange();
        long completedToday = workOrderMapper.countExpertSplitOrdersAcceptedToday(
            tenantId, employeeId, COMPLETED_STATUSES, todayRange[0], todayRange[1]);

        long pending = ownDraft + ownPending + reviewPool + pendingAcceptance;
        SfStaskTechnicianWorkbenchSummaryVo vo = new SfStaskTechnicianWorkbenchSummaryVo();
        vo.setPendingCount(pending);
        vo.setProcessingCount(processing);
        vo.setCompletedTodayCount(completedToday);
        vo.setPendingReviewCount(pending);
        vo.setPendingProductionReviewCount(reviewPool);
        vo.setPendingAcceptanceCount(pendingAcceptance);
        return vo;
    }

    /**
     * 查询技术员指定分栏任务。
     *
     * @param tenantId  当前租户
     * @param employeeId 当前员工
     * @param tab        分栏编码
     * @return 分栏任务
     */
    public SfStaskTechnicianWorkbenchTasksVo tasks(String tenantId, Long employeeId, String tab) {
        String normalizedTab = SfStaskWorkbenchTabSupport.normalizeManagerTab(tab, messages);
        List<SfStaskTechnicianWorkbenchItemVo> items;
        if (TAB_PENDING.equals(normalizedTab)) {
            items = pendingItems(tenantId, employeeId);
        } else {
            boolean completed = TAB_COMPLETED_TODAY.equals(normalizedTab);
            List<SfStaskWorkOrder> splits = loadSplits(tenantId, employeeId, completed);
            SfStaskEmployeeQueryContext employees = prepareEmployees(List.of(), splits);
            SfStaskSplitWorkbenchBatchReader.SplitData data = splitReader.loadEvents(
                tenantId, splits, true);
            items = splitAssembler.technicianItems(splits, employees.employees(), data, completed, employeeId);
        }
        plantingBatchHelper.enrichTechnicianWorkbenchItems(items);
        SfStaskTechnicianWorkbenchTasksVo vo = new SfStaskTechnicianWorkbenchTasksVo();
        vo.setTab(normalizedTab);
        vo.setItems(items);
        return vo;
    }

    /**
     * 查询技术员旧版兼容工作台。
     *
     * @param tenantId  当前租户
     * @param employeeId 当前员工
     * @return 兼容工作台
     */
    public SfStaskWorkbenchVo workbench(String tenantId, Long employeeId) {
        List<SfStaskWorkOrder> personalRows = CollUtil.emptyIfNull(
            workOrderMapper.selectTechnicianByEmployeeAndStatuses(tenantId, employeeId, LEGACY_STATUSES));
        List<SfStaskTaskPackage> reviewPool = CollUtil.emptyIfNull(
            taskPackageMapper.selectByStatusAndCreatorRole(
                tenantId, StaskOrderStatus.PENDING_TECH_CONFIRM, StaskCreatorRole.PRODUCTION_ADMIN));
        List<SfStaskTaskPackage> ownPackages = CollUtil.emptyIfNull(
            taskPackageMapper.selectByCreatorAndStatuses(tenantId, employeeId, OWN_PENDING_PACKAGE_STATUSES));
        List<SfStaskTaskPackage> packages = mergePackages(reviewPool, ownPackages);
        SfStaskEmployeeQueryContext employees = prepareEmployees(packages, personalRows);
        SfStaskPackageWorkbenchBatchReader.PackageData packageData = packageReader.load(tenantId, packages);
        Map<Long, List<com.ym.agriculture.farmtask.workorder.model.vo.SfStaskGreenhouseBriefVo>> greenhouses =
            splitReader.loadGreenhouses(tenantId, personalRows);

        List<SfStaskWorkOrderVo> rows = new ArrayList<>();
        for (SfStaskTaskPackage taskPackage : packages) {
            rows.add(SfStaskWorkOrderAssembler.toPackageVo(taskPackage, employees.employees(),
                packageData.greenhouses().getOrDefault(taskPackage.getPackageId(), List.of())));
        }
        rows.addAll(splitAssembler.legacyOrders(personalRows, employees.employees(), greenhouses));
        plantingBatchHelper.enrichWorkOrderVos(rows);

        long ownDraftCount = ownPackages.stream()
            .filter(row -> StaskOrderStatus.DRAFT.equals(row.getStatus())
                && Objects.equals(row.getCreatorEmployeeId(), employeeId))
            .count();
        SfStaskWorkbenchVo vo = new SfStaskWorkbenchVo();
        vo.setPendingReviewCount(ownDraftCount + reviewPool.size());
        vo.setPendingAcceptanceCount(personalRows.stream()
            .filter(row -> StaskOrderStatus.PENDING_ACCEPTANCE.equals(row.getStatus())).count());
        vo.setProcessingCount(rows.stream().filter(SfStaskTechnicianWorkbenchQueryService::isProcessing).count());
        vo.setRows(rows);
        return vo;
    }

    private List<SfStaskTechnicianWorkbenchItemVo> pendingItems(String tenantId, Long employeeId) {
        List<SfStaskTaskPackage> packages = loadPendingPackages(tenantId, employeeId);
        List<SfStaskWorkOrder> pendingAcceptance = CollUtil.emptyIfNull(
            workOrderMapper.selectExpertSplitOrdersByStatuses(
                tenantId, employeeId, PENDING_ACCEPTANCE_STATUSES));
        SfStaskEmployeeQueryContext employees = prepareEmployees(packages, pendingAcceptance);
        SfStaskPackageWorkbenchBatchReader.PackageData data = packageReader.load(tenantId, packages);
        Map<Long, String> employeeNames = new HashMap<>();
        employees.employees().forEach((id, employee) -> employeeNames.put(id, employee.getName()));
        Map<String, String> roleNames = roleNameReader.load(packages.stream()
            .map(SfStaskTaskPackage::getCreatorRoleCode).filter(Objects::nonNull).distinct().toList());
        List<SfStaskTechnicianWorkbenchItemVo> items = new ArrayList<>(
            packageAssembler.technicianPendingItems(packages, data, employeeNames, roleNames, employeeId));
        SfStaskSplitWorkbenchBatchReader.SplitData splitData = splitReader.loadEvents(
            tenantId, pendingAcceptance, true);
        items.addAll(splitAssembler.technicianItems(
            pendingAcceptance, employees.employees(), splitData, false, employeeId));
        SfStaskWorkOrderAssembler.sortTechnicianWorkbenchItems(items);
        return items;
    }

    private List<SfStaskTaskPackage> loadPendingPackages(String tenantId, Long employeeId) {
        List<SfStaskTaskPackage> reviewPool = CollUtil.emptyIfNull(
            taskPackageMapper.selectByStatusAndCreatorRole(
                tenantId, StaskOrderStatus.PENDING_TECH_CONFIRM, StaskCreatorRole.PRODUCTION_ADMIN));
        List<SfStaskTaskPackage> ownPackages = CollUtil.emptyIfNull(
            taskPackageMapper.selectByCreatorAndStatuses(tenantId, employeeId, OWN_PENDING_PACKAGE_STATUSES));
        return mergePackages(reviewPool, ownPackages);
    }

    private List<SfStaskWorkOrder> loadSplits(String tenantId, Long employeeId, boolean completed) {
        if (!completed) {
            return CollUtil.emptyIfNull(workOrderMapper.selectExpertSplitOrdersByStatuses(
                tenantId, employeeId, PROCESSING_SPLIT_STATUSES));
        }
        Date[] todayRange = SfStaskWorkbenchTimeSupport.todayRange();
        return CollUtil.emptyIfNull(workOrderMapper.selectExpertSplitOrdersAcceptedToday(
            tenantId, employeeId, COMPLETED_STATUSES, todayRange[0], todayRange[1]));
    }

    private SfStaskEmployeeQueryContext prepareEmployees(
        List<SfStaskTaskPackage> packages, List<SfStaskWorkOrder> splits) {
        SfStaskEmployeeQueryContext context = employeeContextFactory.create();
        context.preload(Stream.concat(
                packages.stream().flatMap(row -> Stream.of(
                    row.getCreatorEmployeeId(), row.getHandlerTechnicianEmployeeId())),
                splits.stream().flatMap(row -> Stream.of(
                    row.getLeaderId(), row.getCreatorEmployeeId(), row.getHandlerTechnicianEmployeeId())))
            .filter(Objects::nonNull).distinct().toList());
        return context;
    }

    private static List<SfStaskTaskPackage> mergePackages(
        List<SfStaskTaskPackage> reviewPool, List<SfStaskTaskPackage> ownPackages) {
        Map<Long, SfStaskTaskPackage> merged = new LinkedHashMap<>();
        reviewPool.forEach(row -> merged.put(row.getPackageId(), row));
        ownPackages.forEach(row -> merged.putIfAbsent(row.getPackageId(), row));
        return merged.values().stream()
            .sorted(Comparator.comparing(SfStaskTaskPackage::getCreateTime,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .toList();
    }

    private static boolean isProcessing(SfStaskWorkOrderVo row) {
        return !StaskOrderStatus.PENDING_TECH_CONFIRM.equals(row.getStatus())
            && !StaskOrderStatus.DRAFT.equals(row.getStatus())
            && !StaskOrderStatus.PENDING_ACCEPTANCE.equals(row.getStatus())
            && !StaskOrderStatus.VOIDED.equals(row.getStatus());
    }
}
