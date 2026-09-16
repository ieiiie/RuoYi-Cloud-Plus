package com.ym.common.web.config;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.ym.common.core.utils.ObjectUtils;
import com.ym.common.json.enhance.JsonValueEnhancer;
import com.ym.common.web.advice.ResponseEnhancementAdvice;
import com.ym.common.web.handler.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 通用配置
 *
 * @author Lion Li
 */
@AutoConfiguration
public class ResourcesConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        // 全局日期格式转换配置
        registry.addConverter(String.class, Date.class, source -> {
            DateTime parse = DateUtil.parse(source);
            if (ObjectUtils.isNull(parse)) {
                return null;
            }
            return parse.toJdkDate();
        });
        registry.addConverter(String.class, LocalDateTime.class, source -> {
            DateTime parse = DateUtil.parse(source);
            if (ObjectUtils.isNull(parse)) {
                return null;
            }
            return parse.toLocalDateTime();
        });
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
    }

    /**
     * 全局异常处理器
     */
    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    /**
     * 注册响应增强处理器。
     *
     * @param jsonValueEnhancer JSON 字段增强器
     * @return 响应增强处理器
     */
    @Bean
    public ResponseEnhancementAdvice responseEnhancementAdvice(JsonValueEnhancer jsonValueEnhancer) {
        return new ResponseEnhancementAdvice(jsonValueEnhancer);
    }

}
