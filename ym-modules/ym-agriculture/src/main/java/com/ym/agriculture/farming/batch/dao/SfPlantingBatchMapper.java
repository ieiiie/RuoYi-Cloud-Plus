package com.ym.agriculture.farming.batch.dao;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.agriculture.farming.batch.model.bo.SfPlantingBatchBo;
import com.ym.agriculture.farming.batch.model.constants.PlantingBatchStatus;
import com.ym.agriculture.farming.batch.model.entity.SfPlantingBatch;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchVo;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 种植批次数据层。
 */
public interface SfPlantingBatchMapper extends BaseMapperPlus<SfPlantingBatch, SfPlantingBatchVo> {

    default LambdaQueryWrapper<SfPlantingBatch> buildQueryWrapper(SfPlantingBatchBo bo,
                                                                   Collection<Long> fieldIds,
                                                                   Collection<Long> varietyIds) {
        LambdaQueryWrapper<SfPlantingBatch> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SfPlantingBatch::getDelFlag, SystemConstants.NORMAL)
            .eq(ObjectUtil.isNotNull(bo.getBatchId()), SfPlantingBatch::getBatchId, bo.getBatchId())
            .like(StringUtils.isNotBlank(bo.getBatchCode()), SfPlantingBatch::getBatchCode, bo.getBatchCode())
            .eq(ObjectUtil.isNotNull(bo.getFieldId()), SfPlantingBatch::getFieldId, bo.getFieldId())
            .eq(ObjectUtil.isNotNull(bo.getVarietyId()), SfPlantingBatch::getVarietyId, bo.getVarietyId())
            .eq(StringUtils.isNotBlank(bo.getBatchStatus()), SfPlantingBatch::getBatchStatus, bo.getBatchStatus())
            .ge(bo.getSowingDateBegin() != null, SfPlantingBatch::getSowingDate, bo.getSowingDateBegin())
            .le(bo.getSowingDateEnd() != null, SfPlantingBatch::getSowingDate, bo.getSowingDateEnd());
        applyIds(wrapper, SfPlantingBatch::getFieldId, fieldIds);
        applyIds(wrapper, SfPlantingBatch::getVarietyId, varietyIds);
        return wrapper.orderByDesc(SfPlantingBatch::getBatchId);
    }

    private static <T> void applyIds(LambdaQueryWrapper<SfPlantingBatch> wrapper,
                                     com.baomidou.mybatisplus.core.toolkit.support.SFunction<SfPlantingBatch, T> column,
                                     Collection<T> ids) {
        if (ids != null) {
            if (ids.isEmpty()) {
                wrapper.apply("1=0");
            } else {
                wrapper.in(column, ids);
            }
        }
    }

    default List<SfPlantingBatchVo> selectBatchList(SfPlantingBatchBo bo,
                                                     Collection<Long> fieldIds,
                                                     Collection<Long> varietyIds) {
        return selectVoList(buildQueryWrapper(bo, fieldIds, varietyIds));
    }

    default Page<SfPlantingBatchVo> selectBatchPage(Page<SfPlantingBatch> page, SfPlantingBatchBo bo,
                                                     Collection<Long> fieldIds,
                                                     Collection<Long> varietyIds) {
        return selectVoPage(page, buildQueryWrapper(bo, fieldIds, varietyIds));
    }

    default boolean existsByBatchCode(String batchCode, Long excludeBatchId) {
        return StringUtils.isNotBlank(batchCode) && exists(Wrappers.<SfPlantingBatch>lambdaQuery()
            .eq(SfPlantingBatch::getBatchCode, batchCode)
            .ne(excludeBatchId != null, SfPlantingBatch::getBatchId, excludeBatchId)
            .eq(SfPlantingBatch::getDelFlag, SystemConstants.NORMAL));
    }

    default boolean existsActiveByFieldId(Long fieldId, Long excludeBatchId) {
        return exists(Wrappers.<SfPlantingBatch>lambdaQuery()
            .eq(SfPlantingBatch::getFieldId, fieldId)
            .in(SfPlantingBatch::getBatchStatus, PlantingBatchStatus.ACTIVE_STATUSES)
            .ne(excludeBatchId != null, SfPlantingBatch::getBatchId, excludeBatchId)
            .eq(SfPlantingBatch::getDelFlag, SystemConstants.NORMAL));
    }

    default Long selectActiveBatchIdByFieldId(Long fieldId) {
        if (fieldId == null) {
            return null;
        }
        SfPlantingBatch row = selectOne(Wrappers.<SfPlantingBatch>lambdaQuery()
            .eq(SfPlantingBatch::getFieldId, fieldId)
            .eq(SfPlantingBatch::getDelFlag, SystemConstants.NORMAL)
            .in(SfPlantingBatch::getBatchStatus, PlantingBatchStatus.ACTIVE_STATUSES)
            .orderByDesc(SfPlantingBatch::getBatchId)
            .last("LIMIT 1"));
        return row == null ? null : row.getBatchId();
    }

    default long countByFieldAndYear(Long fieldId, int year) {
        Date begin = Date.from(LocalDate.of(year, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date end = Date.from(LocalDate.of(year + 1, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        return selectCount(Wrappers.<SfPlantingBatch>lambdaQuery()
            .eq(SfPlantingBatch::getFieldId, fieldId)
            .ge(SfPlantingBatch::getSowingDate, begin)
            .lt(SfPlantingBatch::getSowingDate, end)
            .eq(SfPlantingBatch::getDelFlag, SystemConstants.NORMAL));
    }

    default List<SfPlantingBatchVo> selectCalendar(Date begin, Date end) {
        return selectVoList(Wrappers.<SfPlantingBatch>lambdaQuery()
            .eq(SfPlantingBatch::getDelFlag, SystemConstants.NORMAL)
            .and(wrapper -> wrapper.between(SfPlantingBatch::getSowingDate, begin, end)
                .or()
                .between(SfPlantingBatch::getExpectedHarvestDate, begin, end))
            .orderByAsc(SfPlantingBatch::getSowingDate)
            .orderByAsc(SfPlantingBatch::getBatchId));
    }

    default List<SfPlantingBatchVo> selectActiveByFieldIds(Collection<Long> fieldIds) {
        LambdaQueryWrapper<SfPlantingBatch> wrapper = Wrappers.<SfPlantingBatch>lambdaQuery()
            .eq(SfPlantingBatch::getDelFlag, SystemConstants.NORMAL)
            .in(SfPlantingBatch::getBatchStatus, PlantingBatchStatus.ACTIVE_STATUSES)
            .orderByDesc(SfPlantingBatch::getBatchId);
        applyIds(wrapper, SfPlantingBatch::getFieldId, fieldIds);
        return selectVoList(wrapper);
    }
}
