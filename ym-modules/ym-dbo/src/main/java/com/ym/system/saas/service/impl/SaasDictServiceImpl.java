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
import com.ym.system.saas.domain.*;
import com.ym.system.saas.domain.bo.*;
import com.ym.system.saas.domain.vo.*;
import com.ym.system.saas.mapper.*;
import com.ym.system.saas.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * SaaS 全局字典运营服务实现。
 */
@Service
@DS("saas")
@RequiredArgsConstructor
public class SaasDictServiceImpl implements ISaasDictService {
    private final SaasDictTypeMapper typeMapper;
    private final SaasDictDataMapper dataMapper;
    private final SaasControlNotifier controlNotifier;

    @Override
    public PageResult<SaasDictTypeVo> queryTypePage(SaasDictTypeBo bo, PageQuery query) {
        Page<SaasDictTypeVo> page = typeMapper.selectVoPage(query.build(), QueryBuilder.lambda(SaasDictType.class)
            .likeIfText(SaasDictType::getDictName, bo.getDictName()).likeIfText(SaasDictType::getDictType, bo.getDictType())
            .orderByAsc(SaasDictType::getDictId).build());
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    @Override
    public SaasDictTypeVo queryType(Long id) {
        return typeMapper.selectVoById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertType(SaasDictTypeBo bo) {
        validateType(bo, null);
        SaasDictType entity = MapstructUtils.convert(bo, SaasDictType.class);
        entity.setDictId(IdGeneratorUtil.nextLongId());
        typeMapper.insert(entity);
        controlNotifier.refreshGlobalDictAfterCommit();
        return entity.getDictId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateType(SaasDictTypeBo bo) {
        validateType(bo, bo.getDictId());
        typeMapper.updateById(MapstructUtils.convert(bo, SaasDictType.class));
        controlNotifier.refreshGlobalDictAfterCommit();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTypes(Collection<Long> ids) {
        for (SaasDictType type : typeMapper.selectByIds(ids)) {
            if (dataMapper.lambda().eq(SaasDictData::getDictType, type.getDictType()).exists()) {
                throw new ServiceException("字典类型【" + type.getDictType() + "】下仍有字典数据");
            }
        }
        typeMapper.deleteByIds(ids);
        controlNotifier.refreshGlobalDictAfterCommit();
    }

    @Override
    public PageResult<SaasDictDataVo> queryDataPage(SaasDictDataBo bo, PageQuery query) {
        Page<SaasDictDataVo> page = dataMapper.selectVoPage(query.build(), QueryBuilder.lambda(SaasDictData.class)
            .eqIfText(SaasDictData::getDictType, bo.getDictType()).likeIfText(SaasDictData::getDictLabel, bo.getDictLabel())
            .orderByAsc(SaasDictData::getDictType)
            .orderByAsc(SaasDictData::getDictSort).orderByAsc(SaasDictData::getDictCode).build());
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    @Override
    public SaasDictDataVo queryData(Long id) {
        return dataMapper.selectVoById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertData(SaasDictDataBo bo) {
        validateData(bo, null);
        SaasDictData entity = MapstructUtils.convert(bo, SaasDictData.class);
        entity.setDictCode(IdGeneratorUtil.nextLongId());
        dataMapper.insert(entity);
        controlNotifier.refreshGlobalDictAfterCommit();
        return entity.getDictCode();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateData(SaasDictDataBo bo) {
        validateData(bo, bo.getDictCode());
        dataMapper.updateById(MapstructUtils.convert(bo, SaasDictData.class));
        controlNotifier.refreshGlobalDictAfterCommit();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteData(Collection<Long> ids) {
        dataMapper.deleteByIds(ids);
        controlNotifier.refreshGlobalDictAfterCommit();
    }

    private void validateType(SaasDictTypeBo bo, Long id) {
        if (typeMapper.lambda().eq(SaasDictType::getDictType, bo.getDictType()).neIfPresent(SaasDictType::getDictId, id).exists()) {
            throw new ServiceException("字典类型已存在");
        }
    }

    private void validateData(SaasDictDataBo bo, Long id) {
        if (!typeMapper.lambda().eq(SaasDictType::getDictType, bo.getDictType()).exists())
            throw new ServiceException("字典类型不存在");
        if (dataMapper.lambda().eq(SaasDictData::getDictType, bo.getDictType()).eq(SaasDictData::getDictValue, bo.getDictValue())
            .neIfPresent(SaasDictData::getDictCode, id).exists())
            throw new ServiceException("同一字典类型下键值已存在");
    }
}
