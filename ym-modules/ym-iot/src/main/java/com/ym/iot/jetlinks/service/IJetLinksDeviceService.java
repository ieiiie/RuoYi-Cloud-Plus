package com.ym.iot.jetlinks.service;

import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.iot.device.domain.bo.IotDeviceBo;
import com.ym.iot.device.domain.vo.IotDeviceExportVo;
import com.ym.iot.device.domain.vo.IotDeviceVo;
import com.ym.iot.device.service.IIotDeviceService;
import com.ym.jetlinks.rpc.QueryDto;
import com.ym.jetlinks.rpc.RecordDto;

import java.util.Collection;
import java.util.List;

/** JetLinks 业务接口；控制器和 Dubbo 适配只依赖业务契约。 */
public interface IJetLinksDeviceService extends IIotDeviceService {
    QueryDto query(IotDeviceBo bo, PageQuery page);

    List<RecordDto> records(IotDeviceBo bo);

    List<IotDeviceVo> queryList(IotDeviceBo bo);

    PageResult<IotDeviceVo> queryPageList(IotDeviceBo bo, PageQuery page);

    List<IotDeviceVo> byIds(Collection<Long> ids);

    RecordDto get(Long id);

    IotDeviceVo queryById(Long id);

    List<IotDeviceExportVo> queryExportList(IotDeviceBo bo);

    List<RecordDto> byCodes(Collection<String> codes);

    Long requireCode(String code);
}
