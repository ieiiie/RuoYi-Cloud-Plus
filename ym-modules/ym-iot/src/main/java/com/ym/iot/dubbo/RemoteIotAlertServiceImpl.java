package com.ym.iot.dubbo;

import cn.hutool.core.bean.BeanUtil;

import com.ym.common.core.domain.PageResult;
import com.ym.iot.alarm.service.INativeAlarmService;
import com.ym.iot.api.RemoteIotAlertService;
import com.ym.iot.api.domain.bo.RemoteAlertQueryBo;
import com.ym.iot.api.domain.vo.RemoteAlertRecordVo;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;

import lombok.RequiredArgsConstructor;

import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/** Internal business readers use native JetLinks alarms under the caller's device scope. */
@Service
@ConditionalOnJetLinks
@DubboService(version = "2.0.0", retries = 0)
@RequiredArgsConstructor
public class RemoteIotAlertServiceImpl implements RemoteIotAlertService {
    private final INativeAlarmService alarms;

    @Override
    public PageResult<RemoteAlertRecordVo> pageRecords(RemoteAlertQueryBo query) {
        Map<String, String> filters = new LinkedHashMap<>();
        if (query != null)
            BeanUtil.beanToMap(query)
                    .forEach(
                            (key, value) -> {
                                if (value != null) filters.put(key, value.toString());
                            });
        var page = alarms.records(filters);
        return PageResult.build(
                page.getRows().stream()
                        .map(row -> BeanUtil.toBean(row, RemoteAlertRecordVo.class))
                        .toList(),
                page.getTotal());
    }

    @Override
    public long countAlarming() {
        return alarms.warningCount();
    }
}
