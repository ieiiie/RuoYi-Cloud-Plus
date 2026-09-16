package com.ym.agriculture.shared.media.model.entity;

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
 * 媒体资产，对应 {@code sf_media_asset}。无人机任务媒体见 {@code sf_uav_media_file}。
 *
 * @author ym-cloud
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sf_media_asset")
public class SfMediaAsset extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "media_id", type = IdType.ASSIGN_ID)
    private Long mediaId;

    private Long deviceId;
    private Long fieldId;
    private Long batchId;

    /** VIDEO / IMAGE */
    private String mediaType;

    /** CAMERA / UAV / OTHER */
    private String sourceType;

    /** 飞控任务 ID，与 {@code uav_file_id} 组成幂等键 */
    private String uavJobId;

    /** 飞控媒体 file_id */
    private String uavFileId;

    private Long ossId;
    private String fileUrl;
    private Long coverOssId;
    private Date startTime;
    private Date endTime;
    private Date retainUntil;
    private String metaJson;

    @TableLogic
    private String delFlag;

    private Long createDept;
    private Long createBy;
    private java.time.LocalDateTime createTime;
    private Long updateBy;
    private java.time.LocalDateTime updateTime;
    private String remark;
}
