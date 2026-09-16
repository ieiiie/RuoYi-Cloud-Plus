package com.ym.agriculture.farmtask.workorder.model.constants;

/**
 * stask 工单状态常量。
 */
public interface StaskOrderStatus {

    /**
     * 草稿。
     */
    String DRAFT = "DRAFT";

    /**
     * 待技术员确认。
     */
    String PENDING_TECH_CONFIRM = "PENDING_TECH_CONFIRM";

    /**
     * 技术员已退回。
     */
    String TECH_REJECTED = "TECH_REJECTED";

    /**
     * 待组长接单。
     */
    String PENDING_LEADER_ACCEPT = "PENDING_LEADER_ACCEPT";

    /**
     * 待组长派工（历史兼容状态，新工单不再进入）。
     */
    @Deprecated
    String PENDING_LEADER_ASSIGN = "PENDING_LEADER_ASSIGN";

    /**
     * 派工完成。
     */
    String ASSIGN_COMPLETE = "ASSIGN_COMPLETE";

    /**
     * 组长已到岗。
     */
    String LEADER_ARRIVED = "LEADER_ARRIVED";

    /**
     * 待验收。
     */
    String PENDING_ACCEPTANCE = "PENDING_ACCEPTANCE";

    /**
     * 验收通过。
     */
    String ACCEPTANCE_PASSED = "ACCEPTANCE_PASSED";

    /**
     * 验收不通过。
     */
    String ACCEPTANCE_REJECTED = "ACCEPTANCE_REJECTED";

    /**
     * 已撤销。
     */
    String CANCELLED = "CANCELLED";

    /**
     * 已作废。
     */
    String VOIDED = "VOIDED";

    /**
     * 判断拆分工单是否仍处于创建人可以作废的执行前状态。
     *
     * @param status 工单状态
     * @return 是否允许创建人作废
     */
    static boolean isCreatorVoidableBeforeExecution(String status) {
        return PENDING_LEADER_ACCEPT.equals(status) || ASSIGN_COMPLETE.equals(status);
    }
}
