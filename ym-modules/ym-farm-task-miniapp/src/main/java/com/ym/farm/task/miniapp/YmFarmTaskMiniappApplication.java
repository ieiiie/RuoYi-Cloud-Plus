package com.ym.farm.task.miniapp;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

/**
 * 农事任务小程序接口聚合服务。
 */
@EnableDubbo
@SpringBootApplication
public class YmFarmTaskMiniappApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(YmFarmTaskMiniappApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(1024));
        application.run(args);
    }
}
