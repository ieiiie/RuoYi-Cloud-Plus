package com.ym.agriculture.farmtask.inspection.model.vo;

import lombok.Data;

/** 抽检红点统计。 */
@Data
public class SfStaskInspectionBadgeVo {

    /** 当前员工负责技术员关系下的未处理数量。 */
    private Long technicianUnprocessedCount;
    /** 当前员工负责组长关系下的未处理数量。 */
    private Long leaderUnprocessedCount;
    /** 当前员工是否可以创建抽检。 */
    private Boolean canCreate;
}
