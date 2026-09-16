package com.ym.agriculture.farming.solarterms.controller;

import com.ym.common.core.domain.R;
import com.ym.common.web.core.BaseController;
import com.ym.agriculture.farming.solarterms.model.vo.CalendarDateInfoVo;
import com.ym.agriculture.farming.solarterms.service.ISfSolarTermService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/** 管理端通用农历与节气日期解释接口。 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/smart-farming/calendar")
public class SfCalendarController extends BaseController {

    private final ISfSolarTermService solarTermService;

    @GetMapping("/date-info")
    public R<CalendarDateInfoVo> dateInfo(
        @NotNull(message = "发生时间不能为空") @RequestParam
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date happenedAt) {
        return R.ok(solarTermService.getCalendarDateInfo(happenedAt));
    }
}
