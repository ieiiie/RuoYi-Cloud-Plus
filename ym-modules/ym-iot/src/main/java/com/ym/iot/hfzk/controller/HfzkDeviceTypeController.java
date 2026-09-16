package com.ym.iot.hfzk.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;

import com.ym.common.core.domain.PageResult;
import com.ym.common.core.domain.R;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.iot.device.domain.vo.IotDeviceVo;
import com.ym.iot.hfzk.service.IHfzkDeviceTypeQueryService;

import jakarta.validation.constraints.NotBlank;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 按 device_type 从 HFZK 登记表查询设备列表。
 *
 * <p>查询链路：{@code iot_hfzk_user_device (device_type=?) → external_device_id → iot_device}
 *
 * @author ym-cloud
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/iot/hfzk/device-type")
public class HfzkDeviceTypeController {

    private final IHfzkDeviceTypeQueryService hfzkDeviceTypeQueryService;

    /**
     * 按设备类型查询设备列表（不分页）。
     *
     * @param deviceType HFZK 设备类型（必填，如 MOTORVALVE、FERTILIZER、WVP_CAMERA）
     * @param onlineStatus 在线状态筛选（可选：ONLINE/OFFLINE/FAULT）
     * @return 设备列表
     */
    @SaCheckPermission(value={"iot:device:list","iot:device:query","iot:monitor:list","iot:video:list","iot:fertilizer:list","iot:motorvalve:list","sf:field:list"},mode=SaMode.OR)
    @GetMapping("/list")
    public R<List<IotDeviceVo>> listByDeviceType(
            @RequestParam @NotBlank(message = "deviceType不能为空") String deviceType,
            @RequestParam(required = false) String onlineStatus) {
        return R.ok(hfzkDeviceTypeQueryService.listByDeviceType(deviceType, onlineStatus));
    }

    /**
     * 按设备类型分页查询设备。
     *
     * @param deviceType HFZK 设备类型（必填）
     * @param onlineStatus 在线状态筛选（可选）
     * @param deviceCode 设备编号模糊搜索（可选）
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    @SaCheckPermission(value={"iot:device:list","iot:device:query","iot:monitor:list","iot:video:list","iot:fertilizer:list","iot:motorvalve:list","sf:field:list"},mode=SaMode.OR)
    @GetMapping("/page")
    public R<PageResult<IotDeviceVo>> pageByDeviceType(
            @RequestParam @NotBlank(message = "deviceType不能为空") String deviceType,
            @RequestParam(required = false) String onlineStatus,
            @RequestParam(required = false) String deviceCode,
            PageQuery pageQuery) {
        return R.ok(hfzkDeviceTypeQueryService.pageByDeviceType(deviceType, onlineStatus, deviceCode, pageQuery));
    }
}
