package com.ym.agriculture.farming.trace.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farming.trace.model.bo.SfTraceCodeBo;
import com.ym.agriculture.farming.trace.model.constants.TraceCodeStatus;
import com.ym.agriculture.farming.trace.model.entity.SfTraceCode;
import com.ym.agriculture.farming.trace.model.vo.SfTraceCodeVo;

/**
 * 溯源码 Mapper。
 */
public interface SfTraceCodeMapper extends BaseMapperPlus<SfTraceCode, SfTraceCodeVo> {

    default LambdaQueryWrapper<SfTraceCode> buildQueryWrapper(SfTraceCodeBo bo) {
        var w = Wrappers.<SfTraceCode>lambdaQuery();
        if (bo == null) {
            return w.orderByDesc(SfTraceCode::getCreateTime);
        }
        w.eq(bo.getTraceBatchId() != null, SfTraceCode::getTraceBatchId, bo.getTraceBatchId());
        w.like(StringUtils.isNotBlank(bo.getTraceCode()), SfTraceCode::getTraceCode, bo.getTraceCode());
        w.eq(StringUtils.isNotBlank(bo.getStatus()), SfTraceCode::getStatus, bo.getStatus());
        return w.orderByDesc(SfTraceCode::getSeqNo);
    }

    default SfTraceCode selectByTraceCode(String traceCode) {
        return selectOne(Wrappers.<SfTraceCode>lambdaQuery()
            .eq(SfTraceCode::getTraceCode, traceCode));
    }

    default int maxSeqNo(Long traceBatchId) {
        SfTraceCode row = selectOne(Wrappers.<SfTraceCode>lambdaQuery()
            .eq(SfTraceCode::getTraceBatchId, traceBatchId)
            .orderByDesc(SfTraceCode::getSeqNo)
            .last("LIMIT 1"));
        return row == null || row.getSeqNo() == null ? 0 : row.getSeqNo();
    }

    default long countByBatchAndStatus(Long traceBatchId, String status) {
        return selectCount(Wrappers.<SfTraceCode>lambdaQuery()
            .eq(SfTraceCode::getTraceBatchId, traceBatchId)
            .eq(StringUtils.isNotBlank(status), SfTraceCode::getStatus, status));
    }

    default long sumScanCount(Long traceBatchId) {
        return selectList(Wrappers.<SfTraceCode>lambdaQuery()
            .eq(SfTraceCode::getTraceBatchId, traceBatchId)
            .select(SfTraceCode::getScanCount))
            .stream()
            .mapToLong(c -> c.getScanCount() == null ? 0 : c.getScanCount())
            .sum();
    }

    default long countFirstScanned(Long traceBatchId) {
        return selectCount(Wrappers.<SfTraceCode>lambdaQuery()
            .eq(SfTraceCode::getTraceBatchId, traceBatchId)
            .isNotNull(SfTraceCode::getFirstScanTime));
    }

    default long countRepeatScanned(Long traceBatchId) {
        return selectCount(Wrappers.<SfTraceCode>lambdaQuery()
            .eq(SfTraceCode::getTraceBatchId, traceBatchId)
            .gt(SfTraceCode::getScanCount, 1));
    }

    default java.util.List<SfTraceCode> selectNormalByBatch(Long traceBatchId) {
        return selectList(Wrappers.<SfTraceCode>lambdaQuery()
            .eq(SfTraceCode::getTraceBatchId, traceBatchId)
            .eq(SfTraceCode::getStatus, TraceCodeStatus.NORMAL)
            .orderByAsc(SfTraceCode::getSeqNo));
    }
}
