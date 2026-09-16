package com.ym.agriculture.farmtask.inspectionaichat.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/** 巡查照片 AI 会话。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_inspection_ai_conversation")
public class SfInspectionAiConversation extends TenantEntity {
    /** 会话主键。 */
    @TableId(value = "conversation_id", type = IdType.ASSIGN_ID)
    private Long conversationId;
    /** 会话标题。 */
    private String title;
    /** 首轮提问时间。 */
    private Date firstQuestionTime;
    /** 最后提问时间。 */
    private Date lastQuestionTime;
}
