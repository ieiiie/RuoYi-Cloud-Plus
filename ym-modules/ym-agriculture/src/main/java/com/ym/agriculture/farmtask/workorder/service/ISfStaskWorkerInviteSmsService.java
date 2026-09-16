package com.ym.agriculture.farmtask.workorder.service;

/**
 * stask 工人派工邀请短信批量发送服务。
 */
public interface ISfStaskWorkerInviteSmsService {

    /**
     * 扫描待确认且未发短信的派工邀请，按工人合并发送并标记已发。
     */
    void flushPendingInvites();
}
