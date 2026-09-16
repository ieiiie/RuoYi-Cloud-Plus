package com.ym.agriculture.miniapp.solarterms;

import com.ym.agriculture.api.farming.RemoteAgricultureService;
import com.ym.agriculture.api.farming.domain.vo.RemoteCalendarDateInfoVo;
import com.ym.common.core.domain.R;
import jakarta.validation.constraints.NotNull;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/**
 * 农业小程序农历与节气日期解释接口。
 */
@Validated
@RestController
@RequestMapping("/mobile/smart-farming/calendar")
public class MobileCalendarController {

    @DubboReference
    private RemoteAgricultureService agricultureService;

    @GetMapping("/date-info")
    public R<RemoteCalendarDateInfoVo> dateInfo(
        @NotNull(message = "发生时间不能为空") @RequestParam
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date happenedAt) {
        return R.ok(agricultureService.getCalendarDateInfo(happenedAt));
    }
}
