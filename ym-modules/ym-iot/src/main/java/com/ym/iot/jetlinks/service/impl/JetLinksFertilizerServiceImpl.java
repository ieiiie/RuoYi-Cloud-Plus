package com.ym.iot.jetlinks.service.impl;

import com.alibaba.fastjson2.JSON;
import com.ym.common.core.exception.ServiceException;
import com.ym.iot.fertilizer.domain.bo.FertilizerResetBo;
import com.ym.iot.fertilizer.domain.bo.FertilizerTankParamBo;
import com.ym.iot.fertilizer.domain.dto.FertilizerRuntimeConfig;
import com.ym.iot.fertilizer.domain.vo.FertilizerStateSnapshot;
import com.ym.iot.fertilizer.domain.vo.TaskInfo;
import com.ym.iot.fertilizer.enums.FertilizerDeviceState;
import com.ym.iot.fertilizer.support.FertilizerStateProjector;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.iot.jetlinks.service.IJetLinksDeviceService;
import com.ym.iot.jetlinks.service.IJetLinksFertilizerService;
import com.ym.iot.jetlinks.service.IJetLinksFertilizerTasks;
import com.ym.iot.jetlinks.service.IJetLinksTagService;
import com.ym.iot.jetlinks.service.IJetLinksTelemetryService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
@ConditionalOnJetLinks
@RequiredArgsConstructor
public class JetLinksFertilizerServiceImpl implements IJetLinksFertilizerService {
    private final IJetLinksTelemetryService telemetry;
    private final IJetLinksDeviceService devices;
    private final IJetLinksTagService tags;
    private final IJetLinksFertilizerTasks tasks;

    public FertilizerStateSnapshot getState(Long id) {
        var latest = telemetry.getLatest(id);
        var device = devices.queryById(id);
        Map<String, Object> values = new LinkedHashMap<>();
        if (latest != null) {
            values.putAll(latest.getMetrics());
            latest.getTexts()
                    .forEach(
                            (k, v) -> {
                                Object value = v;
                                if (k.endsWith("Flags")) value = JSON.parseObject(v);
                                values.put(k, value);
                            });
        }
        Date source =
                latest == null
                        ? null
                        : latest.getCollectTimes().values().stream()
                                .max(Date::compareTo)
                                .orElse(null);
        var state =
                FertilizerStateProjector.fromCoreTelemetry(
                        id, device.getDeviceCode(), values, source);
        if (!"ONLINE".equals(device.getOnlineStatus()))
            state.setState(FertilizerDeviceState.OFFLINE);
        Map<String, String> tankTags = new LinkedHashMap<>();
        for (var tag : tags.queryByDeviceId(id))
            if (Set.of("Tank1", "Tank2", "Tank3").contains(tag.getTagKey()))
                tankTags.put(tag.getTagKey(), tag.getTagValue());
        state.setTankTags(tankTags);
        return state;
    }

    public FertilizerStateSnapshot getStateWithParams(Long id) {
        return getState(id);
    }

    public FertilizerRuntimeConfig runtimeConfig() {
        return new FertilizerRuntimeConfig(false);
    }

    private static Map<String, Object> step(String function, Map<String, Object> inputs) {
        return Map.of("functionId", function, "inputs", inputs);
    }

    private TaskInfo enqueue(Long id, String label, List<Map<String, Object>> plan) {
        return tasks.enqueue(id, label, plan);
    }

    private TaskInfo control(Long id, String label, String command) {
        return enqueue(id, label, List.of(step("control", Map.of("command", command))));
    }

    public TaskInfo start(Long id, List<FertilizerTankParamBo> ignoredCompatibilityParams) {
        return control(id, "启动", "START");
    }

    public TaskInfo startWithoutReset(Long id) {
        return control(id, "无复位启动", "START_WITHOUT_RESET");
    }

    public TaskInfo stop(Long id) {
        return control(id, "有序停止", "STOP");
    }

    public TaskInfo emergencyStop(Long id, String reason) {
        return tasks.enqueue(
                id, "紧急停止", List.of(step("control", Map.of("command", "EMERGENCY_STOP"))), reason);
    }

    public TaskInfo primeWater(Long id) {
        return control(id, "加引水", "PRIME_WATER");
    }

    public TaskInfo cleanTank(Long id) {
        return control(id, "清罐", "CLEAN_TANK");
    }

    public TaskInfo reset(Long id) {
        return reset(id, null);
    }

    public TaskInfo reset(Long id, FertilizerResetBo bo) {
        List<Map<String, Object>> plan = new ArrayList<>();
        if (bo != null && Boolean.TRUE.equals(bo.getContinuePending())) {
            var snap = getState(id);
            Integer[] pending = {
                snap.getPendingFert1Kg(), snap.getPendingFert2Kg(), snap.getPendingFert3Kg()
            };
            for (int n = 0; n < 3; n++)
                if (pending[n] != null && pending[n] > 1)
                    plan.add(
                            step(
                                    "writeRegister",
                                    Map.of("address", 103 + n * 4, "value", pending[n])));
        }
        plan.add(step("control", Map.of("command", "RESET")));
        return enqueue(id, "复位", plan);
    }

    public TaskInfo refreshRealtime(Long id) {
        return enqueue(id, "读取实时数据", List.of(step("readRealtimeData", Map.of())));
    }

    public TaskInfo readCurrentParams(Long id) {
        return enqueue(
                id,
                "读取当前参数",
                List.of(step("readRealtimeData", Map.of()), step("readSettings", Map.of())));
    }

    public TaskInfo applyParams(Long id, List<FertilizerTankParamBo> tanks) {
        if (tanks == null || tanks.isEmpty()) throw new ServiceException("施肥罐参数不能为空");
        var snapshot = getState(id);
        List<Map<String, Object>> plan = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (var tank : tanks) {
            int n = tank.getTankNo();
            if (n < 1 || n > 3 || !seen.add(n)) throw new ServiceException("罐号必须为1到3且不能重复");
            if (tank.getQuantity() == null) continue;
            if (tank.getQuantity() < 0 || tank.getQuantity() > 10000)
                throw new ServiceException("施肥量必须在0到10000kg");
            int base = 100 + (n - 1) * 4;
            if (tank.getDelay() != null) {
                if (tank.getDelay() < 0 || tank.getDelay() > 10000)
                    throw new ServiceException("施肥延时超出范围");
                plan.add(step("writeRegister", Map.of("address", base, "value", tank.getDelay())));
            }
            if (tank.getFlow() != null) {
                Float density =
                        switch (n) {
                            case 1 -> snapshot.getT1Density();
                            case 2 -> snapshot.getT2Density();
                            default -> snapshot.getT3Density();
                        };
                if (density == null || density <= 0) throw new ServiceException("密度未读取，请先读取当前参数");
                if (tank.getFlow() < 300 * density || tank.getFlow() > 1500 * density)
                    throw new ServiceException("施肥流量超出密度对应范围");
                plan.add(
                        step(
                                "writeRegister",
                                Map.of("address", base + 2, "value", tank.getFlow())));
            }
            if (tank.getQuantity() == 0)
                plan.add(step("writeRegister", Map.of("address", base + 1, "value", 0)));
            plan.add(
                    step(
                            "writeRegister",
                            Map.of("address", base + 3, "value", tank.getQuantity())));
            if (tank.getTotalFertilizer() != null) {
                if (tank.getTotalFertilizer().signum() < 0) throw new ServiceException("累计施肥量不得为负");
                plan.add(
                        step(
                                "writeRegister",
                                Map.of("address", 120 + n, "value", tank.getTotalFertilizer())));
            }
            // time is derived by the device; density/mix fields are display-only, as in the
            // original service.
        }
        if (plan.isEmpty()) throw new ServiceException("没有可写入的施肥参数");
        return enqueue(id, "参数下发与回读验证", plan);
    }

    public TaskInfo getTask(String id) {
        return tasks.getTask(id);
    }

    public TaskInfo getActiveTask(Long id) {
        return tasks.active(id);
    }
}
