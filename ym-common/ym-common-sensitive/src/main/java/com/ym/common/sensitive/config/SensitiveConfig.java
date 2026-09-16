package com.ym.common.sensitive.config;

import com.ym.common.sensitive.handler.SensitiveJsonFieldProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 脱敏模块配置。
 */
@AutoConfiguration
public class SensitiveConfig {

    @Bean
    public SensitiveJsonFieldProcessor sensitiveJsonFieldProcessor() {
        return new SensitiveJsonFieldProcessor();
    }

}
