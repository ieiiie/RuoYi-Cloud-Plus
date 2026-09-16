package com.ym.agriculture.farming.satellite.model.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 遥感任务（卫星遥感页面）分页列表筛选条件。
 *
 * @author ym-cloud
 */
@Data
public class SatelliteTaskQueryBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 遥感任务类型 {@code task_type}，如 growth、soilmoisture
     */
    private String taskType;

    /**
     * 移动端关键词，匹配地块名称或遥感平台任务编号 {@code dk_id}
     */
    private String keyword;

    /**
     * 任务状态筛选（0–3）
     */
    private Integer status;

    /** 检测窗口开始日期下限，格式 yyyy-MM-dd */
    private String startDate;

    /** 检测窗口结束日期上限，格式 yyyy-MM-dd */
    private String endDate;

    /** 地块 ID，精确匹配 {@code field_id} */
    private Long fieldId;

    /** 冗余地块名称，模糊匹配 {@code field_name} */
    private String fieldName;

    /** 冗余物种名称，模糊匹配 {@code species_name} */
    private String speciesName;

    /** 冗余品种名称，模糊匹配 {@code variety_name} */
    private String varietyName;

    /** 冗余种植批次展示名，模糊匹配 {@code planting_batch_name} */
    private String plantingBatchName;
}
