package com.ym.agriculture.farmtask.workorder.model.vo;

import lombok.Data;

/**
 * stask 任务范围冲突视图（计划日期 + 大棚 + 农事项目）。
 */
@Data
public class SfStaskTaskScopeConflictVo {

    /**
     * 大棚 ID。
     */
    private Long greenhouseId;

    /**
     * 大棚名称快照。
     */
    private String greenhouseNameSnapshot;

    /**
     * 农事项目 ID。
     */
    private Long workItemId;

    /**
     * 农事项目名称快照。
     */
    private String workItemNameSnapshot;
}
