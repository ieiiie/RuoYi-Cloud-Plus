package com.ym.agriculture.farmtask.voice.model.vo;

import com.ym.agriculture.farmtask.voice.model.entity.SfStaskVoiceBroadcast;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

/**
 * stask 任务语音播报返回对象。
 */
@Data
@AutoMapper(target = SfStaskVoiceBroadcast.class)
public class SfStaskVoiceBroadcastVo {

    /**
     * 播报记录主键。
     */
    private Long broadcastId;

    /**
     * 关联工单ID。
     */
    private Long orderId;

    /**
     * 中文任务说明。
     */
    private String sourceText;

    /**
     * 维吾尔语任务说明。
     */
    private String uyghurText;

    /**
     * 语音文件对应的系统OSS文件ID。
     */
    private Long ossId;

    /**
     * 语音音频播放地址。
     */
    private String audioUrl;

    /**
     * 播报语言编码。
     */
    private String language;

    /**
     * 讯飞语音合成音色名称。
     */
    private String voiceName;

    /**
     * 生成状态：QUEUED、PROCESSING、GENERATING、SUCCESS、FAILED 或 NOT_GENERATED。
     */
    private String status;

    /**
     * 生成失败原因；非失败状态时为空。
     */
    private String failReason;
}
