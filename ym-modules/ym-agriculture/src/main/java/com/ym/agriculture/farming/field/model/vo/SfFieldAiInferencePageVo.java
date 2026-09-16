package com.ym.agriculture.farming.field.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 地块推理明细分页单行。
 *
 * @author ym-cloud
 */
@Data
public class SfFieldAiInferencePageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 推理流水主键
     */
    private Long id;

    /**
     * 告警/检测时间（毫秒时间戳，与库表一致）
     */
    private Long alarmTime;

    /**
     * 记录创建时间
     */
    private Date createTime;

    /**
     * 分类标签（解析后的展示文案）
     */
    private String clsScoreLabel;

    /**
     * 置信度数值（可解析时）
     */
    private String clsScoreValue;

    /**
     * 原始置信度/分数字符串（未解析或原文）
     */
    private String clsScoreRaw;

    /**
     * 关联图片 URL
     */
    private String imgUrl;

    /** 关联的 UAV 原始媒体主键 */
    private Long uavMediaId;

    /** 原始图片 file_id */
    private String sourceFileId;

    /** 原始图片文件名 */
    private String sourceFileName;

    /** 原始图片对象键或原始 URL */
    private String sourceObjectKey;

    /** 原始图片拍摄纬度 */
    private String shootLat;

    /** 原始图片拍摄经度 */
    private String shootLng;

    /** 原始图片拍摄时间 */
    private Date shootTime;

    /**
     * 算法中台任务号
     */
    private String taskNo;

    /**
     * 飞控 UAV 任务 ID（jobId）
     */
    private String uavJobId;

    /**
     * 模型编号
     */
    private String modelNo;

    /**
     * 模型名称（若有）
     */
    private String modelName;

    /**
     * 任务名称（若有）
     */
    private String taskName;

    /**
     * 算法类型字典值
     */
    private String algorithmTypeValue;
}
