package com.ym.agriculture.farmtask.workorder.model.constants;

/**
 * stask 派工明细状态常量。
 */
public interface StaskDispatchStatus {

    /**
     * 待工人确认。
     */
    String PENDING = "PENDING";

    /**
     * 工人已接受。
     */
    String ACCEPTED = "ACCEPTED";

    /**
     * 工人已拒绝。
     */
    String REJECTED = "REJECTED";

    /**
     * 组长已撤销邀请。
     */
    String CANCELLED = "CANCELLED";
}
