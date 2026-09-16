package com.ym.agriculture.farming.dubbo;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.ym.agriculture.api.farming.RemoteFieldDeviceBindingService;
import com.ym.agriculture.farming.field.dao.SfFieldIotMapper;
import com.ym.common.core.exception.ServiceException;

import lombok.RequiredArgsConstructor;

import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

/** 农业服务查询自己的地块绑定表；IoT 不依赖农业数据库地址或库名。 */
@Service
@DS("master")
@DubboService(group = "iot-ownership-guard", version = "1.0.0", retries = 0, token = "true")
@RequiredArgsConstructor
public class RemoteFieldDeviceBindingServiceImpl implements RemoteFieldDeviceBindingService {
    private final SfFieldIotMapper fieldIotMapper;

    @Override
    public long countActiveBindings(String deviceSn) {
        if (deviceSn == null || deviceSn.isBlank() || deviceSn.length() > 128) {
            throw new ServiceException("设备编号缺失或格式无效，无法核实地块绑定");
        }
        return fieldIotMapper.countAllActiveBindings(deviceSn);
    }
}
