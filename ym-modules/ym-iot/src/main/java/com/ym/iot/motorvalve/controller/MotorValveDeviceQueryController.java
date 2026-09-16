package com.ym.iot.motorvalve.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;

import com.ym.common.core.domain.PageResult;
import com.ym.common.core.domain.R;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.iot.motorvalve.domain.vo.MotorValveDeviceRowVo;
import com.ym.iot.motorvalve.service.IMotorValveDeviceQueryService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 电动阀设备查询接口；关闭 MQTT 实时控制时仍保持页面可读。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/motorvalve/devices")
public class MotorValveDeviceQueryController {

    private final IMotorValveDeviceQueryService queryService;

    @Log(title = "电动阀设备列表", businessType = BusinessType.OTHER)
    @SaCheckPermission(value={"iot:motorvalve:list","iot:device:list","iot:device:query"},mode=SaMode.OR)
    @GetMapping
    public R<List<MotorValveDeviceRowVo>> list(@RequestParam(required = false) String onlineStatus) {
        return R.ok(queryService.list(onlineStatus));
    }

    @Log(title = "电动阀设备分页", businessType = BusinessType.OTHER)
    @SaCheckPermission(value={"iot:motorvalve:list","iot:device:list","iot:device:query"},mode=SaMode.OR)
    @GetMapping("/page")
    public R<PageResult<MotorValveDeviceRowVo>> page(@RequestParam(required = false) String onlineStatus,
                                                   @RequestParam(required = false) String deviceCode,
                                                   PageQuery pageQuery) {
        return R.ok(queryService.page(onlineStatus, deviceCode, pageQuery));
    }
}
