package com.ym.agriculture.farmtask.inspectionaichat.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.ym.common.core.domain.R;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farmtask.inspectionaichat.model.bo.SfInspectionAiRegenerateBo;
import com.ym.agriculture.farmtask.inspectionaichat.model.bo.SfInspectionAiSendBo;
import com.ym.agriculture.farmtask.inspectionaichat.model.vo.SfInspectionAiConversationDetailVo;
import com.ym.agriculture.farmtask.inspectionaichat.model.vo.SfInspectionAiConversationVo;
import com.ym.agriculture.farmtask.inspectionaichat.model.vo.SfInspectionAiSendVo;
import com.ym.agriculture.farmtask.inspectionaichat.service.ISfInspectionAiChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 巡查照片归档 AI 问答接口。 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/smart-farming/inspection-ai")
public class SfInspectionAiChatController {
    private final ISfInspectionAiChatService chatService;

    /** 分页查询当前租户共享会话。 */
    @SaCheckPermission("smartfarming:inspectionPhotoArchive:aiChat")
    @GetMapping("/conversations")
    public R<PageResult<SfInspectionAiConversationVo>> list(PageQuery pageQuery) {
        return R.ok(chatService.queryPage(pageQuery));
    }

    /** 查询会话、消息和附件快照。 */
    @SaCheckPermission("smartfarming:inspectionPhotoArchive:aiChat")
    @GetMapping("/conversations/{conversationId}")
    public R<SfInspectionAiConversationDetailVo> detail(@NotNull @PathVariable Long conversationId) {
        return R.ok(chatService.getDetail(conversationId));
    }

    /** 创建首问或继续历史会话。 */
    @SaCheckPermission("smartfarming:inspectionPhotoArchive:aiChat")
    @Log(title = "巡查照片 AI 问答", businessType = BusinessType.INSERT)
    @PostMapping("/conversations/messages")
    public R<SfInspectionAiSendVo> send(@Valid @RequestBody SfInspectionAiSendBo bo) {
        return R.ok(chatService.send(bo, StpUtil.getTokenValue()));
    }

    /** 重新生成用户问题的回答。 */
    @SaCheckPermission("smartfarming:inspectionPhotoArchive:aiChat")
    @Log(title = "巡查照片 AI 重新生成", businessType = BusinessType.UPDATE)
    @PostMapping("/conversations/messages/{userMessageId}/regenerate")
    public R<SfInspectionAiSendVo> regenerate(@NotNull @PathVariable Long userMessageId,
                                               @Valid @RequestBody SfInspectionAiRegenerateBo bo) {
        return R.ok(chatService.regenerate(userMessageId, bo, StpUtil.getTokenValue()));
    }
}
