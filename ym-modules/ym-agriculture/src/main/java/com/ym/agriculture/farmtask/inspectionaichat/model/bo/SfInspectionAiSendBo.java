package com.ym.agriculture.farmtask.inspectionaichat.model.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/** 发送巡查照片 AI 问题。 */
@Data
public class SfInspectionAiSendBo {
    /** 已有会话；首问不传。 */
    private Long conversationId;
    /** 前端生成的幂等 UUID。 */
    @NotBlank(message = "请求标识不能为空")
    private String clientRequestId;
    /** 中文问题。 */
    @NotBlank(message = "问题不能为空")
    private String content;
    /** 本轮附带的归档照片主键；续问可传空列表。 */
    private List<Long> photoIds;
}
