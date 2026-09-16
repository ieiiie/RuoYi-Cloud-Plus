package com.ym.iot.hfzk.service;

import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.iot.hfzk.domain.bo.IotHfzkUserDeviceBo;
import com.ym.iot.hfzk.domain.vo.IotHfzkUserDeviceVo;

import java.util.List;

/**
 * 中科合肥同步用户设备（本地表 {@code iot_hfzk_user_device}）查询。
 */
public interface IIotHfzkUserDeviceService {

    /**
     * 当前租户下设备列表（不分页）。
     */
    List<IotHfzkUserDeviceVo> queryList(IotHfzkUserDeviceBo bo);

    /**
     * 当前租户下设备分页列表。
     */
    PageResult<IotHfzkUserDeviceVo> queryPageList(IotHfzkUserDeviceBo bo, PageQuery pageQuery);
}
