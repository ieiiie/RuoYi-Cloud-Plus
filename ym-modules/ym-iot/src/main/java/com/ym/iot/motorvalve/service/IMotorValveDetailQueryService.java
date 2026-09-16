package com.ym.iot.motorvalve.service;

import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.iot.motorvalve.domain.bo.ValveControlLogBo;
import com.ym.iot.motorvalve.domain.vo.ValveControlLogVo;
import com.ym.iot.motorvalve.domain.vo.ValveControlProfileVo;
import com.ym.iot.motorvalve.domain.vo.ValveCurrentPercentVo;

/** 电动阀档案、实际开度与历史日志查询，不下发设备指令。 */
public interface IMotorValveDetailQueryService {
    ValveControlProfileVo getControlProfile(Long deviceId);

    ValveCurrentPercentVo getCurrentPercent(Long deviceId);

    PageResult<ValveControlLogVo> queryControlLogPage(ValveControlLogBo bo, PageQuery query);
}
