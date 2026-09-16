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
import java.util.Date;

/**
 * 遥感任务，对应表 {@code sf_satellite_task}（逻辑自 ym-gis {@code satellite_task} 迁入）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_satellite_task")
public class SfSatelliteTask extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "task_id", type = IdType.ASSIGN_ID)
    private Long taskId;

    /** 关联周期计划 {@code sf_satellite_schedule.schedule_id}，单次任务为 null */
    @TableField("schedule_id")
    private Long scheduleId;

    /** 可选：业务地块 {@code sf_field.field_id} */
    @TableField("field_id")
    private Long fieldId;

    /** 创建任务时地块上进行中种植批次 {@code sf_planting_batch.batch_id}，无则 null */
    @TableField("planting_batch_id")
    private Long plantingBatchId;

    /** 冗余：种植批次展示名（{@code sf_planting_batch.batch_code}，创建时快照） */
    @TableField("planting_batch_name")
    private String plantingBatchName;

    /** 冗余：地块名称（创建时快照） */
    @TableField("field_name")
    private String fieldName;

    @TableField("species_id")
    private Long speciesId;

    @TableField("species_name")
    private String speciesName;

    @TableField("variety_id")
    private Long varietyId;

    @TableField("variety_name")
    private String varietyName;

    /** 对外遥感地块唯一标识（雪花字符串） */
    @TableField("dk_id")
    private String dkId;

    @TableField("dk_geom")
    private String dkGeom;

    /** 数值型作物类型码（codeCroptype），由前端/业务按约定映射 */
    @TableField("code_croptype")
    private String codeCroptype;

    @TableField("start_date")
    private String startDate;

    @TableField("end_date")
    private String endDate;

    @TableField("task_type")
    private String taskType;

    @TableField("pixel_image")
    private Integer pixelImage;

    /**
     * 任务状态，见 {@link com.ym.agriculture.farming.satellite.model.constants.SatelliteTaskStatus}：0 待提交，1 处理中，2 成功，3 失败。
     */
    @TableField("status")
    private Integer status;

    @TableField("message")
    private String message;

    @TableField("submit_count")
    private Integer submitCount;

    @TableField("last_submit_time")
    private Date lastSubmitTime;

    @TableLogic
    @TableField("del_flag")
    private String delFlag;
}
