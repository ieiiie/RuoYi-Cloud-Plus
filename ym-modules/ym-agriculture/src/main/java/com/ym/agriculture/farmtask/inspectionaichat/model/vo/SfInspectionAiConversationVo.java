package com.ym.agriculture.farmtask.inspectionaichat.model.vo;

import lombok.Data;

import java.util.Date;

/** AI 会话列表项。 */
@Data
public class SfInspectionAiConversationVo {
    /** 会话主键。 */
    private Long conversationId;
    /** 会话标题。 */
    private String title;
    /** 创建人。 */
    private Long createBy;
    /** 首次提问时间。 */
    private Date firstQuestionTime;
    /** 最后提问时间。 */
    private Date lastQuestionTime;
    /** 创建时间。 */
    private Date createTime;
}
