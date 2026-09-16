package com.ym.agriculture.farming.crop.dao;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.agriculture.farming.crop.model.bo.SfCropSpeciesBo;
import com.ym.agriculture.farming.crop.model.entity.SfCropSpecies;
import com.ym.agriculture.farming.crop.model.vo.SfCropSpeciesVo;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.Collection;
import java.util.List;

/**
 * 作物品类数据层。
 */
public interface SfCropSpeciesMapper extends BaseMapperPlus<SfCropSpecies, SfCropSpeciesVo> {

    default LambdaQueryWrapper<SfCropSpecies> buildQueryWrapper(SfCropSpeciesBo bo) {
        return Wrappers.<SfCropSpecies>lambdaQuery()
            .eq(SfCropSpecies::getDelFlag, SystemConstants.NORMAL)
            .eq(ObjectUtil.isNotNull(bo.getSpeciesId()), SfCropSpecies::getSpeciesId, bo.getSpeciesId())
            .like(StringUtils.isNotBlank(bo.getSpeciesCode()), SfCropSpecies::getSpeciesCode, bo.getSpeciesCode())
            .like(StringUtils.isNotBlank(bo.getSpeciesName()), SfCropSpecies::getSpeciesName, bo.getSpeciesName())
            .eq(StringUtils.isNotBlank(bo.getStatus()), SfCropSpecies::getStatus, bo.getStatus())
            .orderByAsc(SfCropSpecies::getSpeciesId);
    }

    default List<SfCropSpeciesVo> selectSpeciesList(SfCropSpeciesBo bo) {
        return selectVoList(buildQueryWrapper(bo));
    }

    default Page<SfCropSpeciesVo> selectSpeciesPage(Page<SfCropSpecies> page, SfCropSpeciesBo bo) {
        return selectVoPage(page, buildQueryWrapper(bo));
    }

    default List<SfCropSpeciesVo> selectVoListByIds(Collection<Long> speciesIds) {
        if (speciesIds == null || speciesIds.isEmpty()) {
            return List.of();
        }
        return selectVoList(Wrappers.<SfCropSpecies>lambdaQuery()
            .in(SfCropSpecies::getSpeciesId, speciesIds)
            .eq(SfCropSpecies::getDelFlag, SystemConstants.NORMAL));
    }

    default List<SfCropSpeciesVo> selectVoListByCodes(String tenantId, Collection<String> speciesCodes) {
        if (StringUtils.isBlank(tenantId) || speciesCodes == null || speciesCodes.isEmpty()) {
            return List.of();
        }
        return selectVoList(Wrappers.<SfCropSpecies>lambdaQuery()
            .eq(SfCropSpecies::getTenantId, tenantId)
            .in(SfCropSpecies::getSpeciesCode, speciesCodes)
            .eq(SfCropSpecies::getStatus, SystemConstants.NORMAL)
            .eq(SfCropSpecies::getDelFlag, SystemConstants.NORMAL));
    }

    default boolean existsBySpeciesCode(String speciesCode, Long excludeSpeciesId) {
        return StringUtils.isNotBlank(speciesCode) && exists(Wrappers.<SfCropSpecies>lambdaQuery()
            .eq(SfCropSpecies::getSpeciesCode, speciesCode)
            .ne(ObjectUtil.isNotNull(excludeSpeciesId), SfCropSpecies::getSpeciesId, excludeSpeciesId)
            .eq(SfCropSpecies::getDelFlag, SystemConstants.NORMAL));
    }

    default SfCropSpecies selectTenantBySpeciesCode(String tenantId, String speciesCode) {
        return selectOne(Wrappers.<SfCropSpecies>lambdaQuery()
            .eq(SfCropSpecies::getTenantId, tenantId)
            .eq(SfCropSpecies::getSpeciesCode, speciesCode)
            .eq(SfCropSpecies::getDelFlag, SystemConstants.NORMAL)
            .last("LIMIT 1"));
    }

    default boolean updateStatus(Long speciesId, String status) {
        return update(null, Wrappers.<SfCropSpecies>lambdaUpdate()
            .eq(SfCropSpecies::getSpeciesId, speciesId)
            .eq(SfCropSpecies::getDelFlag, SystemConstants.NORMAL)
            .set(SfCropSpecies::getStatus, status)) > 0;
    }
}
