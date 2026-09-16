package com.ym.agriculture.farming.satellite.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 遥感任务分页列表单行。
 *
 * @author ym-cloud
 */
@Data
public class SatelliteTaskListVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 遥感侧地块标识 {@code dk_id}，由任务创建时生成，用于与外部遥感服务对接。
     */
    private String dkId;

    /**
     * 业务地块主键 ID（平台内部地块标识）。
     */
    private Long fieldId;

    /**
     * 创建任务时地块上进行中种植批次 ID；无进行中批次时为 null。
     */
    private Long plantingBatchId;

    /** 冗余：种植批次展示名（{@code sf_planting_batch.batch_code} 快照） */
    private String plantingBatchName;

    /** 冗余：地块名称（创建时快照） */
    private String fieldName;

    private Long speciesId;

    private String speciesName;

    private Long varietyId;

    private String varietyName;

    /**
     * 作物类型编码（作物字典）
     */
    private String codeCroptype;

    /**
     * 任务监测起始日期，格式为 {@code yyyy-MM-dd}。
     */
    private String startDate;

    /**
     * 任务监测结束日期，格式为 {@code yyyy-MM-dd}。
     */
    private String endDate;

    /**
     * 遥感任务类型 {@code task_type}，如 {@code growth}、{@code soilmoisture} 等。
     * 对应系统字典 {@code sat_task_type}。
     */
    private String taskType;

    /**
     * 分析类型中文描述
     */
    private String taskTypeDesc;

    /**
     * 任务状态编码，结合 {@code statusDesc}（见 {@link com.ym.agriculture.farming.satellite.model.constants.SatelliteTaskStatus}：0 待提交，1 处理中，2 成功，3 失败）。
     */
    private Integer status;

    /**
     * 任务状态中文描述，便于列表直接展示（如“待提交”、“处理成功”、“处理失败”）。
     */
    private String statusDesc;

    /**
     * 状态附加信息或错误原因描述，例如回调失败时的具体提示。
     */
    private String message;

    /**
     * 向外部遥感服务提交任务的次数（重试计数），用于排查重复提交等问题。
     */
    private Integer submitCount;

    /**
     * 处理成功次数，来自该任务 {@code dkId} 关联的遥感结果表成功记录数。
     */
    private Integer resultSuccessCount;

    /**
     * 处理失败次数，来自该任务 {@code dkId} 关联的遥感结果表失败记录数。
     */
    private Integer resultFailCount;

    /**
     * 最近一次向遥感服务提交任务的时间。
     */
    private Date lastSubmitTime;

    /**
     * 任务在本系统中的创建时间。
     */
    private Date createTime;

    /**
     * 任务在本系统中的最后更新时间（包括状态变更、回调更新等）。
     */
    private Date updateTime;
}
