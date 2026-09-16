package com.ym.iot.motorvalve.service;

import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.iot.motorvalve.domain.bo.ValveSessionBo;
import com.ym.iot.motorvalve.domain.vo.ValveSessionStatsVo;
import com.ym.iot.motorvalve.domain.vo.ValveSessionVo;

import java.util.List;

/** 电动阀开阀会话业务接口。 */
public interface IValveSessionService {

    /** 分页查询开阀会话。 */
    PageResult<ValveSessionVo> querySessionPage(ValveSessionBo bo, PageQuery pageQuery);

    /** 汇总统计（时间范围内累计时长等）。 */
    ValveSessionStatsVo queryStats(ValveSessionBo bo);

    /**
     * 查询当前仍开启中的会话列表。
     *
     * @param deviceId 设备主键，可为空表示查全部
     */
    List<ValveSessionVo> listOpenSessions(Long deviceId);
}
