package com.ym.iot.jetlinks.service.impl;

import cn.hutool.core.bean.BeanUtil;

import com.ym.common.core.domain.PageResult;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.iot.jetlinks.service.IJetLinksCommandService;
import com.ym.iot.jetlinks.service.IJetLinksValveService;
import com.ym.iot.jetlinks.support.JetLinksAccess;
import com.ym.iot.jetlinks.support.JetLinksMapping;
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
import com.ym.iot.motorvalve.service.IMotorValveDetailQueryService;
import com.ym.iot.motorvalve.service.IMotorValveDeviceQueryService;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Primary
@ConditionalOnJetLinks
@RequiredArgsConstructor
public class JetLinksValveServiceImpl implements IJetLinksValveService {
    private final IJetLinksCommandService commands;
    private final JetLinksAccess access;
    private final IMotorValveDeviceQueryService devices;
    private final IMotorValveDetailQueryService details;

    @Override
    public List<MotorValveDeviceRowVo> listValveDevices(String online) {
        return devices.list(online);
    }

    @Override
    public PageResult<MotorValveDeviceRowVo> pageValveDevices(
            String online, String code, PageQuery page) {
        return devices.page(online, code, page);
    }

    @Override
    public String controlValve(Long id, ValveControlBo bo) {
        return JetLinksCommandServiceImpl.commandText(
                commands.awaitPayload(
                        commands.submit(id, "controlValve", JetLinksMapping.data(bo))));
    }

    @Override
    public ValvePercentControlResultVo percentControl(Long id, ValvePercentControlBo bo) {
        var command =
                commands.awaitPayload(
                        commands.submit(id, "percentControl", JetLinksMapping.data(bo)));
        JetLinksCommandServiceImpl.commandText(command);
        Map<String, Object> result =
                new LinkedHashMap<>(JetLinksCommandServiceImpl.result(command));
        result.put("commandText", JetLinksCommandServiceImpl.commandText(command));
        if (result.containsKey("targetAngle")) result.put("angle", result.get("targetAngle"));
        result.putIfAbsent("valveType", getControlProfile(id).getValveType());
        return BeanUtil.toBean(result, ValvePercentControlResultVo.class);
    }

    @Override
    public List<ValveBatchControlResultVo> batchControl(List<ValveBatchControlItemBo> items) {
        if (items == null || items.isEmpty()) return List.of();
        if (items.stream().anyMatch(Objects::isNull)) throw new ServiceException("批量控制项不能为空");
        List<Long> ids = items.stream().map(ValveBatchControlItemBo::getDeviceId).toList();
        access.require(ids); // complete authorization before the first side effect
        Set<Long> duplicates = new HashSet<>(), seen = new HashSet<>();
        for (Long id : ids) if (!seen.add(id)) duplicates.add(id);
        List<ValveBatchControlResultVo> result = new ArrayList<>();
        for (var item : items) {
            if (duplicates.contains(item.getDeviceId())) {
                result.add(
                        new ValveBatchControlResultVo(
                                item.getDeviceId(), false, null, "同一批次deviceId重复，未下发"));
                continue;
            }
            try {
                result.add(
                        new ValveBatchControlResultVo(
                                item.getDeviceId(),
                                true,
                                controlValve(item.getDeviceId(), item),
                                "命令已提交，设备执行结果以持久事件确认"));
            } catch (RuntimeException e) {
                result.add(
                        new ValveBatchControlResultVo(
                                item.getDeviceId(), false, null, e.getMessage()));
            }
        }
        return result;
    }

    @Override
    public ValveControlProfileVo getControlProfile(Long id) {
        return details.getControlProfile(id);
    }

    @Override
    public ValveCurrentPercentVo getCurrentPercent(Long id) {
        return details.getCurrentPercent(id);
    }

    @Override
    public String readData(Long id, List<String> lora, Integer type) {
        Map<String, Object> p = new LinkedHashMap<>();
        if (lora != null) p.put("loraAddrs", lora);
        if (type != null) p.put("dataType", type);
        return JetLinksCommandServiceImpl.commandText(
                commands.awaitPayload(commands.submit(id, "readData", p)));
    }

    @Override
    public String readGatewayStatus(Long id) {
        return JetLinksCommandServiceImpl.commandText(
                commands.awaitPayload(commands.submit(id, "readGatewayStatus", Map.of())));
    }

    @Override
    public String syncNtpTime(Long id) {
        return JetLinksCommandServiceImpl.commandText(
                commands.awaitPayload(commands.submit(id, "syncNtpTime", Map.of())));
    }

    @Override
    public List<String> batchReadData(List<Long> ids, List<String> lora, Integer type) {
        if (ids == null || ids.isEmpty()) return List.of();
        access.require(ids);
        return ids.stream().distinct().map(id -> readData(id, lora, type)).toList();
    }

    @Override
    public PageResult<ValveControlLogVo> queryControlLogPage(ValveControlLogBo bo, PageQuery page) {
        return details.queryControlLogPage(bo, page);
    }
}
