package com.ym.iot.motorvalve.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;

import com.baomidou.lock.annotation.Lock4j;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.domain.R;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.redis.annotation.RepeatSubmit;
import com.ym.iot.motorvalve.domain.bo.ValveBatchControlItemBo;
import com.ym.iot.motorvalve.domain.bo.ValveControlBo;
import com.ym.iot.motorvalve.domain.bo.ValvePercentControlBo;
import com.ym.iot.motorvalve.domain.bo.ValveSessionBo;
import com.ym.iot.motorvalve.domain.vo.ValveBatchControlResultVo;
import com.ym.iot.motorvalve.domain.vo.ValvePercentControlResultVo;
import com.ym.iot.motorvalve.domain.vo.ValveSessionStatsVo;
import com.ym.iot.motorvalve.domain.vo.ValveSessionVo;
import com.ym.iot.motorvalve.service.IValveCommandService;
import com.ym.iot.motorvalve.service.IValveSessionService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 电动阀控制 REST 接口。
 *
 * @author ym-cloud
 */
@Validated
@RestController
@RequestMapping("/motorvalve")
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression(
        "${ym.iot.jetlinks.enabled:false} || (${ym.mqtt.enabled:false} &&"
                + " ${ym.iot.motorvalve.enabled:false})")
@RequiredArgsConstructor
public class ValveController {

    private final IValveCommandService valveCommandService;
    private final IValveSessionService valveSessionService;

    /**
     * 控制阀门。一次只控制一个 {@code valveNo}；{@code position} 支持 0-360 度，{@code action} 为快捷预设。
     *
     * @param deviceId 设备主键（iot_device.id）
     * @param bo 控制参数
     * @return 下发的指令文本
     */
    @Log(title = "电动阀控制", businessType = BusinessType.OTHER)
    @SaCheckPermission("iot:motorvalve:control")
    @Lock4j(keys = {"#deviceId"})
    @RepeatSubmit(interval = 30000, message = "操作过于频繁，同一设备30秒内只能操作一次，请稍后再试")
    @PostMapping("/{deviceId}/control")
    public R<String> controlValve(
            @PathVariable Long deviceId, @Valid @RequestBody ValveControlBo bo) {
        String commandText = valveCommandService.controlValve(deviceId, bo);
        return R.ok(commandText);
    }

    /**
     * 按百分比控制阀门。
     *
     * <p>单通阀使用 percent 连续换算角度；三通/五通使用 channel + percent 控制单个出口。
     *
     * @param deviceId 设备主键（iot_device.id）
     * @param bo 百分比控制参数
     * @return 换算后的协议角度与实际下发指令
     */
    @Log(title = "电动阀百分比控制", businessType = BusinessType.OTHER)
    @SaCheckPermission("iot:motorvalve:percentControl")
    @Lock4j(keys = {"#deviceId"})
    @RepeatSubmit(interval = 30000, message = "操作过于频繁，同一设备30秒内只能操作一次，请稍后再试")
    @PostMapping("/{deviceId}/percent-control")
    public R<ValvePercentControlResultVo> percentControl(
            @PathVariable Long deviceId, @Valid @RequestBody ValvePercentControlBo bo) {
        return R.ok(valveCommandService.percentControl(deviceId, bo));
    }

    /**
     * 批量控制多个设备。每个设备一次只控制一个阀门，可分别指定 valveNo、position、loraAddrs。
     *
     * @param items 批量控制项列表
     * @return 每台设备的下发结果
     */
    @Log(title = "电动阀批量控制", businessType = BusinessType.OTHER)
    @SaCheckPermission("iot:motorvalve:batchControl")
    @RepeatSubmit(interval = 30000, message = "操作过于频繁，同一设备30秒内只能操作一次，请稍后再试")
    @PostMapping("/batch/control")
    public R<List<ValveBatchControlResultVo>> batchControl(
            @Valid @RequestBody List<ValveBatchControlItemBo> items) {
        return R.ok(valveCommandService.batchControl(items));
    }

    /**
     * 读取设备数据。
     *
     * @param deviceId 设备主键
     * @param loraAddrs LoRa阀门地址列表，4G设备不传
     * @return 下发的指令文本
     */
    @Log(title = "电动阀读取数据", businessType = BusinessType.OTHER)
    @Lock4j(keys = {"#deviceId"})
    @SaCheckPermission("iot:motorvalve:read")
    @PostMapping("/{deviceId}/read-data")
    public R<String> readData(
            @PathVariable Long deviceId,
            @RequestParam(required = false) List<String> loraAddrs,
            @RequestParam(required = false) Integer dataType) {
        String commandText = valveCommandService.readData(deviceId, loraAddrs, dataType);
        return R.ok(commandText);
    }

    /**
     * 读取网关状态。
     *
     * @param deviceId 设备主键
     * @return 下发的指令文本
     */
    @Log(title = "电动阀读取网关状态", businessType = BusinessType.OTHER)
    @Lock4j(keys = {"#deviceId"})
    @SaCheckPermission("iot:motorvalve:read")
    @PostMapping("/{deviceId}/gateway-status")
    public R<String> readGatewayStatus(@PathVariable Long deviceId) {
        String commandText = valveCommandService.readGatewayStatus(deviceId);
        return R.ok(commandText);
    }

    /**
     * NTP 时间同步。
     *
     * @param deviceId 设备主键
     * @return 下发的指令文本
     */
    @Log(title = "电动阀NTP同步", businessType = BusinessType.OTHER)
    @SaCheckPermission("iot:motorvalve:ntpSync")
    @Lock4j(keys = {"#deviceId"})
    @PostMapping("/{deviceId}/ntp-sync")
    public R<String> syncNtpTime(@PathVariable Long deviceId) {
        String commandText = valveCommandService.syncNtpTime(deviceId);
        return R.ok(commandText);
    }

    /**
     * 批量读取设备数据。
     *
     * @param deviceIds 设备主键列表
     * @param loraAddrs LoRa阀门地址列表，4G设备不传
     * @return 每台设备下发的指令文本列表
     */
    @Log(title = "电动阀批量读取数据", businessType = BusinessType.OTHER)
    @SaCheckPermission("iot:motorvalve:read")
    @PostMapping("/batch/read-data")
    public R<List<String>> batchReadData(
            @RequestBody List<Long> deviceIds,
            @RequestParam(required = false) List<String> loraAddrs,
            @RequestParam(required = false) Integer dataType) {
        List<String> results = valveCommandService.batchReadData(deviceIds, loraAddrs, dataType);
        return R.ok(results);
    }

    /**
     * 分页查询开阀会话（含开启持续时间）。
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    @Log(title = "电动阀开阀会话查询", businessType = BusinessType.OTHER)
    @SaCheckPermission(
            value = {"iot:motorvalve:list", "iot:device:list", "iot:device:query"},
            mode = SaMode.OR)
    @GetMapping("/sessions")
    public R<PageResult<ValveSessionVo>> querySessionPage(ValveSessionBo bo, PageQuery pageQuery) {
        return R.ok(valveSessionService.querySessionPage(bo, pageQuery));
    }

    /**
     * 开阀会话汇总统计（累计时长等）。
     *
     * @param bo 查询条件（deviceId、valveNo、beginTime、endTime）
     * @return 汇总统计
     */
    @Log(title = "电动阀开阀会话统计", businessType = BusinessType.OTHER)
    @SaCheckPermission(
            value = {"iot:motorvalve:list", "iot:device:list", "iot:device:query"},
            mode = SaMode.OR)
    @GetMapping("/sessions/stats")
    public R<ValveSessionStatsVo> querySessionStats(ValveSessionBo bo) {
        return R.ok(valveSessionService.queryStats(bo));
    }

    /**
     * 查询当前仍开启中的阀门会话。
     *
     * @param deviceId 设备主键，可选
     * @return 开启中会话列表（含 currentDurationSeconds）
     */
    @Log(title = "电动阀当前开启会话", businessType = BusinessType.OTHER)
    @SaCheckPermission(
            value = {"iot:motorvalve:list", "iot:device:list", "iot:device:query"},
            mode = SaMode.OR)
    @GetMapping("/sessions/open")
    public R<List<ValveSessionVo>> listOpenSessions(@RequestParam(required = false) Long deviceId) {
        return R.ok(valveSessionService.listOpenSessions(deviceId));
    }

    /**
     * 查询单设备当前仍开启中的阀门会话。
     *
     * @param deviceId 设备主键
     * @return 开启中会话列表
     */
    @Log(title = "电动阀设备开启会话", businessType = BusinessType.OTHER)
    @SaCheckPermission(
            value = {"iot:motorvalve:list", "iot:device:list", "iot:device:query"},
            mode = SaMode.OR)
    @GetMapping("/devices/{deviceId}/sessions/open")
    public R<List<ValveSessionVo>> listDeviceOpenSessions(@PathVariable Long deviceId) {
        return R.ok(valveSessionService.listOpenSessions(deviceId));
    }
}
