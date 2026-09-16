package com.ym.agriculture.farmtask.workorder.model.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * stask 任务范围（大棚 + 农事项目）查询入参。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SfStaskTaskScopeBo {

    /**
     * 大棚 ID。
     */
    private Long greenhouseId;

    /**
     * 农事项目 ID。
     */
    private Long workItemId;
}
