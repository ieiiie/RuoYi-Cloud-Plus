package com.ym.agriculture.miniapp.device;

import com.ym.common.core.domain.PageResult;
import com.ym.common.core.domain.R;
import com.ym.iot.api.RemoteIotDeviceService;
import com.ym.iot.api.domain.bo.RemoteDeviceQueryBo;
import com.ym.iot.api.domain.vo.RemoteDeviceSummaryVo;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 农业小程序设备列表聚合接口。
 *
 * @author ym-cloud
 */
@RestController
@RequestMapping("/mobile/smart-farming/devices")
public class MobileDeviceController {

    @DubboReference
    private RemoteIotDeviceService iotDeviceService;

    @GetMapping("/list")
    public R<PageResult<RemoteDeviceSummaryVo>> list(RemoteDeviceQueryBo query) {
        return R.ok(iotDeviceService.pageDevices(query));
    }
}
