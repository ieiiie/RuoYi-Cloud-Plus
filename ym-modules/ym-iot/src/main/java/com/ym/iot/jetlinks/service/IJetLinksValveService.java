package com.ym.iot.jetlinks.service;

import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.iot.motorvalve.domain.bo.ValveBatchControlItemBo;
import com.ym.iot.motorvalve.domain.bo.ValveControlBo;
import com.ym.iot.motorvalve.domain.bo.ValveControlLogBo;
import com.ym.iot.motorvalve.domain.bo.ValvePercentControlBo;
import com.ym.iot.motorvalve.domain.vo.MotorValveDeviceRowVo;
import com.ym.iot.motorvalve.domain.vo.ValveBatchControlResultVo;
import com.ym.iot.motorvalve.domain.vo.ValveControlLogVo;
import com.ym.iot.motorvalve.domain.vo.ValveControlProfileVo;
import com.ym.iot.motorvalve.domain.vo.ValveCurrentPercentVo;
import com.ym.iot.motorvalve.domain.vo.ValvePercentControlResultVo;
import com.ym.iot.motorvalve.service.IValveCommandService;

import java.util.List;

/** JetLinks 业务接口；控制器和 Dubbo 适配只依赖业务契约。 */
public interface IJetLinksValveService extends IValveCommandService {
    List<MotorValveDeviceRowVo> listValveDevices(String online);

    PageResult<MotorValveDeviceRowVo> pageValveDevices(String online, String code, PageQuery page);

    String controlValve(Long id, ValveControlBo bo);

    ValvePercentControlResultVo percentControl(Long id, ValvePercentControlBo bo);

    List<ValveBatchControlResultVo> batchControl(List<ValveBatchControlItemBo> items);

    ValveControlProfileVo getControlProfile(Long id);

    ValveCurrentPercentVo getCurrentPercent(Long id);

    String readData(Long id, List<String> lora, Integer type);

    String readGatewayStatus(Long id);

    String syncNtpTime(Long id);

    List<String> batchReadData(List<Long> ids, List<String> lora, Integer type);

    PageResult<ValveControlLogVo> queryControlLogPage(ValveControlLogBo bo, PageQuery page);
}
