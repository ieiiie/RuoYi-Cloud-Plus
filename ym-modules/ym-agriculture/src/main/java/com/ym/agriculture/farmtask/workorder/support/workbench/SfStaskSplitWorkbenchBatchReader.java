package com.ym.agriculture.farmtask.workorder.support.workbench;

import cn.hutool.core.collection.CollUtil;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskAcceptanceMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskClockRecordMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskCompletionMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskFlowLogMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderGreenhouseMapper;
import com.ym.agriculture.farmtask.leaderlabor.dao.SfStaskLeaderLaborRecordMapper;
import com.ym.agriculture.farmtask.leaderlabor.model.entity.SfStaskLeaderLaborRecord;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskAcceptance;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskClockRecord;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskCompletion;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskFlowLog;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrderGreenhouse;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskGreenhouseBriefVo;
import com.ym.agriculture.farmtask.workorder.support.SfStaskWorkOrderAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.time.ZoneId;

/**
 * 批量读取拆分工单工作台所需的流程、打卡、完工和验收数据。
 */
@Component
@RequiredArgsConstructor
public class SfStaskSplitWorkbenchBatchReader {

    private final SfStaskFlowLogMapper flowLogMapper;
    private final SfStaskCompletionMapper completionMapper;
    private final SfStaskClockRecordMapper clockRecordMapper;
    private final SfStaskAcceptanceMapper acceptanceMapper;
    private final SfStaskWorkOrderGreenhouseMapper greenhouseMapper;
    private final SfStaskLeaderLaborRecordMapper laborRecordMapper;

    /**
     * 一次加载拆分工单事件数据；同一入口的流程日志只查询一次并建立多个内存索引。
     *
     * @param tenantId         当前租户
     * @param orders           拆分工单集合
     * @param includeAcceptance 是否加载验收记录
     * @return 拆分工单批量数据
     */
    public SplitData loadEvents(String tenantId, Collection<SfStaskWorkOrder> orders, boolean includeAcceptance) {
        List<Long> orderIds = orderIds(orders);
        if (orderIds.isEmpty()) {
            return SplitData.empty();
        }
        List<SfStaskFlowLog> logs = CollUtil.emptyIfNull(flowLogMapper.selectByOrderIds(tenantId, orderIds));
        List<SfStaskCompletion> completions =
            CollUtil.emptyIfNull(completionMapper.selectByOrderIds(tenantId, orderIds));
        List<SfStaskClockRecord> clocks =
            CollUtil.emptyIfNull(clockRecordMapper.selectByOrderIds(tenantId, orderIds));
        List<SfStaskAcceptance> acceptances = includeAcceptance
            ? CollUtil.emptyIfNull(acceptanceMapper.selectByOrderIds(tenantId, orderIds))
            : List.of();
        List<Long> leaderIds = orders.stream().map(SfStaskWorkOrder::getLeaderId).filter(Objects::nonNull).distinct().toList();
        List<java.time.LocalDate> planDates = orders.stream().filter(order -> order.getPlanDate() != null)
            .map(order -> order.getPlanDate().toInstant().atZone(ZoneId.of("Asia/Shanghai")).toLocalDate()).distinct().toList();
        Map<String, SfStaskLeaderLaborRecord> laborRecords = new HashMap<>();
        for (SfStaskLeaderLaborRecord labor : laborRecordMapper.selectByLeadersAndPlanDates(tenantId, leaderIds, planDates)) {
            laborRecords.put(laborKey(labor.getLeaderEmployeeId(), labor.getPlanDate()), labor);
        }
        return new SplitData(
            SfStaskWorkOrderAssembler.indexLatestFlowLogs(logs),
            SfStaskWorkOrderAssembler.indexIssuedAtLogs(logs),
            SfStaskWorkOrderAssembler.indexLatestCompletions(completions),
            SfStaskWorkOrderAssembler.indexLatestClockRecords(clocks),
            SfStaskWorkOrderAssembler.indexLatestAcceptances(acceptances), laborRecords);
    }

    /**
     * 一次加载拆分工单棚室摘要，供兼容工作台列表使用。
     *
     * @param tenantId 当前租户
     * @param orders   拆分工单集合
     * @return 工单 ID 到棚室摘要列表的映射
     */
    public Map<Long, List<SfStaskGreenhouseBriefVo>> loadGreenhouses(
        String tenantId, Collection<SfStaskWorkOrder> orders) {
        List<Long> orderIds = orderIds(orders);
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        List<SfStaskWorkOrderGreenhouse> rows =
            CollUtil.emptyIfNull(greenhouseMapper.selectByOrderIds(tenantId, orderIds));
        Map<Long, Map<Long, SfStaskGreenhouseBriefVo>> grouped = new LinkedHashMap<>();
        for (Long orderId : orderIds) {
            grouped.put(orderId, new LinkedHashMap<>());
        }
        for (SfStaskWorkOrderGreenhouse row : rows) {
            if (row.getOrderId() == null || row.getGreenhouseId() == null) {
                continue;
            }
            grouped.computeIfAbsent(row.getOrderId(), key -> new LinkedHashMap<>())
                .putIfAbsent(row.getGreenhouseId(), SfStaskWorkOrderAssembler.toGreenhouseBrief(row));
        }
        Map<Long, List<SfStaskGreenhouseBriefVo>> result = new HashMap<>();
        grouped.forEach((orderId, index) -> result.put(orderId, new ArrayList<>(index.values())));
        return result;
    }

    private static List<Long> orderIds(Collection<SfStaskWorkOrder> orders) {
        return (orders == null ? List.<SfStaskWorkOrder>of() : orders).stream()
            .map(SfStaskWorkOrder::getOrderId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    private static String laborKey(Long leaderId, java.time.LocalDate planDate) { return leaderId + "_" + planDate; }

    /** 拆分工单工作台所需的批量事件索引。 */
    public record SplitData(
        Map<String, SfStaskFlowLog> flowLogs,
        Map<Long, SfStaskFlowLog> issuedLogs,
        Map<Long, SfStaskCompletion> completions,
        Map<Long, SfStaskClockRecord> clocks,
        Map<Long, SfStaskAcceptance> acceptances,
        Map<String, SfStaskLeaderLaborRecord> laborRecords) {

        /** 返回不包含事件数据的空批次。 */
        public static SplitData empty() {
            return new SplitData(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        }
    }
}
