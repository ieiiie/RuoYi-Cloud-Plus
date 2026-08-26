package org.dromara.common.tenant.config;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.dromara.common.core.utils.reflect.ReflectUtils;
import org.dromara.common.redis.config.RedisConfiguration;
import org.dromara.common.redis.config.properties.RedissonProperties;
import org.dromara.common.tenant.core.TenantSaTokenDao;
import org.dromara.common.tenant.handle.PlusTenantLineHandler;
import org.dromara.common.tenant.handle.TenantKeyPrefixHandler;
import org.dromara.common.tenant.manager.TenantSpringCacheManager;
import org.dromara.common.tenant.properties.TenantProperties;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.SingleServerConfig;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 多租户自动配置。
 *
 * @author Lion Li
 */
@EnableConfigurationProperties(TenantProperties.class)
@AutoConfiguration(after = RedisConfiguration.class,
    beforeName = "org.dromara.common.mybatis.config.MybatisPlusConfiguration")
@ConditionalOnProperty(value = "tenant.enable", havingValue = "true")
public class TenantConfiguration {

    /**
     * 仅在 MyBatis-Plus 存在时注册行级租户拦截器。
     */
    @ConditionalOnClass(TenantLineInnerInterceptor.class)
    @AutoConfiguration
    static class MybatisPlusConfiguration {

        /**
         * 多租户插件。
         *
         * @param tenantProperties 租户配置
         * @return 租户行级拦截器
         */
        @Bean
        public TenantLineInnerInterceptor tenantLineInnerInterceptor(TenantProperties tenantProperties) {
            return new TenantLineInnerInterceptor(new PlusTenantLineHandler(tenantProperties));
        }

    }

    /**
     * 为租户业务 Redis Key 加入租户前缀。
     *
     * @param redissonProperties Redisson 配置
     * @return Redisson 定制器
     */
    @Bean
    public RedissonAutoConfigurationCustomizer tenantRedissonCustomizer(RedissonProperties redissonProperties) {
        return config -> {
            TenantKeyPrefixHandler nameMapper = new TenantKeyPrefixHandler(redissonProperties.getKeyPrefix());
            SingleServerConfig singleServerConfig = ReflectUtils.invokeGetter(config, "singleServerConfig");
            if (ObjectUtil.isNotNull(singleServerConfig)) {
                singleServerConfig.setNameMapper(nameMapper);
            }
            ClusterServersConfig clusterServersConfig = ReflectUtils.invokeGetter(config, "clusterServersConfig");
            if (ObjectUtil.isNotNull(clusterServersConfig)) {
                clusterServersConfig.setNameMapper(nameMapper);
            }
        };
    }

    /**
     * 为 Spring Cache 名称加入租户前缀。
     *
     * @return 多租户缓存管理器
     */
    @Primary
    @Bean
    public CacheManager tenantCacheManager() {
        return new TenantSpringCacheManager();
    }

    /**
     * 令 Sa-Token 认证数据始终使用全局 Redis 命名空间。
     *
     * @return 多租户 Sa-Token DAO
     */
    @Primary
    @Bean
    public SaTokenDao tenantSaTokenDao() {
        return new TenantSaTokenDao();
    }

}
