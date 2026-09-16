package com.ym.agriculture.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * 农业外围适配仍使用 Jackson 2 的兼容对象模型；Spring Boot 4 的全局 MVC
 * 使用 Jackson 3，两者类型不同，因此为回调、Retrofit 和库存扩展字段提供局部兼容 Bean。
 */
@Configuration(proxyBeanMethods = false)
public class AgricultureJackson2Configuration {

    @Bean
    public ObjectMapper agricultureJackson2ObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    public Jackson2ObjectMapperBuilder agricultureJackson2ObjectMapperBuilder() {
        return new Jackson2ObjectMapperBuilder().findModulesViaServiceLoader(true);
    }
}
