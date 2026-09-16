package com.ym.agriculture.farmtask.workorder.service.workbench;

import cn.hutool.core.collection.CollUtil;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderMapper;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskLeaderWorkbenchItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskLeaderWorkbenchSummaryVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskLeaderWorkbenchTasksVo;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeQueryContext;
import com.ym.agriculture.farmtask.workorder.support.StaskGreenhousePlantingBatchHelper;
import com.ym.agriculture.farmtask.workorder.support.workbench.SfStaskSplitWorkbenchAssembler;
import com.ym.agriculture.farmtask.workorder.support.workbench.SfStaskSplitWorkbenchBatchReader;
import com.ym.agriculture.farmtask.workorder.support.workbench.SfStaskWorkbenchEmployeeContextFactory;
import com.ym.agriculture.farmtask.workorder.support.workbench.SfStaskWorkbenchRoleNameReader;
import com.ym.agriculture.farmtask.workorder.support.workbench.SfStaskWorkbenchTimeSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 组长工作台查询服务。
 */
@Service
@RequiredArgsConstructor
public class SfStaskLeaderWorkbenchQueryService {

    private static final String TAB_PENDING_ACCEPT = "pending_accept";
    private static final String TAB_IN_PROGRESS = "in_progress";
    private static final String TAB_COMPLETED_TODAY = "completed_today";
    private static final List<String> IN_PROGRESS_STATUSES = List.of(
        StaskOrderStatus.ASSIGN_COMPLETE,
        StaskOrderStatus.LEADER_ARRIVED,
        StaskOrderStatus.PENDING_ACCEPTANCE,
        StaskOrderStatus.ACCEPTANCE_REJECTED);
    private static final List<String> COMPLETED_STATUSES = List.of(StaskOrderStatus.ACCEPTANCE_PASSED);

    private final com.ym.agriculture.shared.i18n.StaskMessageResolver messages;
    private final SfStaskWorkOrderMapper workOrderMapper;
    private final SfStaskSplitWorkbenchBatchReader splitReader;
    private final SfStaskWorkbenchEmployeeContextFactory employeeContextFactory;
    private final SfStaskWorkbenchRoleNameReader roleNameReader;
    private final SfStaskSplitWorkbenchAssembler splitAssembler;
    private final StaskGreenhousePlantingBatchHelper plantingBatchHelper;

    /**
     * 查询组长工作台统计。
     *
     * @param tenantId 当前租户
     * @param leaderId 当前组长
     * @return 工作台统计
     */
    public SfStaskLeaderWorkbenchSummaryVo summary(String tenantId, Long leaderId) {
        long pendingAccept = workOrderMapper.countByLeaderAndStatuses(
            tenantId, leaderId, List.of(StaskOrderStatus.PENDING_LEADER_ACCEPT));
        long inProgress = workOrderMapper.countByLeaderAndStatuses(tenantId, leaderId, IN_PROGRESS_STATUSES);
        var todayRange = SfStaskWorkbenchTimeSupport.todayRange();
        long completedToday = workOrderMapper.countLeaderSplitOrdersAcceptedToday(
            tenantId, leaderId, COMPLETED_STATUSES, todayRange[0], todayRange[1]);

        SfStaskLeaderWorkbenchSummaryVo vo = new SfStaskLeaderWorkbenchSummaryVo();
        vo.setPendingAcceptCount(pendingAccept);
        // 兼容字段保留，待组长派工流程已废弃。
        vo.setPendingAssignCount(0L);
        vo.setInProgressCount(inProgress);
        vo.setCompletedTodayCount(completedToday);
        vo.setPendingReviewCount(pendingAccept);
        vo.setProcessingCount(inProgress);
        return vo;
    }

    /**
     * 查询组长指定分栏任务。
     *
     * @param tenantId 当前租户
     * @param leaderId 当前组长
     * @param tab      分栏编码
     * @return 分栏任务
     */
    public SfStaskLeaderWorkbenchTasksVo tasks(String tenantId, Long leaderId, String tab) {
        String normalizedTab = normalizeTab(tab);
        boolean completedToday = TAB_COMPLETED_TODAY.equals(normalizedTab);
        List<SfStaskWorkOrder> splits = loadSplits(tenantId, leaderId, normalizedTab);
        SfStaskEmployeeQueryContext employees = employeeContextFactory.create();
        employees.preload(splits.stream()
            .flatMap(row -> Stream.of(row.getLeaderId(), row.getCreatorEmployeeId(),
                row.getHandlerTechnicianEmployeeId()))
            .filter(Objects::nonNull).distinct().toList());
        Map<String, String> roleNames = roleNameReader.load(splits.stream()
            .map(SfStaskWorkOrder::getCreatorRoleCode).filter(Objects::nonNull).distinct().toList());
        SfStaskSplitWorkbenchBatchReader.SplitData data = splitReader.loadEvents(tenantId, splits, completedToday);
        List<SfStaskLeaderWorkbenchItemVo> items = splitAssembler.leaderItems(
            splits, employees.employees(), roleNames, data);
        plantingBatchHelper.enrichLeaderWorkbenchItems(items);

        SfStaskLeaderWorkbenchTasksVo vo = new SfStaskLeaderWorkbenchTasksVo();
        vo.setTab(normalizedTab);
        vo.setItems(items);
        return vo;
    }

    private static List<String> statuses(String tab) {
        if (TAB_PENDING_ACCEPT.equals(tab)) {
            return List.of(StaskOrderStatus.PENDING_LEADER_ACCEPT);
        }
        if (TAB_COMPLETED_TODAY.equals(tab)) {
            return COMPLETED_STATUSES;
        }
        return IN_PROGRESS_STATUSES;
    }

    private List<SfStaskWorkOrder> loadSplits(String tenantId, Long leaderId, String tab) {
        if (!TAB_COMPLETED_TODAY.equals(tab)) {
            return CollUtil.emptyIfNull(workOrderMapper.selectSplitOrdersByLeaderAndStatuses(
                tenantId, leaderId, statuses(tab)));
        }
        var todayRange = SfStaskWorkbenchTimeSupport.todayRange();
        return CollUtil.emptyIfNull(workOrderMapper.selectLeaderSplitOrdersAcceptedToday(
            tenantId, leaderId, COMPLETED_STATUSES, todayRange[0], todayRange[1]));
    }

    private String normalizeTab(String tab) {
        if (StringUtils.isBlank(tab)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_WORKBENCH_TAB_REQUIRED);
        }
        String normalized = tab.trim().toLowerCase();
        if (TAB_PENDING_ACCEPT.equals(normalized) || TAB_IN_PROGRESS.equals(normalized)
            || TAB_COMPLETED_TODAY.equals(normalized)) {
            return normalized;
        }
        throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_WORKBENCH_LEADER_TAB_INVALID);
    }
}
