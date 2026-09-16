package com.ym.agriculture.farming.tenantinit.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.farming.crop.dao.SfCropSpeciesMapper;
import com.ym.agriculture.farming.crop.dao.SfCropVarietyMapper;
import com.ym.agriculture.farming.crop.model.entity.SfCropSpecies;
import com.ym.agriculture.farming.crop.model.entity.SfCropVariety;
import com.ym.agriculture.farming.farmwork.dao.SfFarmWorkDictMapper;
import com.ym.agriculture.farming.farmwork.model.constants.FarmWorkNodeType;
import com.ym.agriculture.farming.farmwork.model.entity.SfFarmWorkDict;
import com.ym.agriculture.shared.i18n.model.I18nTextSource;
import com.ym.agriculture.shared.i18n.service.ISfI18nTextService;
import com.ym.agriculture.farming.i18n.support.FarmWorkI18nSourceFactory;
import com.ym.agriculture.farming.i18n.support.SmartFarmingI18nSourceFactory;
import com.ym.agriculture.farming.tenantinit.dao.SfCropSpeciesTemplateMapper;
import com.ym.agriculture.farming.tenantinit.dao.SfCropVarietyTemplateMapper;
import com.ym.agriculture.farming.tenantinit.dao.SfFarmWorkDictTemplateMapper;
import com.ym.agriculture.farming.tenantinit.model.entity.SfCropSpeciesTemplate;
import com.ym.agriculture.farming.tenantinit.model.entity.SfCropVarietyTemplate;
import com.ym.agriculture.farming.tenantinit.model.entity.SfFarmWorkDictTemplate;
import com.ym.agriculture.farming.tenantinit.service.ISfTenantInitializationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SfTenantInitializationServiceImpl implements ISfTenantInitializationService {

    private final SfCropSpeciesTemplateMapper speciesTemplateMapper;
    private final SfCropVarietyTemplateMapper varietyTemplateMapper;
    private final SfFarmWorkDictTemplateMapper farmWorkTemplateMapper;
    private final SfCropSpeciesMapper speciesMapper;
    private final SfCropVarietyMapper varietyMapper;
    private final SfFarmWorkDictMapper farmWorkDictMapper;
    private final ISfI18nTextService i18nTextService;

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void initializeTenant(String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
            throw new ServiceException("租户编号不能为空");
        }
        Map<String, SfCropSpecies> speciesByCode = copySpecies(tenantId);
        copyVarieties(tenantId, speciesByCode);
        copyFarmWorkDict(tenantId);
    }

    private Map<String, SfCropSpecies> copySpecies(String tenantId) {
        Map<String, SfCropSpecies> speciesByCode = new HashMap<>();
        for (SfCropSpeciesTemplate template : speciesTemplateMapper.selectEnabledTemplates()) {
            if (StringUtils.isBlank(template.getSpeciesCode())) {
                continue;
            }
            SfCropSpecies species = speciesMapper.selectTenantBySpeciesCode(tenantId, template.getSpeciesCode());
            if (species == null) {
                species = toSpecies(tenantId, template);
                speciesMapper.insert(species);
                SfCropSpecies inserted = speciesMapper.selectTenantBySpeciesCode(tenantId, template.getSpeciesCode());
                species = inserted == null ? species : inserted;
            }
            registerSmartFarmingTranslations(tenantId, SmartFarmingI18nSourceFactory.sources(species));
            speciesByCode.put(template.getSpeciesCode(), species);
        }
        return speciesByCode;
    }

    private void copyVarieties(String tenantId, Map<String, SfCropSpecies> speciesByCode) {
        for (SfCropVarietyTemplate template : varietyTemplateMapper.selectEnabledTemplates()) {
            if (StringUtils.isBlank(template.getVarietyCode())) {
                continue;
            }
            SfCropVariety variety = varietyMapper.selectTenantByVarietyCode(tenantId, template.getVarietyCode());
            if (variety == null) {
                SfCropSpecies species = speciesByCode.get(template.getSpeciesCode());
                if (species == null || species.getSpeciesId() == null) {
                    throw new ServiceException("作物品种模板未找到对应物种: " + template.getSpeciesCode());
                }
                variety = toVariety(tenantId, species.getSpeciesId(), template);
                varietyMapper.insert(variety);
            }
            registerSmartFarmingTranslations(tenantId, SmartFarmingI18nSourceFactory.sources(variety));
        }
    }

    private void copyFarmWorkDict(String tenantId) {
        List<SfFarmWorkDictTemplate> templates = farmWorkTemplateMapper.selectEnabledTemplates();
        Map<String, SfFarmWorkDict> dictByTemplateCode = new HashMap<>();

        for (SfFarmWorkDictTemplate template : templates) {
            if (!FarmWorkNodeType.CATEGORY.name().equals(template.getNodeType())) {
                continue;
            }
            SfFarmWorkDict dict = ensureFarmWorkDict(tenantId, 0L, template);
            registerFarmWorkTranslations(tenantId, dict);
            dictByTemplateCode.put(template.getTemplateCode(), dict);
        }

        for (SfFarmWorkDictTemplate template : templates) {
            if (!FarmWorkNodeType.ITEM.name().equals(template.getNodeType())) {
                continue;
            }
            SfFarmWorkDict parent = dictByTemplateCode.get(template.getParentTemplateCode());
            if (parent == null || parent.getDictId() == null) {
                throw new ServiceException("农事项目模板未找到对应分类: " + template.getParentTemplateCode());
            }
            SfFarmWorkDict dict = ensureFarmWorkDict(tenantId, parent.getDictId(), template);
            registerFarmWorkTranslations(tenantId, dict);
        }
    }

    private SfFarmWorkDict ensureFarmWorkDict(String tenantId, Long parentId, SfFarmWorkDictTemplate template) {
        SfFarmWorkDict dict = farmWorkDictMapper.selectTenantByDictCode(tenantId, template.getDictCode());
        if (dict != null) {
            return dict;
        }
        dict = toFarmWorkDict(tenantId, parentId, template);
        farmWorkDictMapper.insert(dict);
        SfFarmWorkDict inserted = farmWorkDictMapper.selectTenantByDictCode(tenantId, template.getDictCode());
        return inserted == null ? dict : inserted;
    }

    private void registerFarmWorkTranslations(String tenantId, SfFarmWorkDict dict) {
        i18nTextService.registerTexts(tenantId, FarmWorkI18nSourceFactory.sources(dict));
    }

    private void registerSmartFarmingTranslations(String tenantId, List<I18nTextSource> sources) {
        i18nTextService.registerTexts(tenantId, sources);
    }

    private SfCropSpecies toSpecies(String tenantId, SfCropSpeciesTemplate template) {
        SfCropSpecies species = new SfCropSpecies();
        species.setSpeciesId(IdWorker.getId());
        species.setTenantId(tenantId);
        species.setSpeciesCode(template.getSpeciesCode());
        species.setSpeciesName(template.getSpeciesName());
        species.setRemoteSensingCode(template.getRemoteSensingCode());
        species.setStatus(SystemConstants.NORMAL);
        species.setDelFlag(SystemConstants.NORMAL);
        species.setMapIconUrl(template.getMapIconUrl());
        species.setMapIconEmoji(template.getMapIconEmoji());
        species.setGrowthStageConfigJson(template.getGrowthStageConfigJson());
        species.setRemark(template.getRemark());
        return species;
    }

    private SfCropVariety toVariety(String tenantId, Long speciesId, SfCropVarietyTemplate template) {
        SfCropVariety variety = new SfCropVariety();
        variety.setVarietyId(IdWorker.getId());
        variety.setTenantId(tenantId);
        variety.setSpeciesId(speciesId);
        variety.setVarietyCode(template.getVarietyCode());
        variety.setVarietyName(template.getVarietyName());
        variety.setGrowthCycleDays(template.getGrowthCycleDays());
        variety.setStatus(SystemConstants.NORMAL);
        variety.setDelFlag(SystemConstants.NORMAL);
        variety.setMapIconUrl(template.getMapIconUrl());
        variety.setMapIconEmoji(template.getMapIconEmoji());
        variety.setRemark(template.getRemark());
        return variety;
    }

    private SfFarmWorkDict toFarmWorkDict(String tenantId, Long parentId, SfFarmWorkDictTemplate template) {
        SfFarmWorkDict dict = new SfFarmWorkDict();
        dict.setDictId(IdWorker.getId());
        dict.setTenantId(tenantId);
        dict.setParentId(parentId);
        dict.setNodeType(template.getNodeType());
        dict.setDictCode(template.getDictCode());
        dict.setDictName(template.getDictName());
        dict.setMinWorkers(template.getMinWorkers());
        dict.setMaxWorkers(template.getMaxWorkers());
        dict.setRequiresMaterial("ITEM".equals(template.getNodeType())
            && Boolean.TRUE.equals(template.getRequiresMaterial()));
        dict.setStatus(SystemConstants.NORMAL);
        dict.setCustomFormTemplateJson(template.getCustomFormTemplateJson());
        dict.setSortOrder(template.getSortOrder());
        dict.setDelFlag(SystemConstants.NORMAL);
        dict.setRemark(template.getRemark());
        return dict;
    }
}
