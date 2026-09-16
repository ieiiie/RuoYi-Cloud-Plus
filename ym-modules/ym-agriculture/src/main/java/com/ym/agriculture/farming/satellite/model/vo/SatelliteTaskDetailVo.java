package com.ym.agriculture.farming.satellite.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 遥感任务详情 VO。
 * <p>
 * 用于任务详情页展示任务的基础信息和处理状态；{@code results} 为回调结果落库摘要，
 * 一期前端不展示，仅任务级字段用于界面，有需要时可扩展展示。
 *
 * @author ym-cloud
 */
@Data
public class SatelliteTaskDetailVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 遥感侧地块标识 {@code dk_id}，由任务创建接口生成，用于与外部遥感服务关联。
     */
    private String dkId;

    /**
     * 业务地块主键 ID（平台内部地块标识），便于跳转到地块详情。
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
     * 任务状态编码，结合 {@code statusDesc}（见 {@link com.ym.agriculture.farming.satellite.model.constants.SatelliteTaskStatus}：0 待提交，1 处理中，2 成功，3 失败）。
     */
    private Integer status;

    /**
     * 任务状态中文描述，直接用于详情页展示（如“待提交”、“处理成功”、“处理失败”）。
     */
    private String statusDesc;

    /**
     * 状态附加信息或错误原因描述，例如回调失败时的错误提示。
     */
    private String message;

    /**
     * 向外部遥感服务提交任务的次数（包括重试），可用于分析任务重试情况。
     */
    private Integer submitCount;

    /**
     * 最近一次向遥感服务提交任务的时间。
     */
    private Date lastSubmitTime;

    /**
     * 任务在本系统中的创建时间。
     */
    private Date createTime;

    /**
     * 任务在本系统中的最后更新时间（包括提交、回调、状态更新等）。
     */
    private Date updateTime;

    /**
     * 遥感任务类型 {@code task_type}，与创建入参一致
     */
    private String taskType;

    /**
     * 回调结果摘要列表，对应遥感服务返回并落库的结果记录。
     * 一期仅用于后台排查，不在前端页面展示。
     */
    private List<TaskResultVo> results;

    @Data
    public static class TaskResultVo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 影像拍摄时间，对应回调结果中的 {@code image_date}。
         */
        private String imageDate;

        /**
         * 影像对外访问地址，直接来自 {@code sf_satellite_task_result.oss_url}。
         */
        private String ossUrl;

        /**
         * 差异影像数据在 OSS 中的对象键，对应回调结果中的 {@code object_key1}。
         */
        private String objectKey1;

        /**
         * 备用影像或其他数据对象键（视上游约定而定，对应 {@code object_key2}）。
         */
        private String objectKey2;

        /**
         * 各等级面积 JSON 字符串，对应回调结果中的 {@code area} 字段，
         * 实际为 5 个等级面积数组，含义与 {@code task_type} 对应等级字典一致。
         */
        private String area;

        /**
         * 本次结果处理是否成功，通常由回调的 {@code success} 字段映射而来。
         */
        private Boolean success;

        /**
         * 本条回调结果对应的任务类型
         */
        private String taskType;

        /**
         * 与主任务创建时快照一致，来自回调落库
         */
        private Long plantingBatchId;
    }
}
