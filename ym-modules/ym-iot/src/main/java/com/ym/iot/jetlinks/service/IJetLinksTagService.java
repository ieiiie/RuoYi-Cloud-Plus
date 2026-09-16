package com.ym.iot.jetlinks.service;

import com.ym.iot.device.domain.vo.IotDeviceTagVo;
import com.ym.iot.device.service.IIotDeviceTagService;
import com.ym.jetlinks.rpc.RecordDto;

import java.util.List;

/** JetLinks 业务接口；控制器和 Dubbo 适配只依赖业务契约。 */
public interface IJetLinksTagService extends IIotDeviceTagService {
    List<RecordDto> records(Long id);

    List<IotDeviceTagVo> queryByDeviceId(Long id);
}
