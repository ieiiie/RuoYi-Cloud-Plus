package com.ym.agriculture.farmtask.workorder.service.workbench;

import cn.hutool.core.collection.CollUtil;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderMapper;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkbenchVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkOrderVo;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeQueryContext;
import com.ym.agriculture.farmtask.workorder.support.StaskGreenhousePlantingBatchHelper;
import com.ym.agriculture.farmtask.workorder.support.workbench.SfStaskSplitWorkbenchAssembler;
import com.ym.agriculture.farmtask.workorder.support.workbench.SfStaskSplitWorkbenchBatchReader;
import com.ym.agriculture.farmtask.workorder.support.workbench.SfStaskWorkbenchEmployeeContextFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 空角色、未知角色及组长旧版入口使用的通用兼容工作台查询。
 */
@Service
@RequiredArgsConstructor
public class SfStaskLegacyWorkbenchQueryService {

    private static final List<String> WORKBENCH_STATUSES = List.of(
        StaskOrderStatus.DRAFT,
        StaskOrderStatus.PENDING_TECH_CONFIRM,
        StaskOrderStatus.TECH_REJECTED,
        StaskOrderStatus.PENDING_LEADER_ACCEPT,
        StaskOrderStatus.ASSIGN_COMPLETE,
        StaskOrderStatus.LEADER_ARRIVED,
        StaskOrderStatus.PENDING_ACCEPTANCE,
        StaskOrderStatus.VOIDED);

    private final SfStaskWorkOrderMapper workOrderMapper;
    private final SfStaskSplitWorkbenchBatchReader splitReader;
    private final SfStaskWorkbenchEmployeeContextFactory employeeContextFactory;
    private final SfStaskSplitWorkbenchAssembler splitAssembler;
    private final StaskGreenhousePlantingBatchHelper plantingBatchHelper;

    /**
     * 查询当前员工旧版通用工作台。
     *
     * @param tenantId  当前租户
     * @param employeeId 当前员工
     * @return 兼容工作台
     */
    public SfStaskWorkbenchVo workbench(String tenantId, Long employeeId) {
        List<SfStaskWorkOrder> rows = CollUtil.emptyIfNull(
            workOrderMapper.selectByEmployeeAndStatuses(tenantId, employeeId, WORKBENCH_STATUSES));
        SfStaskEmployeeQueryContext employees = employeeContextFactory.create();
        employees.preload(rows.stream()
            .flatMap(row -> Stream.of(row.getLeaderId(), row.getCreatorEmployeeId(),
                row.getHandlerTechnicianEmployeeId()))
            .filter(Objects::nonNull).distinct().toList());
        List<SfStaskWorkOrderVo> result = splitAssembler.legacyOrders(
            rows, employees.employees(), splitReader.loadGreenhouses(tenantId, rows));
        plantingBatchHelper.enrichWorkOrderVos(result);

        SfStaskWorkbenchVo vo = new SfStaskWorkbenchVo();
        vo.setPendingReviewCount(rows.stream().filter(row ->
            StaskOrderStatus.PENDING_TECH_CONFIRM.equals(row.getStatus())
                || StaskOrderStatus.DRAFT.equals(row.getStatus())).count());
        vo.setPendingAcceptanceCount(rows.stream().filter(row ->
            StaskOrderStatus.PENDING_ACCEPTANCE.equals(row.getStatus())).count());
        vo.setProcessingCount(rows.stream().filter(SfStaskLegacyWorkbenchQueryService::isProcessing).count());
        vo.setRows(result);
        return vo;
    }

    private static boolean isProcessing(SfStaskWorkOrder row) {
        return !StaskOrderStatus.PENDING_TECH_CONFIRM.equals(row.getStatus())
            && !StaskOrderStatus.DRAFT.equals(row.getStatus())
            && !StaskOrderStatus.PENDING_ACCEPTANCE.equals(row.getStatus())
            && !StaskOrderStatus.VOIDED.equals(row.getStatus());
    }
}
