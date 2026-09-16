package com.ym.agriculture.farming.trace.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farming.trace.model.bo.SfTraceBatchBo;
import com.ym.agriculture.farming.trace.model.entity.SfTraceBatch;
import com.ym.agriculture.farming.trace.model.vo.SfTraceBatchVo;

/**
 * 溯源批次 Mapper。
 */
public interface SfTraceBatchMapper extends BaseMapperPlus<SfTraceBatch, SfTraceBatchVo> {

    default LambdaQueryWrapper<SfTraceBatch> buildQueryWrapper(SfTraceBatchBo bo) {
        var w = Wrappers.<SfTraceBatch>lambdaQuery()
                .eq(SfTraceBatch::getDelFlag, SystemConstants.NORMAL);
        if (bo == null) {
            return w;
        }
        w.eq(bo.getTraceBatchId() != null, SfTraceBatch::getTraceBatchId, bo.getTraceBatchId());
        w.eq(bo.getPlantingBatchId() != null, SfTraceBatch::getPlantingBatchId, bo.getPlantingBatchId());
        w.eq(bo.getFieldId() != null, SfTraceBatch::getFieldId, bo.getFieldId());
        w.eq(bo.getVarietyId() != null, SfTraceBatch::getVarietyId, bo.getVarietyId());
        w.eq(StringUtils.isNotBlank(bo.getTraceBatchNo()), SfTraceBatch::getTraceBatchNo, bo.getTraceBatchNo());
        w.like(StringUtils.isNotBlank(bo.getProductNameLike()), SfTraceBatch::getProductName, bo.getProductNameLike());
        w.like(StringUtils.isNotBlank(bo.getProductName()) && StringUtils.isBlank(bo.getProductNameLike()),
                SfTraceBatch::getProductName, bo.getProductName());
        w.eq(StringUtils.isNotBlank(bo.getStatus()), SfTraceBatch::getStatus, bo.getStatus());
        w.orderByDesc(SfTraceBatch::getCreateTime);
        return w;
    }

    default SfTraceBatch selectByTenantAndNo(String tenantId, String traceBatchNo) {
        return selectOne(Wrappers.<SfTraceBatch>lambdaQuery()
                .eq(SfTraceBatch::getTenantId, tenantId)
                .eq(SfTraceBatch::getTraceBatchNo, traceBatchNo)
                .eq(SfTraceBatch::getDelFlag, SystemConstants.NORMAL));
    }

    default SfTraceBatch selectByIdForUpdate(Long traceBatchId, String tenantId) {
        return selectOne(Wrappers.<SfTraceBatch>lambdaQuery()
                .eq(SfTraceBatch::getTraceBatchId, traceBatchId)
                .eq(SfTraceBatch::getTenantId, tenantId)
                .eq(SfTraceBatch::getDelFlag, SystemConstants.NORMAL)
                .last("FOR UPDATE"));
    }
}
