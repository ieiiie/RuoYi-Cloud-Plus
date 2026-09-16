package com.ym.agriculture.farming.uav.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
 * 飞控 {@code getFileByJobId} 媒体明细，对应 {@code sf_uav_media_file}。
 *
 * @author ym-cloud
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sf_uav_media_file")
public class SfUavMediaFile extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "uav_media_id", type = IdType.ASSIGN_ID)
    private Long uavMediaId;

    private String workspaceId;
    /** 本次 API 拉取使用的任务 ID */
    private String syncJobId;
    private String mediaGroupKey;
    private String fileId;
    private String fileName;
    private String filePath;
    private String objectKey;
    @TableField("is_original")
    private Boolean original;
    private String drone;
    private String payload;
    private String tinnyFingerprint;
    private String fingerprint;
    private Date fileCreateTime;
    /** 媒体 JSON 内 job_id */
    @TableField("payload_job_id")
    private String payloadJobId;
    private String absoluteAltitude;
    private String relativeAltitude;
    private String lat;
    private String lng;
    private String jobName;
    private String flyLineId;
    private String flyLineName;
    private String fileType;
    private Long fieldId;
    private Long batchId;

    @TableLogic
    private String delFlag;

    private Long createDept;
    private Long createBy;
    private java.time.LocalDateTime createTime;
    private Long updateBy;
    private java.time.LocalDateTime updateTime;
    private String remark;
}
