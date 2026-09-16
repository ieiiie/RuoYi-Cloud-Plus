package com.ym.agriculture.farming.crop.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.agriculture.farming.crop.dao.SfCropSpeciesMapper;
import com.ym.agriculture.farming.crop.dao.SfCropVarietyMapper;
import com.ym.agriculture.farming.crop.model.bo.SfCropVarietyBo;
import com.ym.agriculture.farming.crop.model.entity.SfCropSpecies;
import com.ym.agriculture.farming.crop.model.entity.SfCropVariety;
import com.ym.agriculture.farming.crop.model.vo.SfCropSpeciesVo;
import com.ym.agriculture.farming.crop.model.vo.SfCropVarietyExportVo;
import com.ym.agriculture.farming.crop.model.vo.SfCropVarietyVo;
import com.ym.agriculture.farming.crop.service.ISfCropVarietyService;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 作物品种服务实现。
 */
@Service
@RequiredArgsConstructor
public class SfCropVarietyServiceImpl implements ISfCropVarietyService {

    private final SfCropVarietyMapper varietyMapper;
    private final SfCropSpeciesMapper speciesMapper;

    @Override
    public List<SfCropVarietyVo> queryList(SfCropVarietyBo bo) {
        return enrich(varietyMapper.selectVarietyList(bo));
    }

    @Override
    public List<SfCropVarietyVo> queryListBySpeciesId(Long speciesId) {
        SfCropVarietyBo bo = new SfCropVarietyBo();
        bo.setSpeciesId(speciesId);
        return queryList(bo);
    }

    @Override
    public PageResult<SfCropVarietyVo> queryPageList(SfCropVarietyBo bo, PageQuery pageQuery) {
        Page<SfCropVarietyVo> page = varietyMapper.selectVarietyPage(pageQuery.build(), bo);
        return PageResult.build(enrich(page.getRecords()), page.getTotal());
    }

    @Override
    public SfCropVarietyVo queryById(Long varietyId) {
        SfCropVarietyVo variety = varietyMapper.selectVoById(varietyId);
        if (variety == null) {
            throw new ServiceException("作物品种不存在");
        }
        enrich(List.of(variety));
        return variety;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(SfCropVarietyBo bo) {
        normalizeAndValidate(bo);
        SfCropVariety variety = MapstructUtils.convert(bo, SfCropVariety.class);
        variety.setStatus(StringUtils.defaultIfBlank(variety.getStatus(), SystemConstants.NORMAL));
        boolean inserted = varietyMapper.insert(variety) > 0;
        if (inserted) {
            bo.setVarietyId(variety.getVarietyId());
        }
        return inserted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(SfCropVarietyBo bo) {
        if (varietyMapper.selectById(bo.getVarietyId()) == null) {
            throw new ServiceException("作物品种不存在");
        }
        normalizeAndValidate(bo);
        return varietyMapper.updateById(MapstructUtils.convert(bo, SfCropVariety.class)) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateStatus(Long varietyId, String status) {
        if (varietyMapper.selectById(varietyId) == null) {
            throw new ServiceException("作物品种不存在");
        }
        return varietyMapper.updateStatus(varietyId, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> varietyIds) {
        return varietyIds != null && !varietyIds.isEmpty() && varietyMapper.deleteBatchIds(varietyIds) > 0;
    }

    @Override
    public List<SfCropVarietyExportVo> queryExportList(SfCropVarietyBo bo) {
        return BeanUtil.copyToList(queryList(bo), SfCropVarietyExportVo.class);
    }

    private void normalizeAndValidate(SfCropVarietyBo bo) {
        bo.setVarietyCode(normalize(bo.getVarietyCode()));
        bo.setVarietyName(normalize(bo.getVarietyName()));
        SfCropSpecies species = speciesMapper.selectById(bo.getSpeciesId());
        if (species == null) {
            throw new ServiceException("所属作物品类不存在");
        }
        if (!SystemConstants.NORMAL.equals(species.getStatus())) {
            throw new ServiceException("所属作物品类已停用");
        }
        if (varietyMapper.existsByVarietyCode(bo.getVarietyCode(), bo.getVarietyId())) {
            throw new ServiceException("作物品种编码已存在");
        }
    }

    private List<SfCropVarietyVo> enrich(List<SfCropVarietyVo> varieties) {
        List<Long> speciesIds = varieties.stream().map(SfCropVarietyVo::getSpeciesId).distinct().toList();
        Map<Long, SfCropSpeciesVo> speciesMap = speciesMapper.selectVoListByIds(speciesIds).stream()
            .collect(Collectors.toMap(SfCropSpeciesVo::getSpeciesId, Function.identity()));
        varieties.forEach(variety -> {
            SfCropSpeciesVo species = speciesMap.get(variety.getSpeciesId());
            if (species != null) {
                variety.setSpeciesName(species.getSpeciesName());
                variety.setRemoteSensingCode(species.getRemoteSensingCode());
            }
        });
        return varieties;
    }

    private static String normalize(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
    }
}
