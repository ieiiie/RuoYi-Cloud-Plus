package com.ym.agriculture.farming.integration.ai.algback.dto.imageinfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图片推理任务查询结果 — 对应 {@code GET /imageInfer/taskResult} 响应 {@code data}。
 *
 * @author ym-cloud
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlgBackImageInferTaskResult {

    private String taskId;

    private String status;

    private String statusMsg;

    private String modelNo;

    private Integer imageCount;

    private String inputMode;

    /** 原样透传的业务参数 */
    private Object bizParams;

    private String submitTime;

    private String updateTime;

    private String completeTime;

    /** 推理结果（结构因单张/多张而异），用 JsonNode 接收 */
    private JsonNode result;
}
