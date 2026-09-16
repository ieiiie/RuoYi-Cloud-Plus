package com.ym.agriculture;

import com.aizuda.oss.autoconfigure.OssAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

/**
 * 农业业务服务。
 */
@EnableDubbo
@MapperScan("com.ym.agriculture.**.dao")
@SpringBootApplication(exclude = OssAutoConfiguration.class)
public class YmAgricultureApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(YmAgricultureApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
    }
}
