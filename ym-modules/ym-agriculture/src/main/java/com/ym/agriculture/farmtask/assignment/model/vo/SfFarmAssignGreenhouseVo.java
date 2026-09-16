package com.ym.agriculture.farmtask.assignment.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * stask 农事分配大棚统计列表出参。
 */
@Data
public class SfFarmAssignGreenhouseVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 大棚ID，对应 {@code sf_field.field_id}。
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
     * 已分配农事数量，按农事项去重统计。
     */
    private long assignedWorkItemCount;

    /**
     * 未分配农事数量，总启用农事项数减去已分配数量。
     */
    private long unassignedWorkItemCount;

    /**
     * 已参与组长数量，按组长人员ID去重统计。
     */
    private long leaderCount;
}
