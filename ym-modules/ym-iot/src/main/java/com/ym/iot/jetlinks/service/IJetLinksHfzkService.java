package com.ym.iot.jetlinks.service;

import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.iot.device.domain.vo.IotDeviceVo;
import com.ym.iot.hfzk.domain.bo.IotHfzkUserDeviceBo;
import com.ym.iot.hfzk.domain.vo.IotHfzkUserDeviceVo;
import com.ym.iot.hfzk.service.IHfzkDeviceTypeQueryService;
import com.ym.iot.hfzk.service.IIotHfzkUserDeviceService;
import com.ym.jetlinks.rpc.RecordDto;

import java.util.List;
import java.util.Map;

/** JetLinks 业务接口；控制器和 Dubbo 适配只依赖业务契约。 */
public interface IJetLinksHfzkService
        extends IIotHfzkUserDeviceService, IHfzkDeviceTypeQueryService {
    List<RecordDto> records(Map<String, Object> filters);

    List<IotHfzkUserDeviceVo> queryList(IotHfzkUserDeviceBo bo);

    PageResult<IotHfzkUserDeviceVo> queryPageList(IotHfzkUserDeviceBo bo, PageQuery page);

    List<IotDeviceVo> listByDeviceType(String type, String online);

    PageResult<IotDeviceVo> pageByDeviceType(
            String type, String online, String code, PageQuery page);
}
