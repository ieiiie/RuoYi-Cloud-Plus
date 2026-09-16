package com.ym.agriculture.farming.news.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farming.news.model.entity.SfNewsMediaTransfer;

import java.util.Date;
import java.util.List;

@InterceptorIgnore(tenantLine = "true")
public interface SfNewsMediaTransferMapper extends BaseMapperPlus<SfNewsMediaTransfer, SfNewsMediaTransfer> {
    default List<SfNewsMediaTransfer> selectRunnable(int limit, Date now, Date staleBefore) {
        return selectList(Wrappers.<SfNewsMediaTransfer>lambdaQuery()
            .and(w -> w.and(p -> p.eq(SfNewsMediaTransfer::getTransferStatus, "PENDING")
                    .and(n -> n.isNull(SfNewsMediaTransfer::getNextRetryAt)
                        .or().le(SfNewsMediaTransfer::getNextRetryAt, now)))
                .or(p -> p.eq(SfNewsMediaTransfer::getTransferStatus, "PROCESSING")
                    .lt(SfNewsMediaTransfer::getProcessingStartedAt, staleBefore)))
            .orderByAsc(SfNewsMediaTransfer::getCreateTime)
            .last("limit " + Math.max(1, Math.min(limit, 100))));
    }

    default boolean claim(Long taskId, Date now, Date staleBefore) {
        return update(null, Wrappers.<SfNewsMediaTransfer>lambdaUpdate()
            .eq(SfNewsMediaTransfer::getTaskId, taskId)
            .and(w -> w.eq(SfNewsMediaTransfer::getTransferStatus, "PENDING")
                .or(p -> p.eq(SfNewsMediaTransfer::getTransferStatus, "PROCESSING")
                    .lt(SfNewsMediaTransfer::getProcessingStartedAt, staleBefore)))
            .set(SfNewsMediaTransfer::getTransferStatus, "PROCESSING")
            .set(SfNewsMediaTransfer::getProcessingStartedAt, now)
            .setSql("attempt_count = attempt_count + 1")) > 0;
    }
}
