package com.ym.agriculture.miniapp.crop;

import com.ym.agriculture.api.farming.RemoteAgricultureService;
import com.ym.agriculture.api.farming.domain.vo.RemoteCropVarietySimpleVo;
import com.ym.common.core.domain.R;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 农业小程序作物查询聚合接口。
 */
@RestController
@RequestMapping("/mobile/smart-farming/crops")
public class MobileCropController {

    @DubboReference
    private RemoteAgricultureService agricultureService;

    @GetMapping("/varieties/simple")
    public R<List<RemoteCropVarietySimpleVo>> varieties() {
        return R.ok(agricultureService.listEnabledCropVarieties());
    }
}
