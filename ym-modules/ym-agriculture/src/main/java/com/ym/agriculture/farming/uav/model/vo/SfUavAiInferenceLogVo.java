package com.ym.agriculture.farming.uav.model.vo;

import com.ym.agriculture.farming.algback.model.entity.SfAiInferenceLog;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 算法中台推理回调流水视图，对应 {@code sf_ai_inference_log}；
 * 供 UAV AI 任务详情接口（如按主键、按 {@code uav_job_id}）在 {@code inferenceLogs} 中一并返回。
 *
 * @author ym-cloud
 */
@Data
@AutoMapper(target = SfAiInferenceLog.class)
public class SfUavAiInferenceLogVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键，对应 {@code sf_ai_inference_log.id} */
    private Long id;

    /** 算法中台任务编号，与 {@code sf_uav_ai_task.alg_task_no} 关联 */
    private String taskNo;

    /** 算法中台侧任务名称（若有） */
    private String taskName;

    /** 模型编号 */
    private String modelNo;

    /** 模型名称（若有） */
    private String modelName;

    /** 客户/业务方编号（中台回传，若有） */
    private String customerNo;

    /** 客户/业务方名称（若有） */
    private String customerName;

    /** 算法类型字典主键（若有） */
    private Long algorithmTypeId;

    /** 算法类型字典展示值 */
    private String algorithmTypeValue;

    /**
     * 中台推送的分类得分原始串（例如 "{'Car': 0.78}"），未再解析，便于排查。
     */
    private String clsScore;

    /**
     * 解析后的分类标签（如「行人」「车辆」），一般由 {@link #clsScore} 结合 {@link #modelNo} 对应字典得到。
     */
    private String clsScoreLabel;

    /**
     * 解析后的置信度或分数字符串（如 {@code "0.78"}），与 {@link #clsScoreLabel} 对应。
     */
    private String clsScoreValue;

    /** 关联检测/告警图片 URL */
    private String imgUrl;

    /** 关联的 UAV 原始媒体主键 */
    private Long uavMediaId;

    /** 原始图片 file_id */
    private String sourceFileId;

    /** 原始图片文件名 */
    private String sourceFileName;

    /** 原始图片对象键或原始 URL */
    private String sourceObjectKey;

    /** 告警或事件发生时间，毫秒时间戳，与库表 {@code alarm_time} 一致 */
    private Long alarmTime;

    /** 本行记录写入时间 */
    private Date createTime;

    /** 拍摄纬度，关联 {@code sf_uav_media_file.lat} */
    private String lat;

    /** 拍摄经度，关联 {@code sf_uav_media_file.lng} */
    private String lng;

    /** 拍摄时间，关联 {@code sf_uav_media_file.file_create_time} */
    private Date shootTime;

    /** 拍摄纬度，直接来自 {@code sf_ai_inference_log.shoot_lat} */
    private String shootLat;

    /** 拍摄经度，直接来自 {@code sf_ai_inference_log.shoot_lng} */
    private String shootLng;
}
