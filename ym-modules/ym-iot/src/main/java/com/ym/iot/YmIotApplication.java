package com.ym.iot;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

/**
 * 物联网业务服务。
 */
@EnableDubbo
@MapperScan("com.ym.iot.**.dao")
@SpringBootApplication
public class YmIotApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(YmIotApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
    }
}
