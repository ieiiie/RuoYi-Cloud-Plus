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
import java.time.LocalDate;

/**
 * 卫星遥感周期计划，对应表 {@code sf_satellite_schedule}。
 * <p>
 * 挂在种植批次（{@code sf_planting_batch}）上，记录检测时间范围、执行周期及任务模板参数。
 * 创建计划时会立即生成关联的遥感子任务，计划保留监测窗口和任务模板快照。
 *
 * @author ym-cloud
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_satellite_schedule")
public class SfSatelliteSchedule extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 计划主键 */
    @TableId(value = "schedule_id", type = IdType.ASSIGN_ID)
    private Long scheduleId;

    /** 关联种植批次 {@code sf_planting_batch.batch_id} */
    @TableField("planting_batch_id")
    private Long plantingBatchId;

    /** 冗余：种植批次展示名（创建时快照） */
    @TableField("planting_batch_name")
    private String plantingBatchName;

    /** 关联业务地块 {@code sf_field.field_id}，可选 */
    @TableField("field_id")
    private Long fieldId;

    /** 冗余：地块名称（创建时快照） */
    @TableField("field_name")
    private String fieldName;

    /** 冗余：物种 ID */
    @TableField("species_id")
    private Long speciesId;

    /** 冗余：物种名称 */
    @TableField("species_name")
    private String speciesName;

    /** 冗余：品种 ID */
    @TableField("variety_id")
    private Long varietyId;

    /** 冗余：品种名称 */
    @TableField("variety_name")
    private String varietyName;

    /** GeoJSON 地块边界（创建时快照，传递给子任务） */
    @TableField("dk_geom")
    private String dkGeom;

    /** 作物类型码，对应 {@code sf_crop_variety.variety_code} */
    @TableField("code_croptype")
    private String codeCroptype;

    /** 任务类型，多种类型用逗号分隔（如 growth,soilmoisture,ndvi） */
    @TableField("task_type")
    private String taskType;

    /** 遥感影像分辨率（米），默认 10 */
    @TableField("pixel_image")
    private Integer pixelImage;

    /** 检测开始日期，默认取种植批次的播种日期 */
    @TableField("detect_start")
    private LocalDate detectStart;

    /** 检测结束日期，默认取种植批次的预计收获日期 */
    @TableField("detect_end")
    private LocalDate detectEnd;

    /** 执行周期（天），7=每周，14=每两周 */
    @TableField("cycle_days")
    private Integer cycleDays;

    /** 下次执行日期，初始值 = detectStart，每次创建子任务后推进 += cycleDays */
    @TableField("next_run_date")
    private LocalDate nextRunDate;

    /**
     * 计划状态：0=未开始、1=周期中、2=已完成、3=已停用。
     * 非停用状态由后端根据 detectStart / detectEnd 与当前日期动态计算；
     * 当 {@code nextRunDate > detectEnd} 时自动标记为 2（已完成）。
     *
     * @see com.ym.agriculture.farming.satellite.model.constants.SatelliteScheduleStatus
     */
    @TableField("schedule_status")
    private Integer scheduleStatus;

    /** 逻辑删除：0 存在，1 删除 */
    @TableLogic
    @TableField("del_flag")
    private String delFlag;
}
