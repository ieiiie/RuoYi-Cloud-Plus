package com.ym.agriculture.farmtask.workorder.support;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderEvent;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskAcceptance;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskClockRecord;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskCompletion;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskFlowLog;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * stask 工单关联记录索引器。
 *
 * <p>仅在内存中选择每个工单的最新业务记录，不访问数据库或外部服务。</p>
 */
public final class SfStaskWorkOrderRecordIndex {

    private SfStaskWorkOrderRecordIndex() {
    }

    /**
     * 按工单和业务事件建立最新流转日志索引。
     *
     * @param logs 流转日志
     * @return key 为“工单 ID:事件编码”的最新日志索引
     */
    public static Map<String, SfStaskFlowLog> latestFlowLogs(List<SfStaskFlowLog> logs) {
        Map<String, SfStaskFlowLog> index = new HashMap<>();
        if (CollUtil.isEmpty(logs)) {
            return index;
        }
        for (SfStaskFlowLog log : logs) {
            if (log.getOrderId() == null || StrUtil.isBlank(log.getEvent())) {
                continue;
            }
            String key = log.getOrderId() + ":" + log.getEvent();
            index.merge(key, log, SfStaskWorkOrderRecordIndex::laterFlowLog);
        }
        return index;
    }

    /**
     * 建立工单发出时间日志索引。
     *
     * @param logs 流转日志
     * @return 工单 ID 到发出日志的索引
     */
    public static Map<Long, SfStaskFlowLog> issuedAtLogs(List<SfStaskFlowLog> logs) {
        Map<Long, SfStaskFlowLog> index = new HashMap<>();
        if (CollUtil.isEmpty(logs)) {
            return index;
        }
        for (SfStaskFlowLog log : logs) {
            if (log.getOrderId() != null && StaskOrderStatus.PENDING_LEADER_ACCEPT.equals(log.getToStatus())) {
                index.merge(log.getOrderId(), log, SfStaskWorkOrderRecordIndex::laterFlowLog);
            }
        }
        for (SfStaskFlowLog log : logs) {
            if (log.getOrderId() != null && StaskOrderEvent.TECH_CONFIRM.equals(log.getEvent())) {
                index.putIfAbsent(log.getOrderId(), log);
            }
        }
        return index;
    }

    /**
     * 建立最新完工记录索引。
     *
     * @param completions 完工记录
     * @return 工单 ID 到最新完工记录的索引
     */
    public static Map<Long, SfStaskCompletion> latestCompletions(List<SfStaskCompletion> completions) {
        Map<Long, SfStaskCompletion> index = new HashMap<>();
        if (CollUtil.isEmpty(completions)) {
            return index;
        }
        for (SfStaskCompletion completion : completions) {
            if (completion.getOrderId() != null) {
                index.merge(completion.getOrderId(), completion, (existing, incoming) ->
                    later(incoming.getCompletedAt(), existing.getCompletedAt()) ? incoming : existing);
            }
        }
        return index;
    }

    /**
     * 建立最新打卡记录索引。
     *
     * @param records 打卡记录
     * @return 工单 ID 到最新打卡记录的索引
     */
    public static Map<Long, SfStaskClockRecord> latestClockRecords(List<SfStaskClockRecord> records) {
        Map<Long, SfStaskClockRecord> index = new HashMap<>();
        if (CollUtil.isEmpty(records)) {
            return index;
        }
        for (SfStaskClockRecord record : records) {
            if (record.getOrderId() != null) {
                index.merge(record.getOrderId(), record, (existing, incoming) ->
                    later(incoming.getClockTime(), existing.getClockTime()) ? incoming : existing);
            }
        }
        return index;
    }

    /**
     * 建立最新验收记录索引。
     *
     * @param rows 验收记录
     * @return 工单 ID 到最新验收记录的索引
     */
    public static Map<Long, SfStaskAcceptance> latestAcceptances(List<SfStaskAcceptance> rows) {
        Map<Long, SfStaskAcceptance> index = new HashMap<>();
        if (CollUtil.isEmpty(rows)) {
            return index;
        }
        for (SfStaskAcceptance row : rows) {
            if (row.getOrderId() != null) {
                index.merge(row.getOrderId(), row, (existing, incoming) ->
                    later(incoming.getAcceptedAt(), existing.getAcceptedAt()) ? incoming : existing);
            }
        }
        return index;
    }

    private static SfStaskFlowLog laterFlowLog(SfStaskFlowLog existing, SfStaskFlowLog incoming) {
        return later(incoming.getCreateTime(), existing.getCreateTime()) ? incoming : existing;
    }

    private static boolean later(Date candidate, Date current) {
        return candidate != null && (current == null || candidate.after(current));
    }
}
