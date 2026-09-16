package com.ym.agriculture.miniapp.home;

import com.ym.agriculture.api.farming.RemoteAgricultureMobileService;
import com.ym.agriculture.api.farming.domain.vo.RemoteAgricultureViewVo;
import com.ym.common.core.domain.R;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 农业小程序首页与地图聚合。 */
@RestController
@RequestMapping("/mobile/smart-farming/home")
public class MobileHomeController {

    @DubboReference
    private RemoteAgricultureMobileService agricultureService;

    @GetMapping("/overview")
    public R<RemoteAgricultureViewVo> overview(@RequestParam(required = false) Long fieldId) {
        return R.ok(agricultureService.homeOverview(fieldId));
    }

    @GetMapping({"/map-plots", "/map-plots-all"})
    public R<Object> mapPlots(@RequestParam(required = false) Long fieldId) {
        return R.ok(agricultureService.homeMap(fieldId).get("plots"));
    }

    @GetMapping({"/weather", "/weather-detail", "/weather/next-24h"})
    public R<RemoteAgricultureViewVo> weather() {
        return R.ok(agricultureService.homeWeather());
    }

    @GetMapping("/sensor-summary-tree")
    public R<RemoteAgricultureViewVo> sensorSummary() {
        return R.ok(agricultureService.sensorSummary());
    }
}
