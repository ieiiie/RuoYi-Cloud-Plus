package com.ym.iot.api;

import com.ym.iot.api.domain.vo.RemoteDeviceSummaryVo;
import com.ym.iot.api.domain.vo.RemoteProductVo;
import com.ym.iot.api.domain.bo.RemoteDeviceQueryBo;
import com.ym.common.core.domain.PageResult;

import java.util.Collection;
import java.util.List;

/**
 * 物联网设备只读契约，供农业业务和小程序聚合服务调用。
 */
public interface RemoteIotDeviceService {

    RemoteDeviceSummaryVo getDevice(Long deviceId);

    List<RemoteDeviceSummaryVo> listDevices(Collection<Long> deviceIds);

    /**
     * 按设备编码批量查询当前租户设备。
     *
     * @param deviceCodes 设备编码集合
     * @return 存在且未删除的设备摘要
     */
    List<RemoteDeviceSummaryVo> listDevicesByCodes(Collection<String> deviceCodes);

    /** 按当前租户和查询条件返回设备列表。 */
    List<RemoteDeviceSummaryVo> listDevices(RemoteDeviceQueryBo query);

    List<RemoteProductVo> listProductsByIds(Collection<Long> productIds);

    List<RemoteProductVo> listProductsByKeys(Collection<String> productKeys);

    PageResult<RemoteDeviceSummaryVo> pageDevices(RemoteDeviceQueryBo query);
}
