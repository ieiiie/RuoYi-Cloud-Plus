package com.ym.agriculture.farmtask.voice.service;

import com.ym.agriculture.farmtask.voice.model.vo.SfStaskVoiceBroadcastVo;

/**
 * stask 任务维语语音播报服务。
 */
public interface ISfStaskVoiceBroadcastService {

    /**
     * 组长接单后预生成工单播报音频。
     *
     * @param orderId 工单 ID
     */
    void preGenerateForOrder(Long orderId);

    /**
     * 后台 worker 处理待生成播报记录。
     *
     * @return 本轮成功领取的记录数
     */
    int processPendingBroadcasts();

    /**
     * 查询工单当前内容指纹对应的最新播报；指纹变化时重新排队。
     *
     * @param orderId 工单 ID
     * @return 播报记录；无记录时返回 NOT_GENERATED
     */
    SfStaskVoiceBroadcastVo queryLatestForOrder(Long orderId);
}
