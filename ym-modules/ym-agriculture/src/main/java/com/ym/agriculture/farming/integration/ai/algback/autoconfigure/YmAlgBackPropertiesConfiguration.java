package com.ym.agriculture.farming.integration.ai.algback.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 注册 {@link YmAlgBackProperties}，未配置 {@code base-url} 时仍可读取 {@code callback-url} 等项。
 *
 * @author ym-cloud
 */
@AutoConfiguration
@EnableConfigurationProperties(YmAlgBackProperties.class)
public class YmAlgBackPropertiesConfiguration {
}
