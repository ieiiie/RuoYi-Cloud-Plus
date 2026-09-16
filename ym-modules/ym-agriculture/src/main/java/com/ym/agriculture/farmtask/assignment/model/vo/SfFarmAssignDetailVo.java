package com.ym.agriculture.farmtask.assignment.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * stask 农事分配详情出参。
 */
@Data
public class SfFarmAssignDetailVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 大棚ID。
     */
    private Long greenhouseId;

    /**
     * 大棚编号。
     */
    private String greenhouseCode;

    /**
     * 大棚名称。
     */
    private String greenhouseName;

    /**
     * 启用农事项总数。
     */
    private long totalWorkItemCount;

    /**
     * 已分配农事数量。
     */
    private long assignedWorkItemCount;

    /**
     * 未分配农事数量。
     */
    private long unassignedWorkItemCount;

    /**
     * 已分配农事，按组长分组。
     */
    private List<SfFarmAssignLeaderGroupVo> assignedGroups;

    /**
     * 待分配农事项列表。
     */
    private List<SfFarmAssignWorkItemVo> unassignedWorkItems;
}
