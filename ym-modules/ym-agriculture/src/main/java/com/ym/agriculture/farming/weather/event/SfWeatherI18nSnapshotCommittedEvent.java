package com.ym.agriculture.farming.weather.event;

import com.ym.agriculture.shared.i18n.model.I18nTextSource;

import java.util.List;

/** 天气中文快照已进入提交阶段，供提交后按 adcode 为关联租户登记翻译词条。 */
public record SfWeatherI18nSnapshotCommittedEvent(String adcode, List<I18nTextSource> sources) {

    public SfWeatherI18nSnapshotCommittedEvent {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
