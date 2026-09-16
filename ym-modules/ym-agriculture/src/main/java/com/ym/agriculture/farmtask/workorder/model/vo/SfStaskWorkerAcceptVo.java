package com.ym.agriculture.farmtask.workorder.model.vo;

import com.ym.agriculture.farmtask.voice.model.vo.SfStaskVoiceBroadcastVo;
import lombok.Data;

/**
 * 工人接收任务结果视图对象。
 */
@Data
public class SfStaskWorkerAcceptVo {

    /**
     * 派工明细 ID。
     */
    private Long dispatchId;

    /**
     * 工单 ID。
     */
    private Long orderId;

    /**
     * 是否已接收成功。
     */
    private Boolean accepted;

    /**
     * 任务维度维语播报信息。
     */
    private SfStaskVoiceBroadcastVo voiceBroadcast;
}
