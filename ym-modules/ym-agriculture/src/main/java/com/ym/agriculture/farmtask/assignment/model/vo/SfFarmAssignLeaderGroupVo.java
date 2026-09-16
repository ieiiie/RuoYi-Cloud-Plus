package com.ym.agriculture.farmtask.assignment.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * stask 农事分配已参与组长分组出参。
 */
@Data
public class SfFarmAssignLeaderGroupVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 组长人员ID。
     */
    private Long leaderId;

    /**
     * 组长姓名；人员已删除时返回兜底文本。
     */
    private String leaderName;

    /**
     * 组长手机号；历史人员缺失时为空。
     */
    private String leaderPhone;

    /**
     * 该组长已分配农事项列表。
     */
    private List<SfFarmAssignWorkItemVo> workItems;
}
