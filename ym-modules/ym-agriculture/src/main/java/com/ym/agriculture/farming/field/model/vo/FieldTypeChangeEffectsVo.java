package com.ym.agriculture.farming.field.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 地块类型从大田变更为大棚时的影响范围。
 *
 * @author ym-cloud
 */
@Data
public class FieldTypeChangeEffectsVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 未完成无人机飞行任务数量 */
    private Long pendingUavTaskCount;

    /** 未执行无人机飞行计划数量 */
    private Long pendingUavPlanCount;

    /** 未完成卫星遥感计划数量 */
    private Long activeSatelliteScheduleCount;

    /** 总影响数量 */
    private Long totalAffectedCount;

    /** 是否存在影响项 */
    private Boolean affected;
}
