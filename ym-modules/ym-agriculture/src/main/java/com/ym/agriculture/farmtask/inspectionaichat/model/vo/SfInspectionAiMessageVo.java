package com.ym.agriculture.farmtask.inspectionaichat.model.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/** AI 会话消息视图。 */
@Data
public class SfInspectionAiMessageVo {
    /** 消息主键。 */
    private Long messageId;
    /** USER 或 ASSISTANT。 */
    private String role;
    /** 消息正文。 */
    private String content;
    /** 生成状态。 */
    private String generationStatus;
    /** 回复的用户消息。 */
    private Long replyToMessageId;
    /** 实际模型。 */
    private String modelId;
    /** 输入 Token。 */
    private Integer inputTokens;
    /** 输出 Token。 */
    private Integer outputTokens;
    /** 总 Token。 */
    private Integer totalTokens;
    /** 可展示失败说明。 */
    private String errorMessage;
    /** 创建时间。 */
    private Date createTime;
    /** 附件快照。 */
    private List<SfInspectionAiAttachmentVo> attachments;
}
