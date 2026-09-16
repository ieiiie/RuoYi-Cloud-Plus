package com.ym.iot.hfzk.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;

import com.ym.common.core.domain.PageResult;
import com.ym.common.core.domain.R;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.web.core.BaseController;
import com.ym.iot.hfzk.domain.bo.IotHfzkUserDeviceBo;
import com.ym.iot.hfzk.domain.vo.IotHfzkUserDeviceVo;
import com.ym.iot.hfzk.service.IIotHfzkUserDeviceService;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 中科合肥同步用户设备列表
 * <p>
 * 路径：{@code /iot/hfzk/user-device}
 * </p>
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/iot/hfzk/user-device")
public class IotHfzkUserDeviceController extends BaseController {

    private final IIotHfzkUserDeviceService hfzkUserDeviceService;

    /**
     * 查询设备列表（不分页，当前租户）。
     */
    @SaCheckPermission(value={"iot:device:list","iot:device:query","iot:monitor:list","iot:video:list","iot:fertilizer:list","iot:motorvalve:list","sf:field:list"},mode=SaMode.OR)
    @GetMapping("/list")
    public R<List<IotHfzkUserDeviceVo>> list(IotHfzkUserDeviceBo bo) {
        return R.ok(hfzkUserDeviceService.queryList(bo));
    }

    /**
     * 分页查询设备列表（当前租户）。
     */
    @SaCheckPermission(value={"iot:device:list","iot:device:query","iot:monitor:list","iot:video:list","iot:fertilizer:list","iot:motorvalve:list","sf:field:list"},mode=SaMode.OR)
    @GetMapping("/page")
    public R<PageResult<IotHfzkUserDeviceVo>> page(IotHfzkUserDeviceBo bo, PageQuery pageQuery) {
        return R.ok(hfzkUserDeviceService.queryPageList(bo, pageQuery));
    }
}
