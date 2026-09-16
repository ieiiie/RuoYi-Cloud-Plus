package com.ym.agriculture.farmtask.workorder.support;

import cn.hutool.core.util.StrUtil;
import com.ym.common.core.exception.ServiceException;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderEvent;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * stask 工单轻量状态机。
 */
@Component
@lombok.RequiredArgsConstructor
public class SfStaskOrderStateMachine {

    private final com.ym.agriculture.shared.i18n.StaskMessageResolver messages;

    private static final String KEY_SEPARATOR = ":";

    private static final Map<String, String> TRANSITIONS = Map.ofEntries(
        transition(StaskOrderStatus.DRAFT, StaskOrderEvent.SUBMIT_BY_MANAGER, StaskOrderStatus.PENDING_TECH_CONFIRM),
        transition(StaskOrderStatus.DRAFT, StaskOrderEvent.SUBMIT_BY_TECHNICIAN, StaskOrderStatus.PENDING_LEADER_ACCEPT),
        transition(StaskOrderStatus.TECH_REJECTED, StaskOrderEvent.SUBMIT_BY_MANAGER, StaskOrderStatus.PENDING_TECH_CONFIRM),
        transition(StaskOrderStatus.DRAFT, StaskOrderEvent.CANCEL, StaskOrderStatus.CANCELLED),
        transition(StaskOrderStatus.PENDING_TECH_CONFIRM, StaskOrderEvent.TECH_CONFIRM, StaskOrderStatus.PENDING_LEADER_ACCEPT),
        transition(StaskOrderStatus.PENDING_TECH_CONFIRM, StaskOrderEvent.TECH_REJECT, StaskOrderStatus.TECH_REJECTED),
        transition(StaskOrderStatus.PENDING_TECH_CONFIRM, StaskOrderEvent.WITHDRAW_TO_DRAFT, StaskOrderStatus.DRAFT),
        transition(StaskOrderStatus.PENDING_TECH_CONFIRM, StaskOrderEvent.CANCEL, StaskOrderStatus.CANCELLED),
        transition(StaskOrderStatus.TECH_REJECTED, StaskOrderEvent.CANCEL, StaskOrderStatus.CANCELLED),
        transition(StaskOrderStatus.PENDING_TECH_CONFIRM, StaskOrderEvent.VOID, StaskOrderStatus.VOIDED),
        transition(StaskOrderStatus.PENDING_LEADER_ACCEPT, StaskOrderEvent.LEADER_ACCEPT, StaskOrderStatus.ASSIGN_COMPLETE),
        transition(StaskOrderStatus.PENDING_LEADER_ACCEPT, StaskOrderEvent.WITHDRAW_TO_DRAFT, StaskOrderStatus.DRAFT),
        transition(StaskOrderStatus.PENDING_LEADER_ACCEPT, StaskOrderEvent.WITHDRAW_TO_TECH_CONFIRM, StaskOrderStatus.PENDING_TECH_CONFIRM),
        transition(StaskOrderStatus.PENDING_LEADER_ACCEPT, StaskOrderEvent.VOID, StaskOrderStatus.VOIDED),
        // 保留历史状态和事件定义，兼容已发布的旧接口；新工单不会进入待组长派工。
        transition(StaskOrderStatus.PENDING_LEADER_ASSIGN, StaskOrderEvent.DISPATCH_READY, StaskOrderStatus.ASSIGN_COMPLETE),
        transition(StaskOrderStatus.PENDING_LEADER_ASSIGN, StaskOrderEvent.VOID, StaskOrderStatus.VOIDED),
        transition(StaskOrderStatus.ASSIGN_COMPLETE, StaskOrderEvent.DISPATCH_REOPEN, StaskOrderStatus.PENDING_LEADER_ASSIGN),
        transition(StaskOrderStatus.ASSIGN_COMPLETE, StaskOrderEvent.VOID, StaskOrderStatus.VOIDED),
        transition(StaskOrderStatus.ASSIGN_COMPLETE, StaskOrderEvent.CLOCK_IN, StaskOrderStatus.LEADER_ARRIVED),
        transition(StaskOrderStatus.LEADER_ARRIVED, StaskOrderEvent.COMPLETE, StaskOrderStatus.PENDING_ACCEPTANCE),
        transition(StaskOrderStatus.PENDING_ACCEPTANCE, StaskOrderEvent.ACCEPTANCE_PASS, StaskOrderStatus.ACCEPTANCE_PASSED),
        transition(StaskOrderStatus.PENDING_ACCEPTANCE, StaskOrderEvent.ACCEPTANCE_REJECT, StaskOrderStatus.ACCEPTANCE_REJECTED),
        transition(StaskOrderStatus.ACCEPTANCE_REJECTED, StaskOrderEvent.REAPPLY_ACCEPTANCE, StaskOrderStatus.PENDING_ACCEPTANCE)
    );

    /**
     * 根据当前状态和事件计算下一状态。
     *
     * @param currentStatus 当前状态
     * @param event         业务事件
     * @return 下一状态
     */
    public String transit(String currentStatus, String event) {
        String nextStatus = TRANSITIONS.get(key(currentStatus, event));
        if (StrUtil.isBlank(nextStatus)) {
            throw messages.exception(StaskMessageKeys.ERROR_INVALID_STATE);
        }
        return nextStatus;
    }

    private static Map.Entry<String, String> transition(String from, String event, String to) {
        return Map.entry(key(from, event), to);
    }

    private static String key(String status, String event) {
        return status + KEY_SEPARATOR + event;
    }
}
