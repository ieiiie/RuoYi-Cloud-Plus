package com.ym.iot.device.service;

import com.ym.iot.device.domain.vo.IotDeviceTagVo;

import java.util.List;

/**
 * 设备标签业务接口。
 * <p>
 * 读取设备的 Key-Value 标签；标签由 JetLinks 维护。
 * </p>
 *
 * @author ym-cloud
 */
public interface IIotDeviceTagService {

    /** 按设备ID查询其所有标签。 */
    List<IotDeviceTagVo> queryByDeviceId(Long deviceId);

}
