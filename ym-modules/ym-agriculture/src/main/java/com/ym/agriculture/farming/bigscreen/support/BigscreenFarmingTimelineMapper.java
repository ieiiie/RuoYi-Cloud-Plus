package com.ym.agriculture.farming.bigscreen.support;

import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.farming.bigscreen.config.BigscreenCaliber;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenTimelineItemVo;
import com.ym.agriculture.farming.farmrecord.model.vo.SfFarmingRecordFieldVo;
import com.ym.agriculture.farming.farmrecord.model.vo.SfFarmingRecordVo;
import com.ym.agriculture.farming.farmrecord.model.vo.SfFarmingRecordWorkItemVo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 农事记录 → 大屏时间轴映射（仅 sf_farming_record）。
 */
@Component
public class BigscreenFarmingTimelineMapper {

    /**
     * 批量映射农事记录为时间轴项。
     */
    public List<SfBigscreenTimelineItemVo> toTimelineItems(List<SfFarmingRecordVo> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        List<SfBigscreenTimelineItemVo> items = new ArrayList<>(records.size());
        for (SfFarmingRecordVo record : records) {
            items.add(toTimelineItem(record));
        }
        return items;
    }

    /**
     * 单条农事记录映射。
     */
    public SfBigscreenTimelineItemVo toTimelineItem(SfFarmingRecordVo record) {
        SfBigscreenTimelineItemVo item = new SfBigscreenTimelineItemVo();
        if (record == null) {
            return item;
        }
        item.setRecordId(record.getRecordId());
        item.setHappenedAt(record.getHappenedAt());
        item.setExecutor(record.getCreatorName());
        item.setDetail(record.getSummary());
        item.setSource(BigscreenCaliber.TIMELINE_SOURCE.equals("FARMING_RECORD")
            ? "farming_record" : BigscreenCaliber.TIMELINE_SOURCE);
        if (record.getWorkItems() != null && !record.getWorkItems().isEmpty()) {
            SfFarmingRecordWorkItemVo work = record.getWorkItems().get(0);
            item.setOperationType(work.getWorkItemName());
        }
        if (record.getFields() != null && !record.getFields().isEmpty()) {
            SfFarmingRecordFieldVo field = record.getFields().get(0);
            item.setFieldName(field.getFieldNameSnapshot());
        }
        return item;
    }
}
