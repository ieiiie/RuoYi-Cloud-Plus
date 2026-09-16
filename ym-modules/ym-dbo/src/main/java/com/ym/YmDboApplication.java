package com.ym;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Dbo 内部运营平台服务。
 */
@EnableDubbo
@EnableScheduling
@SpringBootApplication(scanBasePackages = {
    "com.ym.auth",
    "com.ym.system",
    "com.ym.web"
})
public class YmDboApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(YmDboApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("Dbo 运营平台服务启动成功");
    }
}
