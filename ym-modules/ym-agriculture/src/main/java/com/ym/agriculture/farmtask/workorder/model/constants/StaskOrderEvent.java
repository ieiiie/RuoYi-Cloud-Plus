package com.ym.agriculture.farmtask.workorder.model.constants;

/**
 * stask 工单状态流转事件常量。
 */
public interface StaskOrderEvent {

    /**
     * 生产管理员提交任务包。
     */
    String SUBMIT_BY_MANAGER = "SUBMIT_BY_MANAGER";

    /**
     * 技术员直接派单。
     */
    String SUBMIT_BY_TECHNICIAN = "SUBMIT_BY_TECHNICIAN";

    /**
     * 技术员确认通过。
     */
    String TECH_CONFIRM = "TECH_CONFIRM";

    /**
     * 技术员退回。
     */
    String TECH_REJECT = "TECH_REJECT";

    /**
     * 组长接单。
     */
    String LEADER_ACCEPT = "LEADER_ACCEPT";

    /**
     * 派工人数达标（历史兼容事件）。
     */
    @Deprecated
    String DISPATCH_READY = "DISPATCH_READY";
    /**
     * 派工人数不足，重新补派（历史兼容事件）。
     */
    @Deprecated
    String DISPATCH_REOPEN = "DISPATCH_REOPEN";

    /**
     * 组长到岗打卡。
     */
    String CLOCK_IN = "CLOCK_IN";

    /**
     * 组长提交完工。
     */
    String COMPLETE = "COMPLETE";

    /**
     * 验收通过。
     */
    String ACCEPTANCE_PASS = "ACCEPTANCE_PASS";

    /**
     * 验收不通过。
     */
    String ACCEPTANCE_REJECT = "ACCEPTANCE_REJECT";

    /**
     * 组长重新申请验收。
     */
    String REAPPLY_ACCEPTANCE = "REAPPLY_ACCEPTANCE";

    /**
     * 撤销任务。
     */
    String CANCEL = "CANCEL";

    /**
     * 作废任务。
     */
    String VOID = "VOID";

    /**
     * 创建人撤回为草稿。
     */
    String WITHDRAW_TO_DRAFT = "WITHDRAW_TO_DRAFT";

    /**
     * 技术员撤回为待技术确认。
     */
    String WITHDRAW_TO_TECH_CONFIRM = "WITHDRAW_TO_TECH_CONFIRM";

    /**
     * 保存草稿。
     */
    String SAVE_DRAFT = "SAVE_DRAFT";
}
