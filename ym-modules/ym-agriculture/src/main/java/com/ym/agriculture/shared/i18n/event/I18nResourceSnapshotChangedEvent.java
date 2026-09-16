package com.ym.agriculture.shared.i18n.event;

import com.ym.agriculture.shared.i18n.model.I18nTextSource;

import java.util.List;

/**
 * 智慧农业可翻译资源完整快照变化事件。
 *
 * @param tenantId 租户编号
 * @param sources 已规范化并持久化的当前中文文本快照
 */
public record I18nResourceSnapshotChangedEvent(String tenantId, List<I18nTextSource> sources) {

    public I18nResourceSnapshotChangedEvent {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
