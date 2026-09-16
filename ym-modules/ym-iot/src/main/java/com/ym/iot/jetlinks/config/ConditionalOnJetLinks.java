package com.ym.iot.jetlinks.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ConditionalOnProperty(prefix = "ym.iot.jetlinks", name = "enabled", havingValue = "true")
public @interface ConditionalOnJetLinks {}
