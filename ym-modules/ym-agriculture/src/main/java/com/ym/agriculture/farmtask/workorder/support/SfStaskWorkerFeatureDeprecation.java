package com.ym.agriculture.farmtask.workorder.support;

import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import com.ym.agriculture.shared.i18n.StaskMessageResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 工人派工功能废弃守卫。
 *
 * <p>旧接口仍保持注册以兼容调用方的 HTTP 契约，但不允许再读取或修改工人派工数据。</p>
 */
@Component
@RequiredArgsConstructor
public class SfStaskWorkerFeatureDeprecation {

    private final StaskMessageResolver messages;

    /**
     * 拒绝调用已废弃的工人派工功能。
     */
    public void reject() {
        throw messages.exception(StaskMessageKeys.ERROR_DISPATCH_FEATURE_DEPRECATED);
    }
}
