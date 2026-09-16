package com.ym.agriculture.shared.config;

import com.aizuda.oss.autoconfigure.OssAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 农业卫星影像上游 OSS 适配开关。
 * <p>
 * aizuda-oss 在未配置任何存储平台时会直接中止应用启动，因此默认不导入其
 * 自动装配。启用后仍由原组件校验 {@code aizuda.oss} 的完整性，禁止使用内置密钥。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "ym.agriculture.oss", name = "enabled", havingValue = "true")
@Import(OssAutoConfiguration.class)
public class AgricultureOssConfiguration {
}
