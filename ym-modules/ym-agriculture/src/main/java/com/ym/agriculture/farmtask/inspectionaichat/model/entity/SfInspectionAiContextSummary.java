package com.ym.agriculture.farmtask.inspectionaichat.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/** 巡查照片 AI 历史上下文摘要。 */
@Data
@TableName("sf_inspection_ai_context_summary")
public class SfInspectionAiContextSummary {
    /** 摘要主键。 */
    @TableId(value = "summary_id", type = IdType.ASSIGN_ID)
    private Long summaryId;
    /** 租户编号。 */
    private String tenantId;
    /** 会话主键。 */
    private Long conversationId;
    /** 已覆盖最大消息序号。 */
    private Integer coveredThroughMessageSeq;
    /** 结构化摘要。 */
    private String content;
    /** 实际模型。 */
    private String modelId;
    /** 是否使用备选模型。 */
    private Boolean fallbackUsed;
    /** 服务端请求标识。 */
    private String providerRequestId;
    /** 输入 Token。 */
    private Integer inputTokens;
    /** 输出 Token。 */
    private Integer outputTokens;
    /** 总 Token。 */
    private Integer totalTokens;
    /** 生成状态。 */
    private String generationStatus;
    /** 错误码。 */
    private String errorCode;
    /** 错误说明。 */
    private String errorMessage;
    /** 开始时间。 */
    private Date startedTime;
    /** 完成时间。 */
    private Date completedTime;
    /** 创建时间。 */
    private Date createTime;
    /** 更新时间。 */
    private Date updateTime;
}
