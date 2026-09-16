package com.ym.agriculture.farming.satellite.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 创建周期遥感计划入参（平台 API，JSON 使用 camelCase）。
 * <p>
 * 前端在种植批次详情页点击"发起遥感监测"时提交此对象，
 * 系统据此创建 {@code sf_satellite_schedule} 记录，并立即创建关联遥感子任务。
 *
 * @author ym-cloud
 */
@Data
public class SatelliteScheduleCreateDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 关联种植批次 ID */
    @NotNull(message = "plantingBatchId不能为空")
    private Long plantingBatchId;

    /** 可选：关联业务地块 ID */
    private Long fieldId;

    /** GeoJSON 格式地块边界 */
    @NotNull(message = "dkGeom不能为空")
    private Object dkGeom;

    /** 任务类型列表：growth（长势）、soilmoisture（墒情）等，支持多选 */
    @NotEmpty(message = "taskTypes不能为空")
    private List<String> taskTypes;

    /** 遥感影像分辨率（米），默认 10 */
    @Min(value = 2, message = "pixelImage必须不小于2")
    private Integer pixelImage = 10;

    /** 检测开始日期，默认取种植批次播种日期 */
    @NotNull(message = "detectStart不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private Date detectStart;

    /** 检测结束日期，默认取种植批次预计收获日期 */
    @NotNull(message = "detectEnd不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private Date detectEnd;

}
