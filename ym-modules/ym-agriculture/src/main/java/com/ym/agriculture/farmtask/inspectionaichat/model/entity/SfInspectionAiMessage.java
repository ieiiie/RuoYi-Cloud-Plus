package com.ym.agriculture.farmtask.inspectionaichat.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/** 巡查照片 AI 消息。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_inspection_ai_message")
public class SfInspectionAiMessage extends TenantEntity {
    /** 消息主键。 */
    @TableId(value = "message_id", type = IdType.ASSIGN_ID)
    private Long messageId;
    /** 所属会话。 */
    private Long conversationId;
    /** 会话内顺序。 */
    private Integer messageSeq;
    /** USER 或 ASSISTANT。 */
    private String role;
    /** 消息正文。 */
    private String content;
    /** 用户请求幂等 UUID。 */
    private String clientRequestId;
    /** 助手回答对应的用户消息。 */
    private Long replyToMessageId;
    /** 同一问题生成次数。 */
    private Integer generationAttempt;
    /** 生成状态。 */
    private String generationStatus;
    /** 实际模型。 */
    private String modelId;
    /** 模型服务请求标识。 */
    private String providerRequestId;
    /** 输入 Token。 */
    private Integer inputTokens;
    /** 输出 Token。 */
    private Integer outputTokens;
    /** 总 Token。 */
    private Integer totalTokens;
    /** 脱敏错误码。 */
    private String errorCode;
    /** 脱敏错误说明。 */
    private String errorMessage;
    /** 生成开始时间。 */
    private Date startedTime;
    /** 生成完成时间。 */
    private Date completedTime;
}
