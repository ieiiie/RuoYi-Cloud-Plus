package com.ym.agriculture.farmtask.workorder.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 技术员工作台列表视图（按 tab 返回单一 items 列表）。
 */
@Data
public class SfStaskTechnicianWorkbenchTasksVo {

    /**
     * 当前 Tab：pending-待处理，processing-进行中，completed_today-已完成(今日)。
     */
    private String tab;

    /**
     * 当前 Tab 下的任务列表。
     */
    private List<SfStaskTechnicianWorkbenchItemVo> items;
}
