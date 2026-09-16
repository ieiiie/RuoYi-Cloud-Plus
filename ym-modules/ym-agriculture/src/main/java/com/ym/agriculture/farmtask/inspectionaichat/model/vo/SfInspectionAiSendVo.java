package com.ym.agriculture.farmtask.inspectionaichat.model.vo;

import lombok.Data;

/** 创建 AI 生成任务后的即时响应。 */
@Data
public class SfInspectionAiSendVo {
    /** SSE 请求标识。 */
    private String requestId;
    /** 会话摘要。 */
    private SfInspectionAiConversationVo conversation;
    /** 刚保存的用户消息。 */
    private SfInspectionAiMessageVo userMessage;
    /** 刚创建的助手占位消息。 */
    private SfInspectionAiMessageVo assistantMessage;
}
