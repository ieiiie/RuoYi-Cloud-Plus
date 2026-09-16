package com.ym.agriculture.farming.integration.ai.algback.dto.imageinfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图片推理提交响应 {@code data} — 对应 {@code POST /imageInfer/submitTask} 的响应。
 *
 * @author ym-cloud
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlgBackImageInferSubmitResult {

    /** 任务 ID，后续查询/回调均使用此 ID */
    private String taskId;

    /** 任务初始状态，通常为 PENDING */
    private String status;

    /** 提交的图片数量 */
    private Integer imageCount;

    /** 提示信息 */
    private String msg;
}
