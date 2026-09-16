package com.ym.common.dubbo.config;

import com.ym.common.core.factory.YmlPropertySourceFactory;
import com.ym.common.dubbo.handler.DubboExceptionHandler;
import com.ym.common.dubbo.properties.DubboCustomProperties;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

/**
 * dubbo 配置类
 */
@AutoConfiguration
@EnableConfigurationProperties(DubboCustomProperties.class)
@PropertySource(value = "classpath:common-dubbo.yml", factory = YmlPropertySourceFactory.class)
public class DubboConfiguration {

    static {
        // JDK 25 下 Dubbo JsonUtils 可能在 ServiceLoader 扫描 JSON SPI 时提前探测到未引入的 Gson 实现。
        // 项目按 Dubbo 默认使用 fastjson2，这里只固定首选 JSON 实现，避免误加载 Gson。
        System.setProperty("dubbo.json-framework.prefer",
            System.getProperty("dubbo.json-framework.prefer", "fastjson2"));
    }

    /**
     * dubbo自定义IP注入(避免IP不正确问题)
     */
    @Bean
    public BeanFactoryPostProcessor customBeanFactoryPostProcessor() {
        return new CustomBeanFactoryPostProcessor();
    }

    /**
     * 异常处理器
     */
    @Bean
    public DubboExceptionHandler dubboExceptionHandler() {
        return new DubboExceptionHandler();
    }

}
