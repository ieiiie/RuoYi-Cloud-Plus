package com.ym.system.saas.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.mybatis.core.query.QueryBuilder;
import com.ym.common.mybatis.utils.IdGeneratorUtil;
import com.ym.system.saas.domain.SaasTenantDictDefaultData;
import com.ym.system.saas.domain.SaasTenantDictType;
import com.ym.system.saas.domain.bo.SaasTenantDictDefaultDataBo;
import com.ym.system.saas.domain.vo.SaasTenantDictDefaultDataVo;
import com.ym.system.saas.mapper.SaasTenantDictDefaultDataMapper;
import com.ym.system.saas.mapper.SaasTenantDictTypeMapper;
import com.ym.system.saas.service.ISaasTenantDictDefaultDataService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

/** Dbo 维护的新租户字典默认值。 */
@Service
@DS("saas")
@RequiredArgsConstructor
public class SaasTenantDictDefaultDataServiceImpl implements ISaasTenantDictDefaultDataService {
    private final SaasTenantDictDefaultDataMapper defaultDataMapper;
    private final SaasTenantDictTypeMapper typeMapper;

    @Override
    public PageResult<SaasTenantDictDefaultDataVo> queryPage(SaasTenantDictDefaultDataBo bo, PageQuery pageQuery) {
        Page<SaasTenantDictDefaultDataVo> page = defaultDataMapper.selectVoPage(pageQuery.build(),
            QueryBuilder.lambda(SaasTenantDictDefaultData.class)
                .eqIfText(SaasTenantDictDefaultData::getDictType, bo.getDictType())
                .likeIfText(SaasTenantDictDefaultData::getDictLabel, bo.getDictLabel())
                .likeIfText(SaasTenantDictDefaultData::getDictValue, bo.getDictValue())
                .orderByAsc(SaasTenantDictDefaultData::getDictType)
                .orderByAsc(SaasTenantDictDefaultData::getDictSort)
                .orderByAsc(SaasTenantDictDefaultData::getDictCode).build());
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    @Override
    public SaasTenantDictDefaultDataVo queryById(Long id) {
        return defaultDataMapper.selectVoById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insert(SaasTenantDictDefaultDataBo bo) {
        String dictType = requireType(bo.getDictType()).getDictType();
        String dictLabel = bo.getDictLabel().trim();
        String dictValue = bo.getDictValue().trim();
        validateUnique(dictType, dictLabel, dictValue, null);
        SaasTenantDictDefaultData entity = MapstructUtils.convert(bo, SaasTenantDictDefaultData.class);
        entity.setDictCode(IdGeneratorUtil.nextLongId());
        entity.setDictType(dictType);
        entity.setDictLabel(dictLabel);
        entity.setDictValue(dictValue);
        entity.setDictSort(bo.getDictSort() == null ? nextSort(dictType) : bo.getDictSort());
        entity.setIsDefault(normalizeDefault(bo.getIsDefault()));
        defaultDataMapper.insert(entity);
        return entity.getDictCode();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SaasTenantDictDefaultDataBo bo) {
        SaasTenantDictDefaultData current = defaultDataMapper.selectById(bo.getDictCode());
        if (current == null) {
            throw new ServiceException("租户字典默认值不存在");
        }
        if (!StringUtils.equals(current.getDictType(), bo.getDictType().trim())
            || !StringUtils.equals(current.getDictValue(), bo.getDictValue().trim())) {
            throw new ServiceException("字典类型和业务编码创建后不可修改");
        }
        String dictLabel = bo.getDictLabel().trim();
        validateUnique(current.getDictType(), dictLabel, current.getDictValue(), current.getDictCode());
        current.setDictLabel(dictLabel);
        current.setDictSort(bo.getDictSort() == null ? 0 : bo.getDictSort());
        current.setCssClass(bo.getCssClass());
        current.setListClass(bo.getListClass());
        current.setIsDefault(normalizeDefault(bo.getIsDefault()));
        current.setRemark(bo.getRemark());
        defaultDataMapper.updateById(current);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(Collection<Long> ids) {
        defaultDataMapper.deleteByIds(ids);
    }

    private SaasTenantDictType requireType(String dictType) {
        String normalized = dictType == null ? null : dictType.trim();
        if (StringUtils.isBlank(normalized)) {
            throw new ServiceException("字典类型不能为空");
        }
        SaasTenantDictType type = typeMapper.lambda().eq(SaasTenantDictType::getDictType, normalized).one();
        if (type == null) {
            throw new ServiceException("租户字典类型不存在");
        }
        return type;
    }

    private void validateUnique(String dictType, String dictLabel, String dictValue, Long excludedId) {
        if (defaultDataMapper.lambda().eq(SaasTenantDictDefaultData::getDictType, dictType)
            .eq(SaasTenantDictDefaultData::getDictLabel, dictLabel)
            .neIfPresent(SaasTenantDictDefaultData::getDictCode, excludedId).exists()) {
            throw new ServiceException("同一字典类型下标签不能重复");
        }
        if (defaultDataMapper.lambda().eq(SaasTenantDictDefaultData::getDictType, dictType)
            .eq(SaasTenantDictDefaultData::getDictValue, dictValue)
            .neIfPresent(SaasTenantDictDefaultData::getDictCode, excludedId).exists()) {
            throw new ServiceException("同一字典类型下业务编码不能重复");
        }
    }

    private int nextSort(String dictType) {
        SaasTenantDictDefaultData last = defaultDataMapper.lambda()
            .eq(SaasTenantDictDefaultData::getDictType, dictType)
            .orderByDesc(SaasTenantDictDefaultData::getDictSort)
            .orderByDesc(SaasTenantDictDefaultData::getDictCode)
            .last("LIMIT 1").one();
        return last == null || last.getDictSort() == null ? 1 : last.getDictSort() + 1;
    }

    private String normalizeDefault(String value) {
        return StringUtils.isBlank(value) ? SystemConstants.NO : value.trim();
    }
}
