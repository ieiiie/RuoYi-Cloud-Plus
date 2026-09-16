package com.ym.agriculture.shared.i18n.event;

import cn.hutool.core.collection.CollUtil;
import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.shared.i18n.service.ISfI18nTextService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 在业务事件发布线程内登记翻译资源，使同库业务数据与翻译待办共同提交或回滚。
 */
@Component
@RequiredArgsConstructor
public class SfI18nResourceSnapshotListener {

    private final ISfI18nTextService i18nTextService;

    /**
     * 登记资源的当前中文快照。
     *
     * @param event 资源快照变化事件
     */
    @EventListener
    public void onResourceSnapshotChanged(I18nResourceSnapshotChangedEvent event) {
        if (event == null || StringUtils.isBlank(event.tenantId()) || CollUtil.isEmpty(event.sources())) {
            return;
        }
        i18nTextService.registerTexts(event.tenantId(), event.sources());
    }
}
