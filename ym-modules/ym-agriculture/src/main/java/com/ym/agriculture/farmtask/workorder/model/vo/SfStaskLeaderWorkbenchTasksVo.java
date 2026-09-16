package com.ym.agriculture.farmtask.workorder.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 组长工作台列表视图（按 tab 返回单一 items 列表）。
 */
@Data
public class SfStaskLeaderWorkbenchTasksVo {

    /**
     * 当前 Tab：pending_accept-待接单，in_progress-进行中，completed_today-今日已完成。
     */
    private String tab;

    /**
     * 当前 Tab 下的拆分工单列表。
     */
    private List<SfStaskLeaderWorkbenchItemVo> items;
}
