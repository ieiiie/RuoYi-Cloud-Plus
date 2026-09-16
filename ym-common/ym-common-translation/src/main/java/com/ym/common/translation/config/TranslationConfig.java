package com.ym.common.translation.config;

import com.ym.common.translation.core.TranslationInterface;
import com.ym.common.translation.core.handler.TranslationJsonFieldProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * 翻译模块配置类
 *
 * @author Lion Li
 */
@AutoConfiguration
public class TranslationConfig {

    @Bean
    public TranslationJsonFieldProcessor translationJsonFieldProcessor(List<TranslationInterface<?>> list) {
        return new TranslationJsonFieldProcessor(list);
    }

}
