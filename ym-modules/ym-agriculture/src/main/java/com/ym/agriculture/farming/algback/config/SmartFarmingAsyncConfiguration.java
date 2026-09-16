package com.ym.agriculture.farming.algback.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 启用异步事件监听（如租户创建后初始化算法中台绑定占位）。
 */
@Configuration
@EnableAsync
public class SmartFarmingAsyncConfiguration {
}
