package com.ym.system.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import com.ym.common.core.constant.CacheNames;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.common.core.utils.ObjectUtils;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.json.utils.JsonUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.mybatis.core.query.QueryBuilder;
import com.ym.common.redis.utils.CacheUtils;
import com.ym.system.domain.SysConfig;
import com.ym.system.domain.SysConfigDefinition;
import com.ym.system.domain.SysTenant;
import com.ym.system.domain.SysTenantPackageApp;
import com.ym.system.domain.SysTenantPackageMenu;
import com.ym.system.domain.SysMenu;
import com.ym.system.domain.bo.SysConfigBo;
import com.ym.system.domain.bo.SysConfigValueBo;
import com.ym.system.domain.vo.SysConfigVo;
import com.ym.system.mapper.SysConfigMapper;
import com.ym.system.mapper.SysConfigDefinitionMapper;
import com.ym.system.mapper.SysTenantMapper;
import com.ym.system.mapper.SysTenantPackageAppMapper;
import com.ym.system.mapper.SysTenantPackageMenuMapper;
import com.ym.system.mapper.SysMenuMapper;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.system.service.ISysConfigService;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 参数配置 服务层实现
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysConfigServiceImpl implements ISysConfigService {

    private static final String MAX_CONCURRENT_LOGIN_COUNT_KEY = "sys.account.maxConcurrentLoginCount";

    private final SysConfigMapper configMapper;
    private final SysConfigDefinitionMapper definitionMapper;
    private final SysTenantMapper tenantMapper;
    private final SysTenantPackageAppMapper packageAppMapper;
    private final SysTenantPackageMenuMapper packageMenuMapper;
    private final SysMenuMapper menuMapper;

    /**
     * 分页查询参数配置列表
     *
     * @param config    查询条件
     * @param pageQuery 分页参数
     * @return 参数配置分页列表
     */
    @Override
    public PageResult<SysConfigVo> selectPageConfigList(SysConfigBo config, PageQuery pageQuery) {
        LambdaQueryWrapper<SysConfig> lqw = buildQueryWrapper(config);
        applyDefinitionVisibility(lqw);
        Page<SysConfigVo> page = configMapper.selectVoPage(pageQuery.build(), lqw);
        page.getRecords().forEach(this::enrichDefinition);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    /**
     * 查询参数配置信息
     *
     * @param configId 参数配置ID
     * @return 参数配置信息
     */
    @Override
    public SysConfigVo selectConfigById(Long configId) {
        SysConfigVo config = configMapper.selectVoById(configId);
        if (config == null || config.getDefinitionId() == null
            || !allowedDefinitionIds().contains(config.getDefinitionId())) {
            throw new ServiceException("参数不存在或当前套餐未开通所属应用");
        }
        return enrichDefinition(config);
    }

    /**
     * 根据键名查询参数配置信息
     *
     * @param configKey 参数key
     * @return 参数键值
     */
    @Cacheable(cacheNames = CacheNames.SYS_CONFIG, key = "#configKey")
    @Override
    public String selectConfigByKey(String configKey) {
        SysConfig retConfig = configMapper.lambda().eq(SysConfig::getConfigKey, configKey).one();
        return ObjectUtils.notNullGetter(retConfig, SysConfig::getConfigValue, StringUtils.EMPTY);
    }

    /**
     * 获取注册开关
     *
     * @return true开启，false关闭
     */
    @Override
    public boolean selectRegisterEnabled() {
        String configValue = this.selectConfigByKey("sys.account.registerUser");
        return Convert.toBool(configValue);
    }

    /**
     * 查询参数配置列表
     *
     * @param config 参数配置信息
     * @return 参数配置集合
     */
    @Override
    public List<SysConfigVo> selectConfigList(SysConfigBo config) {
        LambdaQueryWrapper<SysConfig> lqw = buildQueryWrapper(config);
        applyDefinitionVisibility(lqw);
        List<SysConfigVo> list = configMapper.selectVoList(lqw);
        list.forEach(this::enrichDefinition);
        return list;
    }

    private LambdaQueryWrapper<SysConfig> buildQueryWrapper(SysConfigBo bo) {
        Map<String, Object> params = bo.getParams();
        return QueryBuilder.lambda(SysConfig.class)
            .likeIfText(SysConfig::getConfigName, bo.getConfigName())
            .eqIfText(SysConfig::getConfigType, bo.getConfigType())
            .likeIfText(SysConfig::getConfigKey, bo.getConfigKey())
            .betweenParams(SysConfig::getCreateTime, params, "beginTime", "endTime")
            .orderByAsc(SysConfig::getConfigId)
            .build();
    }

    /** 只展示全局定义及当前套餐已开通应用的有效定义。 */
    private void applyDefinitionVisibility(LambdaQueryWrapper<SysConfig> wrapper) {
        Set<Long> definitionIds = allowedDefinitionIds();
        if (definitionIds.isEmpty()) {
            wrapper.apply("1 = 0");
        } else {
            wrapper.in(SysConfig::getDefinitionId, definitionIds);
        }
    }

    private Set<Long> allowedDefinitionIds() {
        SysTenant tenant = tenantMapper.lambda()
            .eq(SysTenant::getTenantId, LoginHelper.getTenantId())
            .one();
        if (tenant == null || tenant.getPackageId() == null) {
            return Set.of();
        }
        Set<Long> appIds = packageAppMapper.selectList(
                new LambdaQueryWrapper<SysTenantPackageApp>()
                    .eq(SysTenantPackageApp::getPackageId, tenant.getPackageId()))
            .stream().map(SysTenantPackageApp::getAppId)
            .collect(java.util.stream.Collectors.toSet());
        List<Long> packageMenuIds = packageMenuMapper.selectList(
                new LambdaQueryWrapper<SysTenantPackageMenu>()
                    .select(SysTenantPackageMenu::getMenuId)
                    .eq(SysTenantPackageMenu::getPackageId, tenant.getPackageId()))
            .stream().map(SysTenantPackageMenu::getMenuId).toList();
        if (!packageMenuIds.isEmpty()) {
            menuMapper.lambda().select(SysMenu::getAppId).in(SysMenu::getMenuId, packageMenuIds).list().stream()
                .map(SysMenu::getAppId).filter(java.util.Objects::nonNull).forEach(appIds::add);
        }
        LambdaQueryWrapper<SysConfigDefinition> definitionQuery = new LambdaQueryWrapper<SysConfigDefinition>()
            .select(SysConfigDefinition::getDefinitionId)
            .eq(SysConfigDefinition::getStatus, SystemConstants.NORMAL)
            .and(query -> {
                query.isNull(SysConfigDefinition::getAppId);
                if (!appIds.isEmpty()) {
                    query.or().in(SysConfigDefinition::getAppId, appIds);
                }
            });
        return definitionMapper.selectList(definitionQuery).stream()
            .map(SysConfigDefinition::getDefinitionId)
            .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * 新增参数配置
     *
     * @param bo 参数配置信息
     * @return 结果
     */
    @CachePut(cacheNames = CacheNames.SYS_CONFIG, key = "#bo.configKey")
    @Override
    public String insertConfig(SysConfigBo bo) {
        SysConfig config = MapstructUtils.convert(bo, SysConfig.class);
        int row = configMapper.insert(config);
        if (row > 0) {
            return config.getConfigValue();
        }
        throw new ServiceException("操作失败");
    }

    /**
     * 修改参数配置
     *
     * @param bo 参数配置信息
     * @return 结果
     */
    @CachePut(cacheNames = CacheNames.SYS_CONFIG, key = "#bo.configKey")
    @Override
    public String updateConfig(SysConfigBo bo) {
        int row;
        SysConfig config = MapstructUtils.convert(bo, SysConfig.class);
        if (config.getConfigId() != null) {
            SysConfig temp = configMapper.selectById(config.getConfigId());
            if (ObjectUtil.isNotNull(temp) && !StringUtils.equals(temp.getConfigKey(), config.getConfigKey())) {
                CacheUtils.evict(CacheNames.SYS_CONFIG, temp.getConfigKey());
            }
            row = configMapper.updateById(config);
        } else {
            CacheUtils.evict(CacheNames.SYS_CONFIG, config.getConfigKey());
            row = configMapper.lambda()
                .eq(SysConfig::getConfigKey, config.getConfigKey())
                .updateCount(config);
        }
        if (row > 0) {
            return config.getConfigValue();
        }
        throw new ServiceException("操作失败");
    }

    /**
     * 批量删除参数信息
     *
     * @param configIds 需要删除的参数ID
     */
    @Override
    public void deleteConfigByIds(List<Long> configIds) {
        List<SysConfig> list = configMapper.selectByIds(configIds);
        list.forEach(config -> {
            if (StringUtils.equals(SystemConstants.YES, config.getConfigType())) {
                throw new ServiceException("内置参数【{}】不能删除", config.getConfigKey());
            }
            CacheUtils.evict(CacheNames.SYS_CONFIG, config.getConfigKey());
        });
        configMapper.deleteByIds(configIds);
    }

    /**
     * 重置参数缓存数据
     */
    @Override
    public void resetConfigCache() {
        CacheUtils.clear(CacheNames.SYS_CONFIG);
    }

    /**
     * 校验参数键名是否唯一
     *
     * @param config 参数配置信息
     * @return 结果
     */
    @Override
    public boolean checkConfigKeyUnique(SysConfigBo config) {
        boolean exist = configMapper.lambda()
            .eq(SysConfig::getConfigKey, config.getConfigKey())
            .neIfPresent(SysConfig::getConfigId, config.getConfigId())
            .exists();
        return !exist;
    }

    @Override
    public void updateConfigValue(Long configId, SysConfigValueBo bo) {
        SysConfig config = configMapper.selectById(configId);
        if (config == null || config.getDefinitionId() == null) {
            throw new ServiceException("参数不存在或尚未绑定运营定义");
        }
        if (!allowedDefinitionIds().contains(config.getDefinitionId())) {
            throw new ServiceException("当前套餐未开通该参数所属应用");
        }
        SysConfigDefinition definition = definitionMapper.selectById(config.getDefinitionId());
        if (definition == null || !SystemConstants.NORMAL.equals(definition.getStatus())) {
            throw new ServiceException("参数定义已停用");
        }
        if (!Boolean.TRUE.equals(definition.getTenantEditable())) {
            throw new ServiceException("该参数不允许租户修改");
        }
        validateValue(definition, bo.getConfigValue());
        config.setConfigValue(bo.getConfigValue());
        if (configMapper.updateById(config) < 1) {
            throw new ServiceException("参数值更新失败");
        }
        CacheUtils.evict(CacheNames.SYS_CONFIG, config.getConfigKey());
    }

    private SysConfigVo enrichDefinition(SysConfigVo vo) {
        if (vo == null || vo.getDefinitionId() == null) {
            return vo;
        }
        SysConfigDefinition definition = definitionMapper.selectById(vo.getDefinitionId());
        if (definition != null) {
            vo.setValueType(definition.getValueType());
            vo.setTenantEditable(definition.getTenantEditable());
            vo.setRequiredFlag(definition.getRequiredFlag());
            vo.setMinLength(definition.getMinLength());
            vo.setMaxLength(definition.getMaxLength());
            vo.setMinValue(definition.getMinValue());
            vo.setMaxValue(definition.getMaxValue());
            vo.setRegexPattern(definition.getRegexPattern());
            vo.setEnumOptions(definition.getEnumOptions());
            if ("PASSWORD".equals(definition.getValueType()) && StringUtils.isNotBlank(vo.getConfigValue())) {
                vo.setConfigValue("******");
            }
        }
        return vo;
    }

    private void validateValue(SysConfigDefinition definition, String value) {
        if (Boolean.TRUE.equals(definition.getRequiredFlag()) && StringUtils.isBlank(value)) {
            throw new ServiceException("参数【{}】不能为空", definition.getConfigName());
        }
        if (value == null) {
            return;
        }
        if (MAX_CONCURRENT_LOGIN_COUNT_KEY.equals(definition.getConfigKey())) {
            try {
                int limit = Integer.parseInt(value);
                if (limit != -1 && limit < 1) {
                    throw new ServiceException("并发登录数量只能为 -1 或正整数");
                }
            } catch (NumberFormatException ex) {
                throw new ServiceException("并发登录数量只能为 -1 或正整数");
            }
        }
        if (definition.getMinLength() != null && value.length() < definition.getMinLength()) {
            throw new ServiceException("参数长度不能小于{}", definition.getMinLength());
        }
        if (definition.getMaxLength() != null && value.length() > definition.getMaxLength()) {
            throw new ServiceException("参数长度不能大于{}", definition.getMaxLength());
        }
        if (StringUtils.isNotBlank(definition.getRegexPattern()) && !Pattern.matches(definition.getRegexPattern(), value)) {
            throw new ServiceException("参数格式不符合运营定义");
        }
        try {
            switch (definition.getValueType()) {
                case "INTEGER" -> validateNumber(definition, new BigDecimal(value), true);
                case "DECIMAL" -> validateNumber(definition, new BigDecimal(value), false);
                case "BOOLEAN" -> {
                    if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                        throw new ServiceException("布尔参数只能为 true 或 false");
                    }
                }
                case "ENUM" -> {
                    List<String> options = JsonUtils.parseArray(definition.getEnumOptions(), String.class);
                    if (!options.contains(value)) {
                        throw new ServiceException("参数值不在允许的枚举范围内");
                    }
                }
                case "JSON" -> JsonUtils.getJsonMapper().readTree(value);
                default -> { }
            }
        } catch (ServiceException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ServiceException("参数值与定义类型不匹配");
        }
    }

    private void validateNumber(SysConfigDefinition definition, BigDecimal number, boolean integer) {
        if (integer && number.stripTrailingZeros().scale() > 0) {
            throw new ServiceException("参数值必须是整数");
        }
        if (definition.getMinValue() != null && number.compareTo(definition.getMinValue()) < 0) {
            throw new ServiceException("参数值不能小于{}", definition.getMinValue());
        }
        if (definition.getMaxValue() != null && number.compareTo(definition.getMaxValue()) > 0) {
            throw new ServiceException("参数值不能大于{}", definition.getMaxValue());
        }
    }

}
