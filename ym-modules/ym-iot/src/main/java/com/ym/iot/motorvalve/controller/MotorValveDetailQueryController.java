package com.ym.iot.motorvalve.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;

import com.ym.common.core.domain.PageResult;
import com.ym.common.core.domain.R;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.iot.motorvalve.domain.bo.ValveControlLogBo;
import com.ym.iot.motorvalve.domain.vo.ValveControlLogVo;
import com.ym.iot.motorvalve.domain.vo.ValveControlProfileVo;
import com.ym.iot.motorvalve.domain.vo.ValveCurrentPercentVo;
import com.ym.iot.motorvalve.service.IMotorValveDetailQueryService;
import com.ym.iot.motorvalve.service.IValveCommandService;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 与设备列表一致，MQTT 关闭时仍提供登录态下的只读详情。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/motorvalve")
public class MotorValveDetailQueryController {
    private final IMotorValveDetailQueryService queryService;
    private final ObjectProvider<IValveCommandService> commandServiceProvider;

    /** 控制服务是否启用；不代表设备在线，也不替代命令接口的权限检查。 */
    public record Capabilities(boolean commandEnabled) {}

    @SaCheckPermission(value={"iot:motorvalve:list","iot:device:list","iot:device:query"},mode=SaMode.OR)
    @GetMapping("/capabilities")
    public R<Capabilities> capabilities() {
        return R.ok(new Capabilities(commandServiceProvider.getIfAvailable() != null));
    }

    @Log(title = "电动阀控阀能力", businessType = BusinessType.OTHER)
    @SaCheckPermission(value={"iot:motorvalve:list","iot:device:list","iot:device:query"},mode=SaMode.OR)
    @GetMapping("/{deviceId}/control-profile")
    public R<ValveControlProfileVo> getControlProfile(@PathVariable Long deviceId) {
        return R.ok(queryService.getControlProfile(deviceId));
    }

    @Log(title = "电动阀当前百分比", businessType = BusinessType.OTHER)
    @SaCheckPermission(value={"iot:motorvalve:list","iot:device:list","iot:device:query"},mode=SaMode.OR)
    @GetMapping("/{deviceId}/current-percent")
    public R<ValveCurrentPercentVo> getCurrentPercent(@PathVariable Long deviceId) {
        return R.ok(queryService.getCurrentPercent(deviceId));
    }

    @Log(title = "电动阀控制日志查询", businessType = BusinessType.OTHER)
    @SaCheckPermission(value={"iot:motorvalve:list","iot:device:list","iot:device:query"},mode=SaMode.OR)
    @GetMapping("/control-logs")
    public R<PageResult<ValveControlLogVo>> queryControlLogPage(ValveControlLogBo bo, PageQuery query) {
        return R.ok(queryService.queryControlLogPage(bo, query));
    }
}
