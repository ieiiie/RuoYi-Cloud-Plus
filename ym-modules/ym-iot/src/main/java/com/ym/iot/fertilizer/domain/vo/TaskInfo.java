package com.ym.iot.fertilizer.domain.vo;

import lombok.Data;

import java.util.Date;
import java.util.UUID;

/**
 * 异步任务信息。
 *
 * <p>控制类接口（POST /start、/stop 等）耗时可能数十秒，
 * 因此采用异步任务模式：接口立即返回含 taskId 的 TaskInfo，
 * 前端通过 {@code GET /api/fertilizer/task/{taskId}} 轮询进度。
 *
 * <h3>状态流转</h3>
 * QUEUED → RUNNING → SUCCEEDED 或 FAILED
 *
 * @author ym-cloud
 */
@Data
public class TaskInfo {

    /** 任务 ID（UUID 前 8 位），前端轮询进度用 */
    private String taskId;

    /** 目标设备主键（iot_device.id） */
    private Long deviceId;

    /** 任务标签，如 "一键启动"、"有序停止"、"紧急停止" */
    private String label;

    /**
     * 任务状态：QUEUED（排队中）、RUNNING（执行中）、
     * SUCCEEDED（成功）、FAILED（失败）
     */
    private String status;

    /** 当前步骤描述，如 "参数下发与回读"、"等待设备进入运行态..." */
    private String step;

    /** 进度百分比 0~100 */
    private int progress;

    /** 成功时返回的结果文本 */
    private String result;

    /** 成功时返回的业务数据，如读取参数任务返回的设备快照 */
    private Object data;

    /** 失败时的错误信息 */
    private String error;

    /** 任务创建时间 */
    private Date createdAt;

    /** 任务完成时间（成功或失败后设置） */
    private Date finishedAt;

    /** 创建新任务，初始状态 QUEUED */
    public static TaskInfo create(Long deviceId, String label) {
        TaskInfo t = new TaskInfo();
        t.taskId = UUID.randomUUID().toString().substring(0, 8);
        t.deviceId = deviceId;
        t.label = label;
        t.status = "QUEUED";
        t.progress = 0;
        t.createdAt = new Date();
        return t;
    }
}
