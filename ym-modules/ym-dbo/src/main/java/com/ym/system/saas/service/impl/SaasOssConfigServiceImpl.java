package com.ym.system.saas.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.*;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.mybatis.core.query.QueryBuilder;
import com.ym.common.mybatis.utils.IdGeneratorUtil;
import com.ym.system.saas.domain.SaasOssConfig;
import com.ym.system.saas.domain.SaasTenant;
import com.ym.system.saas.domain.bo.SaasOssConfigBo;
import com.ym.system.saas.domain.vo.SaasOssConfigVo;
import com.ym.system.saas.mapper.SaasOssConfigMapper;
import com.ym.system.saas.mapper.SaasTenantMapper;
import com.ym.system.saas.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * SaaS OSS 配置运营服务实现。
 */
@Service
@DS("saas")
@RequiredArgsConstructor
public class SaasOssConfigServiceImpl implements ISaasOssConfigService {
    private final SaasOssConfigMapper configMapper;
    private final SaasTenantMapper tenantMapper;
    private final SaasControlNotifier controlNotifier;

    @Override
    public PageResult<SaasOssConfigVo> queryPageList(SaasOssConfigBo bo, PageQuery query) {
        Page<SaasOssConfigVo> page = configMapper.selectVoPage(query.build(), QueryBuilder.lambda(SaasOssConfig.class)
            .likeIfText(SaasOssConfig::getConfigKey, bo.getConfigKey()).likeIfText(SaasOssConfig::getBucketName, bo.getBucketName())
            .orderByAsc(SaasOssConfig::getOssConfigId).build());
        page.getRecords().forEach(this::maskSecret);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    @Override
    public List<SaasOssConfigVo> queryList() {
        List<SaasOssConfigVo> list = configMapper.selectVoList(QueryBuilder.lambda(SaasOssConfig.class)
            .orderByAsc(SaasOssConfig::getOssConfigId).build());
        list.forEach(this::maskSecret);
        return list;
    }

    @Override
    public SaasOssConfigVo queryById(Long id) {
        SaasOssConfigVo vo = configMapper.selectVoById(id);
        if (vo != null) vo.setSecretKey("");
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertByBo(SaasOssConfigBo bo) {
        validateKey(bo, null);
        SaasOssConfig entity = MapstructUtils.convert(bo, SaasOssConfig.class);
        entity.setOssConfigId(IdGeneratorUtil.nextLongId());
        configMapper.insert(entity);
        controlNotifier.refreshOssConfigAfterCommit(entity.getConfigKey());
        return entity.getOssConfigId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateByBo(SaasOssConfigBo bo) {
        SaasOssConfig old = configMapper.selectById(bo.getOssConfigId());
        if (old == null) throw new ServiceException("OSS配置不存在");
        validateKey(bo, bo.getOssConfigId());
        SaasOssConfig entity = MapstructUtils.convert(bo, SaasOssConfig.class);
        if (StringUtils.isBlank(entity.getSecretKey())) entity.setSecretKey(old.getSecretKey());
        configMapper.updateById(entity);
        controlNotifier.refreshOssConfigAfterCommit(entity.getConfigKey());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWithValidByIds(Collection<Long> ids) {
        if (tenantMapper.lambda().in(SaasTenant::getOssConfigId, ids).exists()) {
            throw new ServiceException("OSS配置已被租户绑定，不能删除");
        }
        configMapper.deleteByIds(ids);
        controlNotifier.refreshOssConfigAfterCommit("*");
    }

    private void validateKey(SaasOssConfigBo bo, Long id) {
        if (configMapper.lambda().eq(SaasOssConfig::getConfigKey, bo.getConfigKey())
            .neIfPresent(SaasOssConfig::getOssConfigId, id).exists()) throw new ServiceException("配置Key已存在");
    }

    private void maskSecret(SaasOssConfigVo vo) {
        vo.setSecretKey(StringUtils.isBlank(vo.getSecretKey()) ? "" : "******");
    }
}
