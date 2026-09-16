package com.ym.iot.device.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;

import com.ym.common.core.domain.R;
import com.ym.common.web.core.BaseController;
import com.ym.iot.device.domain.vo.IotDeviceTagVo;
import com.ym.iot.device.service.IIotDeviceTagService;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 设备标签
 * <p>
 * 查询设备的 Key-Value 标签。路径：{@code /iot/device/tag}
 * </p>
 *
 * @author ym-cloud
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/iot/device/tag")
public class IotDeviceTagController extends BaseController {

    private final IIotDeviceTagService tagService;

    /**
     * 按设备 ID 查询标签列表
     *
     * @param device_id 设备主键
     * @return 统一响应，{@code data} 为标签列表
     */
    @SaCheckPermission(value={"iot:device:list","iot:device:query","iot:monitor:list","iot:video:list","iot:fertilizer:list","iot:motorvalve:list","sf:field:list"},mode=SaMode.OR)
    @GetMapping("/list")
    public R<List<IotDeviceTagVo>> list(@RequestParam Long device_id) {
        return R.ok(tagService.queryByDeviceId(device_id));
    }

}
