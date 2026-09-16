package com.ym.farm.task.miniapp.farmwork;

import com.ym.agriculture.api.farming.RemoteAgricultureService;
import com.ym.agriculture.api.farming.domain.vo.RemoteFarmWorkTreeVo;
import com.ym.common.core.domain.R;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 农事任务小程序农事字典聚合接口。
 */
@RestController
@RequestMapping("/miniapp/smart-farming/stask/farm-work")
public class FarmTaskFarmWorkController {

    @DubboReference
    private RemoteAgricultureService agricultureService;

    @GetMapping("/tree")
    public R<List<RemoteFarmWorkTreeVo>> tree() {
        return R.ok(agricultureService.listEnabledFarmWorkTree());
    }
}
