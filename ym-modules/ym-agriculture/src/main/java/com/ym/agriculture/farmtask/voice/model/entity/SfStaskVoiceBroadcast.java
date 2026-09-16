package com.ym.agriculture.farmtask.voice.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * stask 任务维语语音播报，表 {@code sf_stask_voice_broadcast}。
 */
@Data
@TableName("sf_stask_voice_broadcast")
public class SfStaskVoiceBroadcast {

    /**
     * 播报记录主键。
     */
    @TableId("broadcast_id")
    private Long broadcastId;

    /**
     * 租户编号。
     */
    private String tenantId;

    /**
     * 工单 ID。
     */
    private Long orderId;

    /**
     * 中文任务说明。
     */
    private String sourceText;

    /**
     * 维语任务说明。
     */
    private String uyghurText;

    /**
     * 中文、当前维文术语、音色与缓存版本组合指纹 SHA-256。
     */
    private String textHash;

    /**
     * 播报语言；新记录固定为 ug-CN，历史 ug/uy 兼容读取。
     */
    private String language;

    /**
     * 讯飞音色。
     */
    private String voiceName;

    /**
     * 系统 OSS 文件 ID。
     */
    private Long ossId;

    /**
     * 音频播放地址。
     */
    private String audioUrl;

    /**
     * 生成状态：QUEUED/PROCESSING/SUCCESS/FAILED。
     */
    private String status;

    /**
     * 失败原因。
     */
    private String failReason;

    /**
     * 已领取生成尝试次数；默认最多为首次加三次重试，共四次。
     */
    private Integer retryCount;

    /**
     * 记录创建时间，格式：yyyy-MM-dd HH:mm:ss。
     */
    private Date createTime;

    /**
     * 记录最后更新时间，格式：yyyy-MM-dd HH:mm:ss。
     */
    private Date updateTime;
}
