package com.ym.agriculture.farming.crop.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.agriculture.farming.crop.dao.SfCropSpeciesMapper;
import com.ym.agriculture.farming.crop.dao.SfCropVarietyMapper;
import com.ym.agriculture.farming.crop.model.bo.SfCropSpeciesBo;
import com.ym.agriculture.farming.crop.model.entity.SfCropSpecies;
import com.ym.agriculture.farming.crop.model.vo.SfCropSpeciesDetailVo;
import com.ym.agriculture.farming.crop.model.vo.SfCropSpeciesExportVo;
import com.ym.agriculture.farming.crop.model.vo.SfCropSpeciesTreeVo;
import com.ym.agriculture.farming.crop.model.vo.SfCropSpeciesVo;
import com.ym.agriculture.farming.crop.model.vo.SfCropVarietyVo;
import com.ym.agriculture.farming.crop.service.ISfCropSpeciesService;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.json.utils.JsonUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 作物品类服务实现。
 */
@Service
@RequiredArgsConstructor
public class SfCropSpeciesServiceImpl implements ISfCropSpeciesService {

    private final SfCropSpeciesMapper speciesMapper;
    private final SfCropVarietyMapper varietyMapper;

    @Override
    public List<SfCropSpeciesVo> queryList(SfCropSpeciesBo bo) {
        return enrich(speciesMapper.selectSpeciesList(bo));
    }

    @Override
    public List<SfCropSpeciesTreeVo> queryTree(SfCropSpeciesBo bo, String varietyStatus) {
        List<SfCropSpeciesVo> speciesList = queryList(bo);
        if (speciesList.isEmpty()) {
            return List.of();
        }
        List<Long> speciesIds = speciesList.stream().map(SfCropSpeciesVo::getSpeciesId).toList();
        Map<Long, List<SfCropVarietyVo>> varieties = varietyMapper.selectVoListBySpeciesIds(speciesIds).stream()
            .filter(item -> StringUtils.isBlank(varietyStatus) || varietyStatus.equals(item.getStatus()))
            .collect(Collectors.groupingBy(SfCropVarietyVo::getSpeciesId));
        return speciesList.stream().map(species -> {
            SfCropSpeciesTreeVo node = BeanUtil.copyProperties(species, SfCropSpeciesTreeVo.class);
            List<SfCropVarietyVo> children = varieties.getOrDefault(species.getSpeciesId(), List.of());
            children.forEach(item -> fillSpecies(item, species));
            node.setChildren(new ArrayList<>(children));
            return node;
        }).toList();
    }

    @Override
    public PageResult<SfCropSpeciesVo> queryPageList(SfCropSpeciesBo bo, PageQuery pageQuery) {
        Page<SfCropSpeciesVo> page = speciesMapper.selectSpeciesPage(pageQuery.build(), bo);
        return PageResult.build(enrich(page.getRecords()), page.getTotal());
    }

    @Override
    public SfCropSpeciesVo queryById(Long speciesId) {
        SfCropSpeciesVo species = speciesMapper.selectVoById(speciesId);
        if (species == null) {
            throw new ServiceException("作物品类不存在");
        }
        return enrich(species);
    }

    @Override
    public SfCropSpeciesDetailVo queryDetailById(Long speciesId) {
        SfCropSpeciesVo species = queryById(speciesId);
        SfCropSpeciesDetailVo detail = BeanUtil.copyProperties(species, SfCropSpeciesDetailVo.class);
        List<SfCropVarietyVo> varieties = varietyMapper.selectVoListBySpeciesIds(List.of(speciesId));
        varieties.forEach(item -> fillSpecies(item, species));
        detail.setVarietyList(varieties);
        detail.setVarietyCount((long) varieties.size());
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(SfCropSpeciesBo bo) {
        normalizeAndValidate(bo);
        SfCropSpecies species = MapstructUtils.convert(bo, SfCropSpecies.class);
        species.setStatus(StringUtils.defaultIfBlank(species.getStatus(), SystemConstants.NORMAL));
        species.setGrowthStageConfigJson(toJson(bo.getGrowthStageConfig()));
        boolean inserted = speciesMapper.insert(species) > 0;
        if (inserted) {
            bo.setSpeciesId(species.getSpeciesId());
        }
        return inserted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(SfCropSpeciesBo bo) {
        SfCropSpecies existing = speciesMapper.selectById(bo.getSpeciesId());
        if (existing == null) {
            throw new ServiceException("作物品类不存在");
        }
        normalizeAndValidate(bo);
        SfCropSpecies species = MapstructUtils.convert(bo, SfCropSpecies.class);
        if (bo.getGrowthStageConfig() != null) {
            species.setGrowthStageConfigJson(toJson(bo.getGrowthStageConfig()));
        }
        return speciesMapper.updateById(species) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateStatus(Long speciesId, String status) {
        if (speciesMapper.selectById(speciesId) == null) {
            throw new ServiceException("作物品类不存在");
        }
        if ("1".equals(status) && varietyMapper.countNormalBySpeciesId(speciesId) > 0) {
            throw new ServiceException("该品类下仍有正常状态的品种，无法停用");
        }
        return speciesMapper.updateStatus(speciesId, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> speciesIds) {
        if (speciesIds == null || speciesIds.isEmpty()) {
            return false;
        }
        boolean hasVariety = speciesIds.stream().anyMatch(id -> varietyMapper.countBySpeciesId(id) > 0);
        if (hasVariety) {
            throw new ServiceException("品类下仍存在品种，无法删除");
        }
        return speciesMapper.deleteBatchIds(speciesIds) > 0;
    }

    @Override
    public List<SfCropSpeciesExportVo> queryExportList(SfCropSpeciesBo bo) {
        return BeanUtil.copyToList(queryList(bo), SfCropSpeciesExportVo.class);
    }

    private void normalizeAndValidate(SfCropSpeciesBo bo) {
        bo.setSpeciesCode(normalize(bo.getSpeciesCode()));
        bo.setSpeciesName(normalize(bo.getSpeciesName()));
        if (speciesMapper.existsBySpeciesCode(bo.getSpeciesCode(), bo.getSpeciesId())) {
            throw new ServiceException("作物品类编码已存在");
        }
    }

    private List<SfCropSpeciesVo> enrich(List<SfCropSpeciesVo> speciesList) {
        speciesList.forEach(this::enrich);
        return speciesList;
    }

    private SfCropSpeciesVo enrich(SfCropSpeciesVo species) {
        if (StringUtils.isNotBlank(species.getGrowthStageConfigJson())) {
            species.setGrowthStageConfig(JsonUtils.parseObject(species.getGrowthStageConfigJson(),
                new TypeReference<Map<String, Object>>() {
                }));
        }
        return species;
    }

    private static void fillSpecies(SfCropVarietyVo variety, SfCropSpeciesVo species) {
        variety.setSpeciesName(species.getSpeciesName());
        variety.setRemoteSensingCode(species.getRemoteSensingCode());
    }

    private static String normalize(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
    }

    private static String toJson(Map<String, Object> value) {
        return value == null || value.isEmpty() ? null : JsonUtils.toJsonString(value);
    }
}
