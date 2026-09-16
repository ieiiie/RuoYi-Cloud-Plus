package com.ym.system.saas.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.mybatis.core.query.QueryBuilder;
import com.ym.common.mybatis.utils.IdGeneratorUtil;
import com.ym.system.saas.domain.SaasTenantDictData;
import com.ym.system.saas.domain.SaasTenantDictDefaultData;
import com.ym.system.saas.domain.SaasTenantDictType;
import com.ym.system.saas.domain.bo.SaasTenantDictTypeBo;
import com.ym.system.saas.domain.vo.SaasTenantDictTypeVo;
import com.ym.system.saas.mapper.SaasTenantDictDataMapper;
import com.ym.system.saas.mapper.SaasTenantDictDefaultDataMapper;
import com.ym.system.saas.mapper.SaasTenantDictTypeMapper;
import com.ym.system.saas.service.ISaasTenantDictTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

/** Dbo 通过 saas 数据源维护租户字典类型。 */
@Service
@DS("saas")
@RequiredArgsConstructor
public class SaasTenantDictTypeServiceImpl implements ISaasTenantDictTypeService {
    private final SaasTenantDictTypeMapper typeMapper;
    private final SaasTenantDictDataMapper dataMapper;
    private final SaasTenantDictDefaultDataMapper defaultDataMapper;

    @Override
    public PageResult<SaasTenantDictTypeVo> queryPage(SaasTenantDictTypeBo bo, PageQuery pageQuery) {
        Page<SaasTenantDictTypeVo> page = typeMapper.selectVoPage(pageQuery.build(),
            QueryBuilder.lambda(SaasTenantDictType.class)
                .likeIfText(SaasTenantDictType::getDictName, bo.getDictName())
                .likeIfText(SaasTenantDictType::getDictType, bo.getDictType())
                .orderByAsc(SaasTenantDictType::getDictId).build());
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    @Override
    public SaasTenantDictTypeVo queryById(Long id) {
        return typeMapper.selectVoById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insert(SaasTenantDictTypeBo bo) {
        String dictType = bo.getDictType().trim();
        if (typeMapper.lambda().eq(SaasTenantDictType::getDictType, dictType).exists()) {
            throw new ServiceException("字典类型已存在");
        }
        SaasTenantDictType entity = MapstructUtils.convert(bo, SaasTenantDictType.class);
        entity.setDictId(IdGeneratorUtil.nextLongId());
        entity.setDictName(bo.getDictName().trim());
        entity.setDictType(dictType);
        typeMapper.insert(entity);
        return entity.getDictId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SaasTenantDictTypeBo bo) {
        SaasTenantDictType current = typeMapper.selectById(bo.getDictId());
        if (current == null) {
            throw new ServiceException("租户字典类型不存在");
        }
        current.setDictName(bo.getDictName().trim());
        current.setRemark(bo.getRemark());
        typeMapper.updateById(current);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(Collection<Long> ids) {
        for (SaasTenantDictType type : typeMapper.selectByIds(ids)) {
            if (defaultDataMapper.lambda().eq(SaasTenantDictDefaultData::getDictType, type.getDictType()).exists()) {
                throw new ServiceException("字典类型【" + type.getDictName() + "】仍有默认值，不能删除");
            }
            if (dataMapper.lambda().eq(SaasTenantDictData::getDictType, type.getDictType()).exists()) {
                throw new ServiceException("字典类型【" + type.getDictName() + "】已被租户使用，不能删除");
            }
        }
        typeMapper.deleteByIds(ids);
    }
}
