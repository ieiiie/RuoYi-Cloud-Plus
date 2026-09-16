package com.ym.agriculture.farming.satellite.model.constants;

/**
 * 遥感任务主表 {@code sf_satellite_task.status} 数值约定（连续 0–3，方案 B）。
 * <p>
 * 由旧码迁移：原 0→0，原 1→1，原 2（遗留）→1，原 3→2，原 4→3。迁移脚本见 {@code sql/sf_satellite_task_status_remap_b.sql}。
 *
 * @author ym-cloud
 */
public final class SatelliteTaskStatus {

    /** 待提交（创建后、定时任务推送前） */
    public static final int PENDING_SUBMIT = 0;

    /** 已提交至外部遥感服务，等待结果回调（展示：处理中） */
    public static final int PROCESSING = 1;

    /** 回调处理成功 */
    public static final int SUCCESS = 2;

    /** 提交失败、回调失败或配置错误等 */
    public static final int FAILED = 3;

    private SatelliteTaskStatus() {
    }
}
