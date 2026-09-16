package com.ym.agriculture.farming.algback.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 算法中台推理回调流水，对应 {@code sf_ai_inference_log}。
 */
@Data
@NoArgsConstructor
@TableName("sf_ai_inference_log")
public class SfAiInferenceLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String tenantId;

    private String taskNo;
    private String taskName;
    private String modelNo;
    private String modelName;
    private String customerNo;
    private String customerName;
    private Long algorithmTypeId;
    private String algorithmTypeValue;
    /**
     * 中台推送的分类得分原始值（例如 "{'Car': 0.78}"），不做解析直接存储，便于后续排查。
     */
    private String clsScore;
    /**
     * 解析后的分类标签（例如「行人」「车辆」等），由 {@code clsScore} 结合 {@code modelNo} 对应的数据字典解析而来。
     */
    private String clsScoreLabel;
    /**
     * 解析后的分类数值（字符串形式，例如 "0.78"），与 {@code clsScoreLabel} 对应。
     */
    private String clsScoreValue;
    private String imgUrl;
    /** 关联的 UAV 原始媒体主键，来自 sf_uav_media_file.uav_media_id。 */
    private Long uavMediaId;
    /** 原始图片 file_id。 */
    private String sourceFileId;
    /** 原始图片文件名。 */
    private String sourceFileName;
    /** 原始图片对象键或原始 URL。 */
    private String sourceObjectKey;
    /** 原始图片拍摄纬度。 */
    private String shootLat;
    /** 原始图片拍摄经度。 */
    private String shootLng;
    /** 原始图片拍摄时间。 */
    private Date shootTime;
    private Long alarmTime;
    private String videoPlayUrl;
    private String streamServerUrl;
    private String computingVideoPlayUrl;
    private String pushVideoPlayUrl;
    private String rawJson;
    private java.time.LocalDateTime createTime;
}
