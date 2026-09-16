package com.ym.agriculture.farmtask.clocklocation.service;

import com.ym.agriculture.farmtask.clocklocation.model.bo.SfStaskClockLocationBo;
import com.ym.agriculture.farmtask.clocklocation.model.vo.SfStaskClockLocationVo;

/**
 * 打卡地点服务。
 */
public interface ISfStaskClockLocationService {

    SfStaskClockLocationVo getCurrent();

    SfStaskClockLocationVo getByTenantId(String tenantId);

    void save(SfStaskClockLocationBo bo);
}
