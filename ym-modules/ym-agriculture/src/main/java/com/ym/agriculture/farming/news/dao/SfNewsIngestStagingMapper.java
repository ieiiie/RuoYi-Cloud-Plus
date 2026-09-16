package com.ym.agriculture.farming.news.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farming.news.model.entity.SfNewsIngestStaging;

import java.util.Date;
import java.util.List;

/** 农业资讯采集暂存 Mapper。 */
@InterceptorIgnore(tenantLine = "true")
public interface SfNewsIngestStagingMapper extends BaseMapperPlus<SfNewsIngestStaging, SfNewsIngestStaging> {

    /** 按接收时间领取前查询待处理记录。 */
    default List<SfNewsIngestStaging> selectPending(int limit) {
        Date staleBefore = new Date(System.currentTimeMillis() - 10 * 60 * 1000L);
        return selectList(Wrappers.<SfNewsIngestStaging>lambdaQuery()
            .and(w -> w.eq(SfNewsIngestStaging::getIngestStatus, "PENDING")
                .or(stale -> stale.eq(SfNewsIngestStaging::getIngestStatus, "PROCESSING")
                    .lt(SfNewsIngestStaging::getProcessingStartedAt, staleBefore)))
            .orderByAsc(SfNewsIngestStaging::getReceivedAt)
            .last("limit " + Math.max(1, Math.min(limit, 100))));
    }

    /** 条件领取一条记录，避免多个任务重复处理。 */
    default boolean claim(Long id, Date now) {
        Date staleBefore = new Date(now.getTime() - 10 * 60 * 1000L);
        return update(null, Wrappers.<SfNewsIngestStaging>lambdaUpdate()
            .eq(SfNewsIngestStaging::getId, id)
            .and(w -> w.eq(SfNewsIngestStaging::getIngestStatus, "PENDING")
                .or(stale -> stale.eq(SfNewsIngestStaging::getIngestStatus, "PROCESSING")
                    .lt(SfNewsIngestStaging::getProcessingStartedAt, staleBefore)))
            .set(SfNewsIngestStaging::getIngestStatus, "PROCESSING")
            .set(SfNewsIngestStaging::getProcessingStartedAt, now)
            .setSql("process_attempts = process_attempts + 1")) > 0;
    }
}
