package com.ym.agriculture.farming.batch.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.agriculture.farming.batch.model.entity.SfPlantingBatchLog;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchLogVo;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;

/**
 * 种植批次状态流水数据层。
 */
public interface SfPlantingBatchLogMapper extends BaseMapperPlus<SfPlantingBatchLog, SfPlantingBatchLogVo> {

    default List<SfPlantingBatchLogVo> selectByBatchId(Long batchId, String tenantId) {
        return selectVoList(Wrappers.<SfPlantingBatchLog>lambdaQuery()
            .eq(SfPlantingBatchLog::getBatchId, batchId)
            .eq(SfPlantingBatchLog::getTenantId, tenantId)
            .orderByAsc(SfPlantingBatchLog::getOperateTime)
            .orderByAsc(SfPlantingBatchLog::getLogId));
    }
}
