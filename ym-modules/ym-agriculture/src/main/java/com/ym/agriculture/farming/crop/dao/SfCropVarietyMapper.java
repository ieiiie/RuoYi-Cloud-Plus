package com.ym.agriculture.farming.crop.dao;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.agriculture.farming.crop.model.bo.SfCropVarietyBo;
import com.ym.agriculture.farming.crop.model.entity.SfCropVariety;
import com.ym.agriculture.farming.crop.model.vo.SfCropVarietyVo;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.Collection;
import java.util.List;

/**
 * 作物品种数据层。
 */
public interface SfCropVarietyMapper extends BaseMapperPlus<SfCropVariety, SfCropVarietyVo> {

    default LambdaQueryWrapper<SfCropVariety> buildQueryWrapper(SfCropVarietyBo bo) {
        return Wrappers.<SfCropVariety>lambdaQuery()
            .eq(SfCropVariety::getDelFlag, SystemConstants.NORMAL)
            .eq(ObjectUtil.isNotNull(bo.getVarietyId()), SfCropVariety::getVarietyId, bo.getVarietyId())
            .eq(ObjectUtil.isNotNull(bo.getSpeciesId()), SfCropVariety::getSpeciesId, bo.getSpeciesId())
            .like(StringUtils.isNotBlank(bo.getVarietyCode()), SfCropVariety::getVarietyCode, bo.getVarietyCode())
            .like(StringUtils.isNotBlank(bo.getVarietyName()), SfCropVariety::getVarietyName, bo.getVarietyName())
            .eq(StringUtils.isNotBlank(bo.getStatus()), SfCropVariety::getStatus, bo.getStatus())
            .orderByAsc(SfCropVariety::getVarietyId);
    }

    default List<SfCropVarietyVo> selectVarietyList(SfCropVarietyBo bo) {
        return selectVoList(buildQueryWrapper(bo));
    }

    default Page<SfCropVarietyVo> selectVarietyPage(Page<SfCropVariety> page, SfCropVarietyBo bo) {
        return selectVoPage(page, buildQueryWrapper(bo));
    }

    default List<SfCropVarietyVo> selectVoListBySpeciesIds(Collection<Long> speciesIds) {
        if (speciesIds == null || speciesIds.isEmpty()) {
            return List.of();
        }
        return selectVoList(Wrappers.<SfCropVariety>lambdaQuery()
            .in(SfCropVariety::getSpeciesId, speciesIds)
            .eq(SfCropVariety::getDelFlag, SystemConstants.NORMAL)
            .orderByAsc(SfCropVariety::getVarietyId));
    }

    default boolean existsByVarietyCode(String varietyCode, Long excludeVarietyId) {
        return StringUtils.isNotBlank(varietyCode) && exists(Wrappers.<SfCropVariety>lambdaQuery()
            .eq(SfCropVariety::getVarietyCode, varietyCode)
            .ne(ObjectUtil.isNotNull(excludeVarietyId), SfCropVariety::getVarietyId, excludeVarietyId)
            .eq(SfCropVariety::getDelFlag, SystemConstants.NORMAL));
    }

    default SfCropVariety selectTenantByVarietyCode(String tenantId, String varietyCode) {
        return selectOne(Wrappers.<SfCropVariety>lambdaQuery()
            .eq(SfCropVariety::getTenantId, tenantId)
            .eq(SfCropVariety::getVarietyCode, varietyCode)
            .eq(SfCropVariety::getDelFlag, SystemConstants.NORMAL)
            .last("LIMIT 1"));
    }

    default long countBySpeciesId(Long speciesId) {
        return selectCount(Wrappers.<SfCropVariety>lambdaQuery()
            .eq(SfCropVariety::getSpeciesId, speciesId)
            .eq(SfCropVariety::getDelFlag, SystemConstants.NORMAL));
    }

    default long countNormalBySpeciesId(Long speciesId) {
        return selectCount(Wrappers.<SfCropVariety>lambdaQuery()
            .eq(SfCropVariety::getSpeciesId, speciesId)
            .eq(SfCropVariety::getStatus, SystemConstants.NORMAL)
            .eq(SfCropVariety::getDelFlag, SystemConstants.NORMAL));
    }

    default boolean updateStatus(Long varietyId, String status) {
        return update(null, Wrappers.<SfCropVariety>lambdaUpdate()
            .eq(SfCropVariety::getVarietyId, varietyId)
            .eq(SfCropVariety::getDelFlag, SystemConstants.NORMAL)
            .set(SfCropVariety::getStatus, status)) > 0;
    }
}
