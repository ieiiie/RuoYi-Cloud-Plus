package com.ym.agriculture.api.farmtask;

import com.ym.agriculture.api.farmtask.domain.vo.RemoteClockLocationVo;

import com.ym.agriculture.api.farmtask.domain.bo.RemoteTaskAcceptanceBo;
import com.ym.agriculture.api.farmtask.domain.bo.RemoteTaskClockInBo;
import com.ym.agriculture.api.farmtask.domain.bo.RemoteTaskCompleteBo;
import com.ym.agriculture.api.farmtask.domain.bo.RemoteTaskLeaderAcceptBo;
import com.ym.agriculture.api.farmtask.domain.vo.RemoteTaskViewVo;

/**
 * 农事任务小程序跨服务契约。
 *
 * <p>契约只暴露可序列化模型；租户、登录人与数据权限由 Dubbo 上下文传播，
 * Provider 仍按当前登录人重新校验角色、工单归属及状态机。</p>
 */
public interface RemoteFarmTaskService {

    /** 查询当前租户的农事任务打卡地点及围栏设置。 */
    RemoteClockLocationVo getClockLocation();

    RemoteTaskViewVo workbench(String roleCode);

    RemoteTaskViewVo workbenchSummary(String roleCode);

    RemoteTaskViewVo workbenchTasks(String roleCode, String tab);

    RemoteTaskViewVo detail(Long orderId);

    int leaderAccept(Long orderId, RemoteTaskLeaderAcceptBo command);

    int clockIn(Long orderId, RemoteTaskClockInBo command);

    int complete(Long orderId, RemoteTaskCompleteBo command);

    int reapplyAcceptance(Long orderId, RemoteTaskCompleteBo command);

    int acceptance(Long orderId, RemoteTaskAcceptanceBo command);
}
