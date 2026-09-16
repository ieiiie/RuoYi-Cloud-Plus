package com.ym.common.redis.config;

import com.ym.common.redis.aspectj.RateLimiterAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * @author guangxin
 * @date 2023/1/18
 */
@AutoConfiguration(after = RedisConfiguration.class)
public class RateLimiterConfig {

    @Bean
    public RateLimiterAspect rateLimiterAspect() {
        return new RateLimiterAspect();
    }

}
