package com.ym.agriculture.farming.uav.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.util.Date;

/**
 * 无人机飞行任务记录，对应表 {@code sf_uav_flight_task}。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sf_uav_flight_task")
public class SfUavFlightTask extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 地块 ID。
     */
    private Long fieldId;

    /** 冗余：地块名称（创建时快照） */
    private String fieldName;

    private Long speciesId;

    private String speciesName;

    private Long varietyId;

    private String varietyName;

    /** 创建时地块进行中种植批次 ID，无则 null */
    private Long plantingBatchId;

    /** 冗余：种植批次展示名（{@code sf_planting_batch.batch_code}，创建时快照） */
    private String plantingBatchName;

    /**
     * 飞控工作空间 ID。
     */
    private String workspaceId;

    /**
     * 飞行任务 ID（飞控 jobId）。
     */
    private String jobId;

    /**
     * 飞控任务 ID（任务列表接口中的 job_id，便于与飞控任务对齐）。
     */
    private String uavJobId;

    /**
     * 飞控任务名称（任务列表接口中的 job_name）。
     */
    private String uavJobName;

    /**
     * 任务名称（本地存储时已在原始名称后拼接 jobId）。
     */
    private String taskName;

    /** 飞控任务状态 */
    private Integer status;

    /** 执行进度（0-100） */
    private Integer progress;

    /** 飞控用户名（英文） */
    private String username;

    /** 飞控业务/错误码 */
    private Integer code;

    /** 上传状态 */
    private Integer uploading;

    /** 条件/就绪信息原始 JSON 字符串 */
    private String conditions;

    /** 任务来源：1 平台，2 物联感知 等 */
    private Integer source;

    /** 航线文件 ID */
    private String fileId;

    /** 航线文件名称 */
    private String fileName;

    /** 机场 SN */
    private String dockSn;

    /** 机场名称 */
    private String dockName;

    /** 航线类型 */
    private Integer waylineType;

    /** 任务类型 */
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

    /** 失控动作 */
    private Integer outOfControlAction;

    /** 遥控失联退出航线枚举值 */
    private Integer exitWaylineWhenRcLostEnum;

    /** 媒体总数 */
    private Integer mediaCount;

    /** 已上传媒体数量 */
    private Integer uploadedCount;

    /** 父任务 ID */
    private String parentId;

    /** 是否断点任务 */
    private Boolean isBreakpoint;

    /**
     * 创建任务时指定的 AI 分析模型编号（算法中台 modelNo）；未选时由分析接口走默认模型。
     */
    private String aiModelNo;

    /**
     * Multiple AI model numbers, stored as a comma-separated string.
     */
    private String aiModelNos;

    private java.time.LocalDateTime createTime;
}
