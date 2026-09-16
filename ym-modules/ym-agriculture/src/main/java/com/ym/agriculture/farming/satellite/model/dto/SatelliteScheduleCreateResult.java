package com.ym.agriculture.farming.satellite.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 遥感监测计划及其首次创建子任务的结果。
 *
 * @author ym-cloud
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SatelliteScheduleCreateResult {

    /** 周期计划主键 {@code sf_satellite_schedule.schedule_id} */
    private Long scheduleId;

    /** 与该计划关联的遥感子任务编号列表 {@code sf_satellite_task.dk_id} */
    private List<String> dkIds;
}
