package com.ym.agriculture.farming.bigscreen.support;

import com.ym.iot.api.domain.vo.RemotePestChartVo;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 大屏虫情图表裁剪：仅保留最近一次识别记录，不输出历史折线/堆叠柱。
 */
@Component
public class BigscreenPestChartAssembler {

    /**
     * 将完整虫情图表裁为大屏快照（{@code defaultRecord} + 单条 {@code records}）。
     *
     * @param full IoT 全量虫情图表，可为 null
     * @return 裁剪结果；无识别记录时返回仅含 {@code deviceId} 的空壳
     */
    public RemotePestChartVo toLatestSnapshot(RemotePestChartVo full) {
        if (full == null) {
            return null;
        }
        RemotePestChartVo slim = new RemotePestChartVo();
        slim.setDeviceId(full.getDeviceId());
        RemotePestChartVo.PestRecord latest = resolveLatestRecord(full);
        slim.setDefaultRecord(latest);
        slim.setRecords(latest != null ? List.of(latest) : List.of());
        return slim;
    }

    private static RemotePestChartVo.PestRecord resolveLatestRecord(RemotePestChartVo full) {
        if (full.getDefaultRecord() != null) {
            return full.getDefaultRecord();
        }
        List<RemotePestChartVo.PestRecord> records = full.getRecords();
        if (records == null || records.isEmpty()) {
            return null;
        }
        return records.get(records.size() - 1);
    }
}
