package com.ym.agriculture.farmtask.inspection.model.vo;

import lombok.Data;

import java.util.List;

/** 抽检大棚分组。 */
@Data
public class SfStaskInspectionGreenhouseGroupVo {

    /** 布局合成分组 ID；未分组为 ungrouped。 */
    private String groupId;
    /** 分组名称。 */
    private String groupName;
    /** 分组顺序。 */
    private Integer groupOrder;
    /** 当前分组下的大棚。 */
    private List<SfStaskInspectionGreenhouseFieldVo> greenhouses;
}
