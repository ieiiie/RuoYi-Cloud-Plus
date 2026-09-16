package com.ym.agriculture.farming.uav.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 航线任务状态（与飞控 Wayline 任务枚举一致），用于理解 {@code getJobListPageVo} 等接口的 {@code status} 整型值。
 * <p>
 * 轮询拉媒体时，通常只选 {@link #SUCCESS}；若需在取消/失败等终态下仍尝试拉已上传片段，可配置包含
 * {@link #CANCEL}、{@link #FAILED} 等 {@code end=true} 的状态。
 */
@Getter
@AllArgsConstructor
public enum WaylineJobStatusEnum {

    PENDING(1, false),
    IN_PROGRESS(2, false),
    SUCCESS(3, true),
    CANCEL(4, true),
    FAILED(5, true),
    PAUSED(6, false),
    UNKNOWN(-1, true);

    private final int val;
    /** 是否终态（不再进行正常执行） */
    private final boolean end;

    public static WaylineJobStatusEnum find(int val) {
        return Arrays.stream(values())
            .filter(s -> s.val == val)
            .findFirst()
            .orElse(UNKNOWN);
    }
}
