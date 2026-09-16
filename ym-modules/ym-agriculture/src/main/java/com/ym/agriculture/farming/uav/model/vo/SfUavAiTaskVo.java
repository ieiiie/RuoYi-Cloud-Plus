package com.ym.agriculture.farming.uav.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ym.agriculture.farming.uav.model.entity.SfUavAiTask;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * UAV 媒体 AI 分析任务只读视图，对应表 {@code sf_uav_ai_task}。
 * <p>
 * 供 {@code GET /smart-farming/uav/ai-tasks/page}、{@code GET .../ai-tasks/{id}} 等接口出参；
 * 字段与表结构一致，含创建时写入的冗余快照（地块/批次/作物），便于列表直接展示而无需再 JOIN。
 * {@link #inferenceLogs} 仅详情接口填充，与 {@link #algTaskNo} 关联 {@code sf_ai_inference_log.task_no}。
 *
 * @author ym-cloud
 */
@Data
@AutoMapper(target = SfUavAiTask.class)
public class SfUavAiTaskVo implements Serializable {

    /** 旧平台停用说明；不覆盖历史任务的原始执行状态。 */
    public boolean isLegacyPlatformRetired() { return true; }

    public String getLegacyPlatformMessage() { return "旧无人机平台已停用，仅提供历史查询"; }

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键，对应 {@code sf_uav_ai_task.id} */
    private Long id;

    /** 租户编号 */
    private String tenantId;

    /** 业务地块 ID，对应 {@code sf_field.field_id} */
    private Long fieldId;

    /**
     * 提交分析时地块上「进行中」种植批次主键；无进行中批次时为 null。
     * 与 {@link #plantingBatchName} 同为创建时刻快照。
     */
    private Long plantingBatchId;

    /**
     * 冗余：种植批次展示名，创建时取自 {@code sf_planting_batch.batch_code}（业务上作批次名称/编号）。
     */
    private String plantingBatchName;

    /** 冗余：地块名称，创建时取自 {@code sf_field.field_name} */
    private String fieldName;

    /** 冗余：物种 ID，由当时批次关联品种解析 */
    private Long speciesId;

    /** 冗余：物种名称 */
    private String speciesName;

    /** 冗余：品种 ID */
    private Long varietyId;

    /** 冗余：品种名称 */
    private String varietyName;

    /**
     * 本地 UAV 飞行任务 ID，与 {@code sf_uav_flight_task.job_id} 一致，用于关联媒体与飞控任务。
     */
    private String uavJobId;

    /** 算法中台返回的任务编号（taskNo），用于查询分析结果与停止任务 */
    private String algTaskNo;

    /** AI model number used by this analysis task. */
    @JsonProperty("model_no")
    private String modelNo;

    /**
     * 任务状态，如 {@code SUBMITTED}（已提交）、{@code FINISHED}（已完成）、{@code FAILED}（失败）等。
     */
    private String status;

    /**
     * 计划调用算法中台「停止」接口的时刻；到期后由调度任务置为完成态（若配置了分析时长）。
     */
    private Date scheduledStopAt;

    /**
     * 分析时长（毫秒），与提交时传入的业务起止时间一致，仅作审计与展示。
     */
    private Long analysisDurationMs;

    /** 创建部门 ID */
    private Long createDept;

    /** 创建人用户 ID */
    private Long createBy;

    /** 记录创建时间 */
    private Date createTime;

    /** 更新人用户 ID */
    private Long updateBy;

    /** 记录最后更新时间 */
    private Date updateTime;

    /** 备注 */
    private String remark;

    /**
     * 该次 AI 分析在 {@code sf_ai_inference_log} 中的回调流水；列表分页接口不查库，一般为 null 或空列表。
     */
    private List<SfUavAiInferenceLogVo> inferenceLogs;
}
