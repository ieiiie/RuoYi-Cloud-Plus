package com.ym.agriculture.miniapp.solarterms;

import com.ym.agriculture.api.farming.RemoteAgricultureService;
import com.ym.agriculture.api.farming.domain.vo.RemoteSolarTermDetailVo;
import com.ym.common.core.domain.R;
import jakarta.validation.constraints.NotBlank;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 农业小程序节气查询聚合接口。
 */
@Validated
@RestController
@RequestMapping("/mobile/smart-farming/solar-terms")
public class MobileSolarTermController {

    @DubboReference
    private RemoteAgricultureService agricultureService;

    @GetMapping("/current")
    public R<RemoteSolarTermDetailVo> current() {
        return R.ok(agricultureService.getCurrentSolarTerm());
    }

    @GetMapping("/{termCode}")
    public R<RemoteSolarTermDetailVo> detail(@NotBlank @PathVariable String termCode,
                                             @RequestParam(required = false) Integer year) {
        return R.ok(agricultureService.getSolarTerm(termCode, year));
    }
}
