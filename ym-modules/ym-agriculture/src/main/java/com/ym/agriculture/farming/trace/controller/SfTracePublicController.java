package com.ym.agriculture.farming.trace.controller;

import com.ym.common.core.domain.R;
import com.ym.common.web.core.BaseController;
import com.ym.agriculture.farming.trace.model.vo.SfTracePublicVo;
import com.ym.agriculture.farming.trace.service.ISfTracePublicService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * H5 公开溯源查询，前缀 {@code /smart-farming/trace/public}，免登录。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/smart-farming/trace/public")
public class SfTracePublicController extends BaseController {

    private final ISfTracePublicService tracePublicService;

    @GetMapping("/{traceCode}")
    public R<SfTracePublicVo> query(@PathVariable String traceCode, HttpServletRequest request) {
        return R.ok(tracePublicService.queryPublic(traceCode, request));
    }
}
