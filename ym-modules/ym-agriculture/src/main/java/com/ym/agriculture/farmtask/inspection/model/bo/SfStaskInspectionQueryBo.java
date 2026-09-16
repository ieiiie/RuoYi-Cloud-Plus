package com.ym.agriculture.farmtask.inspection.model.bo;

import lombok.Data;

import java.time.LocalDate;

/** 抽检分页查询参数。 */
@Data
public class SfStaskInspectionQueryBo {

    /** 发现日期开始，包含当天。 */
    private LocalDate foundStartDate;
    /** 发现日期结束，包含当天。 */
    private LocalDate foundEndDate;
    /** 大棚 ID。 */
    private Long greenhouseId;
    /** 负责技术员员工 ID。 */
    private Long responsibleTechnicianEmployeeId;
    /** 负责组长员工 ID。 */
    private Long responsibleLeaderEmployeeId;
    /** 处理状态：UNPROCESSED/PROCESSED。 */
    private String status;
}
