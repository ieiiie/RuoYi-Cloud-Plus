package com.ym.agriculture.farmtask.voice.model.constants;

/**
 * stask 任务语音播报生成状态。
 */
public final class StaskVoiceBroadcastStatus {

    private StaskVoiceBroadcastStatus() {
    }

    /**
     * 已进入待处理队列，等待后台任务领取。
     */
    public static final String QUEUED = "QUEUED";

    /**
     * 后台任务已领取，正在准备翻译或语音合成。
     */
    public static final String PROCESSING = "PROCESSING";

    /**
     * 正在调用语音合成服务生成音频。
     */
    public static final String GENERATING = "GENERATING";

    /**
     * 音频生成并保存成功，可供客户端播放。
     */
    public static final String SUCCESS = "SUCCESS";

    /**
     * 本次生成失败，可根据重试策略再次处理。
     */
    public static final String FAILED = "FAILED";

    /**
     * 工单尚无语音播报记录，作为查询结果的派生状态返回。
     */
    public static final String NOT_GENERATED = "NOT_GENERATED";
}
