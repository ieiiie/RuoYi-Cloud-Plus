package com.ym.agriculture.miniapp;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

/**
 * 农业小程序接口聚合服务。
 */
@EnableDubbo
@SpringBootApplication
public class YmAgricultureMiniappApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(YmAgricultureMiniappApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(1024));
        application.run(args);
    }
}
