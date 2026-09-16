package com.ym.agriculture.farmtask.inspectionaichat.service;

import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farmtask.inspectionaichat.model.bo.SfInspectionAiRegenerateBo;
import com.ym.agriculture.farmtask.inspectionaichat.model.bo.SfInspectionAiSendBo;
import com.ym.agriculture.farmtask.inspectionaichat.model.vo.SfInspectionAiConversationDetailVo;
import com.ym.agriculture.farmtask.inspectionaichat.model.vo.SfInspectionAiConversationVo;
import com.ym.agriculture.farmtask.inspectionaichat.model.vo.SfInspectionAiSendVo;

/** 巡查照片 AI 问答服务。 */
public interface ISfInspectionAiChatService {
    /** 分页查询当前租户会话。 */
    PageResult<SfInspectionAiConversationVo> queryPage(PageQuery pageQuery);
    /** 查询会话详情。 */
    SfInspectionAiConversationDetailVo getDetail(Long conversationId);
    /** 发送首问或续问。 */
    SfInspectionAiSendVo send(SfInspectionAiSendBo bo, String sseToken);
    /** 重新生成指定问题的回答。 */
    SfInspectionAiSendVo regenerate(Long userMessageId, SfInspectionAiRegenerateBo bo, String sseToken);
    /** 将超时的后台任务标记为失败。 */
    int failStaleGenerations();
}
