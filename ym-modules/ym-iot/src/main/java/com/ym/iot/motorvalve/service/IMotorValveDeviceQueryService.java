package com.ym.iot.motorvalve.service;

import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.iot.motorvalve.domain.vo.MotorValveDeviceRowVo;

import java.util.List;

/** 电动阀设备列表与实时状态查询，不直接连接厂家服务。 */
public interface IMotorValveDeviceQueryService {
    List<MotorValveDeviceRowVo> list(String onlineStatus);

    PageResult<MotorValveDeviceRowVo> page(
            String onlineStatus, String deviceCode, PageQuery pageQuery);
}
