package com.ym.agriculture.farming.uav.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ym.agriculture.farming.uav.model.entity.SfUavFlightTask;
import com.ym.agriculture.farming.uav.support.SfUavAiModelNoSupport;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 本地飞行任务只读视图，对应表 {@code sf_uav_flight_task}。
 * <p>
 * 供 {@code GET /smart-farming/uav/local-flight-tasks/page}、{@code .../{id}} 等接口出参；字段与落库结构一致。
 *
 * @author ym-cloud
 */
@Data
@AutoMapper(target = SfUavFlightTask.class)
public class SfUavFlightTaskVo implements Serializable {

    /** 旧平台停用说明；不覆盖历史任务的原始执行状态。 */
    public boolean isLegacyPlatformRetired() { return true; }

    public String getLegacyPlatformMessage() { return "旧无人机平台已停用，仅提供历史查询"; }

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 租户编号 */
    private String tenantId;

    /** 业务地块 ID */
    private Long fieldId;

    /** 冗余：地块名称（创建时快照） */
    private String fieldName;

    private Long varietyId;

    private String varietyName;

    /** 创建时地块进行中种植批次 ID，无则 null */
    private Long plantingBatchId;

    /** 冗余：种植批次展示名（{@code sf_planting_batch.batch_code} 快照） */
    private String plantingBatchName;

    /** 飞控工作空间 ID（UUID） */
    private String workspaceId;

    /** 飞控任务名称（列表接口 job_name） */
    private String uavJobName;

    /** 飞控任务 ID（列表接口 job_id） */
    private String uavJobId;

    /** 任务名称（本地往往在原始名称后拼接 jobId） */
    private String taskName;

    /** 飞控任务状态（枚举数值） */
    private Integer status;

    /** 执行进度，0–100 */
    private Integer progress;

    /** 飞控用户名（英文） */
    private String username;

    /** 飞控业务/错误码 */
    private Integer code;

    /** 上传状态（按飞控定义） */
    private Integer uploading;

    /** 条件/就绪信息原始 JSON 字符串 */
    private String conditions;

    /** 任务来源：1 平台，2 物联感知 等 */
    private Integer source;

    /** 航线文件 ID */
    private String fileId;

    /** 航线文件名称 */
    private String fileName;

    /** 机场设备序列号 */
    private String dockSn;

    /** 机场名称 */
    private String dockName;

    /** 航线类型（枚举值） */
    private Integer waylineType;

    /** 任务类型（枚举值） */
    private Integer taskType;

    /** 计划执行时间 */
    private Date executeTime;

    /** 任务开始时间 */
    private Date beginTime;

    /** 任务结束时间 */
    private Date endTime;

    /** 任务完成时间 */
    private Date completedTime;

    /** 飞控用户名（中文） */
    private String usernameCn;

    /** 飞控用户 ID */
    private String userId;

    /** 返航高度（米） */
    private Integer rthAltitude;

    /** 失控动作（枚举值） */
    private Integer outOfControlAction;

    /** 遥控失联是否退出航线（枚举值） */
    private Integer exitWaylineWhenRcLostEnum;

    /** 媒体文件总数 */
    private Integer mediaCount;

    /** 已上传媒体数量 */
    private Integer uploadedCount;

    /** 父任务 ID */
    private String parentId;

    /** 是否断点续飞任务 */
    private Boolean isBreakpoint;

    /** 创建任务时指定的 AI 分析模型编号（算法中台 modelNo）；未选时分析走默认模型 */
    private String aiModelNo;

    /** Multiple AI model numbers, stored as a comma-separated string. */
    @JsonIgnore
    private String aiModelNos;

    @JsonProperty("ai_model_nos")
    public List<String> getAiModelNoList() {
        return SfUavAiModelNoSupport.splitWithFallback(aiModelNos, aiModelNo);
    }

    /** 本地记录创建时间 */
    private Date createTime;
    /** 详情携带本地影像，分页查询不加载文件列表。 */
    private java.util.List<SfUavMediaFileVo> mediaFiles;
}
