package com.ym.iot.motorvalve.service.impl;

import com.ym.common.core.domain.PageResult;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.iot.device.domain.IotDevice;
import com.ym.iot.device.domain.vo.IotLatestVo;
import com.ym.iot.device.service.IIotDataPointService;
import com.ym.iot.motorvalve.domain.bo.ValveControlLogBo;
import com.ym.iot.motorvalve.domain.vo.ValveControlLogVo;
import com.ym.iot.motorvalve.domain.vo.ValveControlProfileVo;
import com.ym.iot.motorvalve.domain.vo.ValveCurrentPercentVo;
import com.ym.iot.motorvalve.enums.ValveType;
import com.ym.iot.motorvalve.service.IMotorValveDetailQueryService;
import com.ym.iot.motorvalve.support.MotorvalveValveTypeResolver;
import com.ym.iot.motorvalve.support.ValvePercentControlCalculator;
import com.ym.iot.product.domain.IotProduct;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 电动阀档案、历史遥测与日志只读查询，不依赖 MQTT 控制服务。 */
@Service
@RequiredArgsConstructor
public class MotorValveDetailQueryServiceImpl implements IMotorValveDetailQueryService {
    private final com.ym.iot.jetlinks.service.IJetLinksDeviceService jetLinksDevices;
    private final com.ym.iot.jetlinks.service.IJetLinksProductService jetLinksProducts;
    private final com.ym.iot.jetlinks.service.IJetLinksHistoryService jetLinksHistory;
    private final IIotDataPointService dataPointService;

    /** 查询档案中的阀型和控制选项，不向设备发送指令。 */
    @Override
    public ValveControlProfileVo getControlProfile(Long deviceId) {
        IotDevice device = loadDevice(deviceId);
        IotProduct product = loadProduct(device);
        ValveType type = resolveType(device, product);
        ValveControlProfileVo profile = new ValveControlProfileVo();
        profile.setValveType(type.getCode());
        profile.setValveTypeLabel(typeLabel(type, product));
        profile.setDirectConnect(
                product == null
                        || product.getNodeType() == null
                        || !"GATEWAY".equalsIgnoreCase(product.getNodeType().trim()));
        profile.setPositionTelemetryKey("K01~K04");
        profile.setOptions(type.listControlOptions());
        return profile;
    }

    /** 仅换算实际存在的遥测，不将缺失通道伪装为零开度。 */
    @Override
    public ValveCurrentPercentVo getCurrentPercent(Long deviceId) {
        IotDevice device = loadDevice(deviceId);
        IotProduct product = loadProduct(device);
        ValveType type = resolveType(device, product);
        ValveCurrentPercentVo vo = new ValveCurrentPercentVo();
        vo.setValveType(type.getCode());
        vo.setValveTypeLabel(typeLabel(type, product));
        vo.setSinglePort(type == ValveType.SINGLE_PORT);
        List<ValveCurrentPercentVo.ValvePercentItem> items = new ArrayList<>();
        IotLatestVo latest = dataPointService.getLatest(deviceId);
        if (latest != null && latest.getMetrics() != null) {
            for (int n = 1; n <= 4; n++) {
                BigDecimal position = latest.getMetrics().get("valve_position_" + n);
                if (position == null) continue;
                int angle = position.intValue();
                var result = ValvePercentControlCalculator.reverseCalculatePercent(type, angle);
                ValveCurrentPercentVo.ValvePercentItem item =
                        new ValveCurrentPercentVo.ValvePercentItem();
                item.setValveNo(n);
                item.setAngle(angle);
                item.setPercent(result.percent());
                item.setChannel(result.selectedOutlet());
                item.setSelectedOutlet(result.selectedOutlet());
                item.setOutletPercent(result.outletPercent());
                item.setChannels(result.channels());
                items.add(item);
            }
        }
        vo.setValves(items);
        return vo;
    }

    /** 查询当前租户控制日志，分页契约保持不变。 */
    @Override
    public PageResult<ValveControlLogVo> queryControlLogPage(
            ValveControlLogBo bo, PageQuery query) {
        return jetLinksHistory.valveLogs(bo, query);
    }

    private IotDevice loadDevice(Long deviceId) {
        IotDevice device =
                cn.hutool.core.bean.BeanUtil.toBean(
                        jetLinksDevices.queryById(deviceId), IotDevice.class);
        if (device == null) throw new ServiceException("设备不存在");
        return device;
    }

    private IotProduct loadProduct(IotDevice device) {
        return device.getProductId() == null
                ? null
                : cn.hutool.core.bean.BeanUtil.toBean(
                        jetLinksProducts.queryById(device.getProductId()), IotProduct.class);
    }

    private ValveType resolveType(IotDevice device, IotProduct product) {
        return com.ym.iot.jetlinks.support.JetLinksValveMetadata.configured(
                jetLinksDevices.get(device.getDeviceId()).data());
    }

    private String typeLabel(ValveType type, IotProduct product) {
        return MotorvalveValveTypeResolver.resolveDisplayLabel(
                type, product == null ? null : product.getProductKey());
    }
}
