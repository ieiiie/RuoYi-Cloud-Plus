package com.ym.agriculture.farming.satellite.model.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 遥感周期计划分页列表筛选条件。
 *
 * @author ym-cloud
 */
@Data
public class SatelliteScheduleQueryBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 物种名称，模糊匹配 */
    private String speciesName;

    /** 地块 ID，精确匹配 */
    private Long fieldId;

    /** 分析类型（单选），模糊匹配逗号分隔的 task_type 字段 */
    private String taskType;

    /** 计划状态筛选 计划状态：0=未开始、1=周期中、2=已完成、3=已停用。 */
    private Integer scheduleStatus;
}
