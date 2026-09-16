package com.ym.farm.task.miniapp.clocklocation;

import com.ym.agriculture.api.farmtask.RemoteFarmTaskService;
import com.ym.agriculture.api.farmtask.domain.vo.RemoteClockLocationVo;
import com.ym.common.core.domain.R;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 农事任务小程序打卡地点接口。
 */
@RestController
@RequestMapping("/miniapp/smart-farming/stask/clock-location")
public class FarmTaskClockLocationController {

    @DubboReference
    private RemoteFarmTaskService farmTaskService;

    @GetMapping
    public R<RemoteClockLocationVo> get() {
        return R.ok(farmTaskService.getClockLocation());
    }
}
