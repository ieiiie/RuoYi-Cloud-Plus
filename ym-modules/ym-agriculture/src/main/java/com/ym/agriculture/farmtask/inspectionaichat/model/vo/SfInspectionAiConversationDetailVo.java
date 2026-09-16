package com.ym.agriculture.farmtask.inspectionaichat.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/** AI 会话详情。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SfInspectionAiConversationDetailVo extends SfInspectionAiConversationVo {
    /** 按会话顺序排列的消息。 */
    private List<SfInspectionAiMessageVo> messages;
}
