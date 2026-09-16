package com.ym.agriculture.farmtask.inspection.model.bo;

import lombok.Data;

/** 抽检大棚分组查询参数。 */
@Data
public class SfStaskInspectionGreenhouseGroupQueryBo {

    /** 跨分组匹配的大棚名称或编码关键字。 */
    private String keyword;
}
