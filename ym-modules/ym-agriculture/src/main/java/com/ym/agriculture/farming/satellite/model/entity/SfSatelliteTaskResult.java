package com.ym.agriculture.farming.satellite.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 遥感回调结果，对应表 {@code sf_satellite_task_result}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_satellite_task_result")
public class SfSatelliteTaskResult extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "result_id", type = IdType.ASSIGN_ID)
    private Long resultId;

    @TableField("dk_id")
    private String dkId;

    /** 与主任务创建时快照一致，来自 {@link SfSatelliteTask#getPlantingBatchId()} */
    @TableField("planting_batch_id")
    private Long plantingBatchId;

    @TableField("task_type")
    private String taskType;

    @TableField("dk_bounds")
    private String dkBounds;

    @TableField("break_value")
    private String breakValue;

    @TableField("area")
    private String area;

    @TableField("object_key1")
    private String objectKey1;

    @TableField("object_key2")
    private String objectKey2;

    @TableField("image_pixel")
    private Integer imagePixel;

    @TableField("success")
    private Boolean success;

    @TableField("image_date")
    private String imageDate;

    @TableField("bucket")
    private String bucket;

    @TableField("oss_url")
    private String ossUrl;

    @TableLogic
    @TableField("del_flag")
    private String delFlag;
}
