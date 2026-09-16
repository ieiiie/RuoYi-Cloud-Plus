package com.ym.agriculture.farmtask.workorder.service.workbench;

import cn.hutool.core.collection.CollUtil;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskTaskPackageMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderMapper;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskCreatorRole;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskTaskPackage;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskHomeTaskCardVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskManagerWorkbenchItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskManagerWorkbenchSummaryVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskManagerWorkbenchTasksVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkbenchVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkOrderVo;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeQueryContext;
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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 生产管理员工作台查询服务。
 */
@Service
@RequiredArgsConstructor
public class SfStaskManagerWorkbenchQueryService {

    private static final String TAB_PENDING = "pending";
    private static final String TAB_PROCESSING = "processing";
    private static final String TAB_COMPLETED_TODAY = "completed_today";

    private static final List<String> PENDING_PACKAGE_STATUSES = List.of(
        StaskOrderStatus.DRAFT, StaskOrderStatus.TECH_REJECTED);
    private static final List<String> PROCESSING_PACKAGE_STATUSES = List.of(
        StaskOrderStatus.PENDING_TECH_CONFIRM);
    private static final List<String> PENDING_ACCEPTANCE_STATUSES = List.of(
        StaskOrderStatus.PENDING_ACCEPTANCE);
    private static final List<String> PROCESSING_SPLIT_STATUSES = List.of(
        StaskOrderStatus.PENDING_LEADER_ACCEPT,
        StaskOrderStatus.ASSIGN_COMPLETE,
        StaskOrderStatus.LEADER_ARRIVED,
        StaskOrderStatus.ACCEPTANCE_REJECTED);
    private static final List<String> COMPLETED_STATUSES = List.of(StaskOrderStatus.ACCEPTANCE_PASSED);

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
     * 查询生产管理员工作台统计。
     *
     * @param tenantId  当前租户
     * @param employeeId 当前员工
     * @return 工作台统计
     */
    public SfStaskManagerWorkbenchSummaryVo summary(String tenantId, Long employeeId) {
        long pendingPackages = taskPackageMapper.countByCreatorAndStatuses(
            tenantId, employeeId, PENDING_PACKAGE_STATUSES);
        long pendingAcceptance = workOrderMapper.countByStatuses(tenantId, PENDING_ACCEPTANCE_STATUSES);
        long processingPackages = taskPackageMapper.countByStatusAndCreatorRole(
            tenantId, StaskOrderStatus.PENDING_TECH_CONFIRM, StaskCreatorRole.PRODUCTION_ADMIN);
        long processingSplits = workOrderMapper.countByStatuses(tenantId, PROCESSING_SPLIT_STATUSES);
        Date[] todayRange = SfStaskWorkbenchTimeSupport.todayRange();
        long completedToday = workOrderMapper.countSplitOrdersAcceptedToday(
            tenantId, COMPLETED_STATUSES, todayRange[0], todayRange[1]);

        long pendingCount = pendingPackages + pendingAcceptance;
        SfStaskManagerWorkbenchSummaryVo vo = new SfStaskManagerWorkbenchSummaryVo();
        vo.setPendingCount(pendingCount);
        vo.setProcessingCount(processingPackages + processingSplits);
        vo.setCompletedTodayCount(completedToday);
        vo.setPendingReviewCount(pendingCount);
        vo.setPendingAcceptanceCount(pendingAcceptance);
        return vo;
    }

    /**
     * 查询生产管理员指定分栏任务。
     *
     * @param tenantId  当前租户
     * @param employeeId 当前员工
     * @param tab        分栏编码
     * @return 分栏任务
     */
    public SfStaskManagerWorkbenchTasksVo tasks(String tenantId, Long employeeId, String tab) {
        String normalizedTab = SfStaskWorkbenchTabSupport.normalizeManagerTab(tab, messages);
        List<SfStaskTaskPackage> packages = loadPackages(tenantId, employeeId, normalizedTab);
        List<SfStaskWorkOrder> splits = loadSplits(tenantId, normalizedTab);
        QueryContext context = prepareContext(packages, splits);
        SfStaskPackageWorkbenchBatchReader.PackageData packageData = packageReader.load(tenantId, packages);
        SfStaskSplitWorkbenchBatchReader.SplitData splitData = splitReader.loadEvents(
            tenantId, splits, TAB_COMPLETED_TODAY.equals(normalizedTab));

        List<SfStaskHomeTaskCardVo> packageCards = TAB_PENDING.equals(normalizedTab)
            ? packageAssembler.managerPendingCards(
                packages, packageData, context.employeeNames(), context.roleNames())
            : packageAssembler.managerProcessingCards(
                packages, packageData, context.employeeNames(), context.roleNames());
        List<SfStaskWorkOrderVo> splitVos = splitAssembler.managerOrders(
            splits, context.employees(), splitData);
        if (TAB_COMPLETED_TODAY.equals(normalizedTab)) {
            splitAssembler.applyCompletedAcceptance(splitVos, splitData);
        }
        List<SfStaskManagerWorkbenchItemVo> items = packageAssembler.mergeManagerItems(packageCards, splitVos);
        plantingBatchHelper.enrichManagerWorkbenchItems(items);

        SfStaskManagerWorkbenchTasksVo vo = new SfStaskManagerWorkbenchTasksVo();
        vo.setTab(normalizedTab);
        vo.setItems(items);
        return vo;
    }

    /**
     * 查询生产管理员旧版兼容工作台。
     *
     * @param tenantId  当前租户
     * @param employeeId 当前员工
     * @return 兼容工作台
     */
    public SfStaskWorkbenchVo workbench(String tenantId, Long employeeId) {
        List<SfStaskTaskPackage> pendingPackages = CollUtil.emptyIfNull(
            taskPackageMapper.selectByCreatorAndStatuses(tenantId, employeeId, PENDING_PACKAGE_STATUSES));
        List<SfStaskTaskPackage> processingPackages = CollUtil.emptyIfNull(
            taskPackageMapper.selectByStatusAndCreatorRole(
                tenantId, StaskOrderStatus.PENDING_TECH_CONFIRM, StaskCreatorRole.PRODUCTION_ADMIN));
        List<SfStaskWorkOrder> pendingSplits = CollUtil.emptyIfNull(
            workOrderMapper.selectSplitOrdersByStatuses(tenantId, PENDING_ACCEPTANCE_STATUSES));
        List<SfStaskWorkOrder> processingSplits = CollUtil.emptyIfNull(
            workOrderMapper.selectSplitOrdersByStatuses(tenantId, PROCESSING_SPLIT_STATUSES));
        Date[] todayRange = SfStaskWorkbenchTimeSupport.todayRange();
        List<SfStaskWorkOrder> completedSplits = CollUtil.emptyIfNull(
            workOrderMapper.selectSplitOrdersAcceptedToday(
                tenantId, COMPLETED_STATUSES, todayRange[0], todayRange[1]));

        List<SfStaskTaskPackage> allPackages = Stream.concat(
            pendingPackages.stream(), processingPackages.stream()).toList();
        List<SfStaskWorkOrder> allSplits = Stream.of(
            pendingSplits.stream(), processingSplits.stream(), completedSplits.stream())
            .flatMap(stream -> stream).toList();
        QueryContext context = prepareContext(allPackages, allSplits);
        SfStaskPackageWorkbenchBatchReader.PackageData packageData = packageReader.load(tenantId, allPackages);
        SfStaskSplitWorkbenchBatchReader.SplitData splitData = splitReader.loadEvents(tenantId, allSplits, true);

        List<SfStaskHomeTaskCardVo> pendingCards = packageAssembler.managerPendingCards(
            pendingPackages, packageData, context.employeeNames(), context.roleNames());
        List<SfStaskHomeTaskCardVo> processingCards = packageAssembler.managerProcessingCards(
            processingPackages, packageData, context.employeeNames(), context.roleNames());
        List<SfStaskHomeTaskCardVo> allCards = Stream.concat(
            pendingCards.stream(), processingCards.stream()).toList();
        plantingBatchHelper.enrichHomeTaskCards(allCards);

        List<SfStaskWorkOrderVo> pendingVos = splitAssembler.managerOrders(
            pendingSplits, context.employees(), splitData);
        List<SfStaskWorkOrderVo> processingVos = splitAssembler.managerOrders(
            processingSplits, context.employees(), splitData);
        List<SfStaskWorkOrderVo> completedVos = splitAssembler.managerOrders(
            completedSplits, context.employees(), splitData);
        splitAssembler.applyCompletedAcceptance(completedVos, splitData);
        List<SfStaskWorkOrderVo> allVos = Stream.of(
            pendingVos.stream(), processingVos.stream(), completedVos.stream())
            .flatMap(stream -> stream).toList();
        plantingBatchHelper.enrichWorkOrderVos(allVos);

        SfStaskWorkbenchVo vo = new SfStaskWorkbenchVo();
        long pendingCount = pendingPackages.size() + pendingSplits.size();
        vo.setPendingCount(pendingCount);
        vo.setProcessingCount((long) processingPackages.size() + processingSplits.size());
        vo.setCompletedTodayCount((long) completedSplits.size());
        vo.setPendingReviewCount(pendingCount);
        vo.setPendingAcceptanceCount((long) pendingSplits.size());
        vo.setPendingTasks(pendingCards);
        vo.setPendingAcceptanceTasks(pendingVos);
        vo.setProcessingPackages(processingCards);
        vo.setProcessingTasks(processingVos);
        vo.setCompletedTodayTasks(completedVos);
        vo.setRows(allVos);
        return vo;
    }

    private List<SfStaskTaskPackage> loadPackages(String tenantId, Long employeeId, String tab) {
        if (TAB_PENDING.equals(tab)) {
            return CollUtil.emptyIfNull(
                taskPackageMapper.selectByCreatorAndStatuses(tenantId, employeeId, PENDING_PACKAGE_STATUSES));
        }
        if (TAB_PROCESSING.equals(tab)) {
            return CollUtil.emptyIfNull(
                taskPackageMapper.selectByStatusAndCreatorRole(
                    tenantId, StaskOrderStatus.PENDING_TECH_CONFIRM, StaskCreatorRole.PRODUCTION_ADMIN));
        }
        return List.of();
    }

    private List<SfStaskWorkOrder> loadSplits(String tenantId, String tab) {
        if (TAB_PENDING.equals(tab)) {
            return CollUtil.emptyIfNull(
                workOrderMapper.selectSplitOrdersByStatuses(tenantId, PENDING_ACCEPTANCE_STATUSES));
        }
        if (TAB_PROCESSING.equals(tab)) {
            return CollUtil.emptyIfNull(
                workOrderMapper.selectSplitOrdersByStatuses(tenantId, PROCESSING_SPLIT_STATUSES));
        }
        Date[] todayRange = SfStaskWorkbenchTimeSupport.todayRange();
        return CollUtil.emptyIfNull(workOrderMapper.selectSplitOrdersAcceptedToday(
            tenantId, COMPLETED_STATUSES, todayRange[0], todayRange[1]));
    }

    private QueryContext prepareContext(List<SfStaskTaskPackage> packages, List<SfStaskWorkOrder> splits) {
        SfStaskEmployeeQueryContext employees = employeeContextFactory.create();
        employees.preload(Stream.concat(
                packages.stream().flatMap(row -> Stream.of(
                    row.getCreatorEmployeeId(), row.getHandlerTechnicianEmployeeId())),
                splits.stream().flatMap(row -> Stream.of(
                    row.getLeaderId(), row.getCreatorEmployeeId(), row.getHandlerTechnicianEmployeeId())))
            .filter(Objects::nonNull)
            .distinct()
            .toList());
        Map<Long, String> employeeNames = new HashMap<>();
        employees.employees().forEach((id, employee) -> employeeNames.put(id, employee.getName()));
        Map<String, String> roleNames = roleNameReader.load(packages.stream()
            .map(SfStaskTaskPackage::getCreatorRoleCode)
            .filter(Objects::nonNull)
            .distinct()
            .toList());
        return new QueryContext(employees.employees(), employeeNames, roleNames);
    }

    private record QueryContext(Map<Long, SysEmployeeVo> employees, Map<Long, String> employeeNames,
                                Map<String, String> roleNames) {
    }
}
