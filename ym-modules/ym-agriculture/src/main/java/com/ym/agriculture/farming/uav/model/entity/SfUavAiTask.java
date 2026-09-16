package com.ym.agriculture.farming.uav.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.util.Date;

/**
 * UAV AI 分析任务映射：记录地块 + UAV 任务 + 算法平台任务号。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sf_uav_ai_task")
public class SfUavAiTask extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 地块 ID */
    private Long fieldId;

    /** 提交时地块上进行中种植批次 ID，无则 null */
    private Long plantingBatchId;

    /** 冗余：种植批次展示名（{@code sf_planting_batch.batch_code}，创建时快照） */
    private String plantingBatchName;

    /** 冗余：地块名称（创建时快照） */
    private String fieldName;

    private Long speciesId;

    private String speciesName;

    private Long varietyId;

    private String varietyName;

    /** UAV 任务 ID（jobId） */
    private String uavJobId;

    /** 算法平台任务号（taskNo） */
    private String algTaskNo;

    /** AI model number used by this analysis task. */
    private String modelNo;

    /** 简单状态标记：SUBMITTED / FINISHED / FAILED 等 */
    private String status;

    /** 推理类型：VIDEO（视频推理，默认）/ IMAGE（图片推理） */
    private String inferType;

    /** 计划调用中台停止的时刻；到期后由定时任务执行停止并置为 FINISHED */
    private Date scheduledStopAt;

    /** 分析时长（毫秒），与提交时业务起止时间一致，便于审计 */
    private Long analysisDurationMs;

    @TableLogic
    private String delFlag;

    private Long createDept;
    private Long createBy;
    private java.time.LocalDateTime createTime;
    private Long updateBy;
    private java.time.LocalDateTime updateTime;
    private String remark;
}
