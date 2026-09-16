package com.ym.agriculture.farming.integration.ai.algback.dto.task;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * {@code addAlgorithmTask} 成功时的 {@code data}；{@link #taskNo} 用于启停与排查。
 *
 * @author ym-cloud
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlgBackAddTaskResult {

    private String msg;

    /** 任务号；{@link com.ym.agriculture.farming.integration.ai.algback.api.AlgBackApi#setAlgorithmTaskStatus} 必填。 */
    private String taskNo;

    /** 计算流播放地址（中台拼接后完整 URL）。 */
    private String computingVideoPlayUrl;

    /** 原始推流侧播放地址。 */
    private String pushVideoPlayUrl;
}
