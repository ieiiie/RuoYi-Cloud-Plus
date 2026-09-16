package com.ym.agriculture.farming.algback.controller;

import com.ym.agriculture.farming.integration.ai.algback.autoconfigure.YmAlgBackProperties;
import com.ym.common.core.domain.R;
import com.ym.common.web.core.BaseController;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 算法中台对接只读配置；用于前端或运维展示与中台约定一致的回调基址等。
 *
 * @author ym-cloud
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/smart-farming/ai/alg-back/config")
public class SfAlgBackConfigController extends BaseController {

    private final YmAlgBackProperties algBackProperties;

    /**
     * 推理结果回调完整 URL（配置项 {@code ym.alg-back.callback-url}），供中台客户 {@code httpReqUrl} 填写参考
     *
     * @return 统一响应，{@code data} 为回调 URL 字符串，未配置时为空串
     */
    @GetMapping("/inference-callback-url")
    public R<String> inferenceCallbackUrl() {
        String url = algBackProperties.getCallbackUrl();
        return R.ok(url != null ? url : "");
    }
}
