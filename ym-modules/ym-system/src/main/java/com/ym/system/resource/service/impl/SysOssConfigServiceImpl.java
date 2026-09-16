package com.ym.system.resource.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.ym.common.core.constant.CacheNames;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.common.core.utils.ObjectUtils;
import com.ym.common.core.utils.SpringUtils;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.json.utils.JsonUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.mybatis.core.query.QueryBuilder;
import com.ym.common.oss.constant.OssConstant;
import com.ym.common.redis.utils.CacheUtils;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.system.resource.domain.SysOssConfig;
import com.ym.system.resource.domain.bo.SysOssConfigBo;
import com.ym.system.resource.domain.vo.SysOssConfigVo;
import com.ym.system.resource.event.OssConfigChangeEvent;
import com.ym.system.resource.mapper.SysOssConfigMapper;
import com.ym.system.resource.service.ISysOssConfigService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * 对象存储配置Service业务层处理
 *
 * @author Lion Li
 * @author 孤舟烟雨
 * @date 2021-08-13
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysOssConfigServiceImpl implements ISysOssConfigService {

    private final SysOssConfigMapper ossConfigMapper;

    /**
     * 项目启动时，初始化参数到缓存，加载配置类
     */
    @Override
    public void init() {
        // sys_oss_config 是平台全局表；应用启动阶段没有 HTTP 登录上下文，需显式跳过租户行级拦截。
        List<SysOssConfig> list = TenantHelper.ignore((Supplier<List<SysOssConfig>>) ossConfigMapper::selectList);
        // 加载OSS初始化配置
        for (SysOssConfig config : list) {
            CacheUtils.put(CacheNames.SYS_OSS_CONFIG, config.getConfigKey(), JsonUtils.toJsonString(config));
        }
    }

    @Override
    public SysOssConfigVo queryById(Long ossConfigId) {
        return ossConfigMapper.selectVoById(ossConfigId);
    }

    @Override
    public PageResult<SysOssConfigVo> queryPageList(SysOssConfigBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysOssConfig> lqw = buildQueryWrapper(bo);
        Page<SysOssConfigVo> result = ossConfigMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(result.getRecords(), result.getTotal());
    }


    private LambdaQueryWrapper<SysOssConfig> buildQueryWrapper(SysOssConfigBo bo) {
        return QueryBuilder.lambda(SysOssConfig.class)
            .eqIfText(SysOssConfig::getConfigKey, bo.getConfigKey())
            .likeIfText(SysOssConfig::getBucketName, bo.getBucketName())
            .orderByAsc(SysOssConfig::getOssConfigId)
            .build();
    }

    @Override
    public Boolean insertByBo(SysOssConfigBo bo) {
        SysOssConfig config = MapstructUtils.convert(bo, SysOssConfig.class);
        validEntityBeforeSave(config);
        boolean flag = ossConfigMapper.insert(config) > 0;
        if (flag) {
            // 从数据库查询完整的数据做缓存
            config = ossConfigMapper.selectById(config.getOssConfigId());
            publishOssConfigSaved(config, null);
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(SysOssConfigBo bo) {
        SysOssConfig config = MapstructUtils.convert(bo, SysOssConfig.class);
        validEntityBeforeSave(config);
        SysOssConfig oldConfig = ossConfigMapper.selectById(config.getOssConfigId());
        boolean flag = ossConfigMapper.lambda()
            .set(ObjectUtil.isNull(config.getPrefix()), SysOssConfig::getPrefix, "")
            .set(ObjectUtil.isNull(config.getRegion()), SysOssConfig::getRegion, "")
            .set(ObjectUtil.isNull(config.getExt1()), SysOssConfig::getExt1, "")
            .set(ObjectUtil.isNull(config.getRemark()), SysOssConfig::getRemark, "")
            .eq(SysOssConfig::getOssConfigId, config.getOssConfigId())
            .update(config);
        if (flag) {
            // 从数据库查询完整的数据做缓存
            config = ossConfigMapper.selectById(config.getOssConfigId());
            publishOssConfigSaved(config, ObjectUtils.notNullGetter(oldConfig, SysOssConfig::getConfigKey));
        }
        return flag;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(SysOssConfig entity) {
        if (StringUtils.isNotEmpty(entity.getConfigKey()) && !checkConfigKeyUnique(entity)) {
            throw new ServiceException("操作配置'{}'失败, 配置key已存在!", entity.getConfigKey());
        }
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            if (CollUtil.containsAny(ids, OssConstant.SYSTEM_DATA_IDS)) {
                throw new ServiceException("系统内置, 不可删除!");
            }
        }
        List<SysOssConfig> list = CollUtil.newArrayList();
        for (Long configId : ids) {
            SysOssConfig config = ossConfigMapper.selectById(configId);
            if (ObjectUtil.isNotNull(config)) {
                list.add(config);
            }
        }
        boolean flag = ossConfigMapper.deleteByIds(ids) > 0;
        if (flag) {
            list.forEach(sysOssConfig ->
                SpringUtils.context().publishEvent(OssConfigChangeEvent.remove(sysOssConfig.getConfigKey())));
        }
        return flag;
    }

    /**
     * 判断configKey是否唯一
     */
    private boolean checkConfigKeyUnique(SysOssConfig sysOssConfig) {
        long ossConfigId = ObjectUtils.notNull(sysOssConfig.getOssConfigId(), -1L);
        SysOssConfig info = ossConfigMapper.lambda()
            .select(SysOssConfig::getOssConfigId, SysOssConfig::getConfigKey)
            .eq(SysOssConfig::getConfigKey, sysOssConfig.getConfigKey())
            .one();
        return ObjectUtil.isNull(info) || ObjectUtil.equals(info.getOssConfigId(), ossConfigId);
    }

    /**
     * 发布 OSS 配置保存事件。
     *
     * @param config       当前配置
     * @param oldConfigKey 变更前配置 key
     */
    private void publishOssConfigSaved(SysOssConfig config, String oldConfigKey) {
        SpringUtils.context().publishEvent(OssConfigChangeEvent.save(
            config.getConfigKey(),
            oldConfigKey,
            JsonUtils.toJsonString(config)
        ));
    }

}
