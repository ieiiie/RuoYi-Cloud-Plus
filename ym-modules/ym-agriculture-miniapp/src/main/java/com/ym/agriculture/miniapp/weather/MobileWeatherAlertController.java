package com.ym.agriculture.miniapp.weather;

import com.ym.agriculture.api.farming.RemoteAgricultureMobileService;
import com.ym.agriculture.api.farming.domain.vo.RemoteAgricultureViewVo;
import com.ym.agriculture.miniapp.support.MobileRequestAdapter;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.domain.R;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 移动端气象预警兼容接口。 */
@RestController
@RequestMapping("/mobile/smart-farming/weather-alerts")
public class MobileWeatherAlertController {

    @DubboReference
    private RemoteAgricultureMobileService agricultureService;

    @GetMapping("/summary")
    public R<RemoteAgricultureViewVo> summary() {
        return R.ok(agricultureService.weatherAlertSummary());
    }

    @GetMapping("/page")
    public R<PageResult<RemoteAgricultureViewVo>> page(@RequestParam MultiValueMap<String, String> parameters) {
        return R.ok(agricultureService.pageWeatherAlerts(MobileRequestAdapter.query(parameters)));
    }

    @GetMapping("/{warningId}")
    public R<RemoteAgricultureViewVo> detail(@PathVariable String warningId) {
        return R.ok(agricultureService.getWeatherAlert(warningId));
    }
}
