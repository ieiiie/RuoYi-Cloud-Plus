package com.ym.agriculture.farming.uav.service;

import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farming.uav.model.bo.SfUavAiTaskQueryBo;
import com.ym.agriculture.farming.uav.model.bo.SfUavFlightTaskQueryBo;
import com.ym.agriculture.farming.uav.model.vo.SfUavAiTaskVo;
import com.ym.agriculture.farming.uav.model.vo.SfUavFlightTaskVo;

/**
 * 本地库 {@code sf_uav_flight_task}、{@code sf_uav_ai_task} 只读查询（分页与详情），权限与地块归属一致。
 */
public interface IUavLocalTaskQueryService {

    PageResult<SfUavFlightTaskVo> pageFlightTasks(SfUavFlightTaskQueryBo bo, PageQuery pageQuery);

    SfUavFlightTaskVo getFlightTask(Long id);

    PageResult<SfUavAiTaskVo> pageAiTasks(SfUavAiTaskQueryBo bo, PageQuery pageQuery);

    SfUavAiTaskVo getAiTask(Long id);

    /**
     * 按 UAV 任务 ID（{@code sf_uav_ai_task.uav_job_id}）查 AI 分析任务详情；同 job 多条时取 {@code create_time} 最新，再按 {@code id} 降序。
     */
    SfUavAiTaskVo getAiTaskByUavJobId(String uavJobId);

}
