package com.ym.agriculture.farmtask.inspection.model.bo;

import lombok.Data;

/** 抽检关联工单候选查询参数。 */
@Data
public class SfStaskInspectionOrderQueryBo {

    /** 当前已选择的大棚 ID。 */
    private Long greenhouseId;
    /** 按工单编号或农事项名称匹配的关键字。 */
    private String keyword;
}
