package com.ym.agriculture.farmtask.workorder.model.vo;

import com.ym.agriculture.farmtask.worker.model.vo.SfStaskWorkerSkillVo;
import lombok.Data;

import java.util.List;

/**
 * 工人「我的」页统计视图。
 */
@Data
public class SfStaskWorkerProfileVo {

    /**
     * 待接收邀请数量（按工单去重，对应 UI 状态 PENDING_CONFIRM）。
     */
    private Long pendingInviteCount;

    /**
     * 已接受且未完成验收的任务数量（按工单去重，对应 UI 状态 ACCEPTED）。
     */
    private Long acceptedCount;

    /**
     * 验收通过任务总数（按工单去重，对应 UI 状态 COMPLETED）。
     */
    private Long completedCount;

    /**
     * 农事技能列表；{@code workCount} 为对应农事项目下验收通过次数。
     */
    private List<SfStaskWorkerSkillVo> skills;
}
