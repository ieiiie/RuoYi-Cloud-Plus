package com.ym.auth.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** 微信小程序凭据，支持农业与农事任务多 appid。 */
@Data
@Component
@ConfigurationProperties(prefix = "weixin.miniapp")
public class WeixinMiniappProperties {

    private List<AppCredential> apps = new ArrayList<>();

    public String requireSecret(String appId) {
        if (appId == null || appId.isBlank()) {
            throw new IllegalArgumentException("微信 appid 不能为空");
        }
        return apps.stream()
            .filter(app -> appId.equals(app.getAppId()))
            .map(AppCredential::getAppSecret)
            .filter(secret -> secret != null && !secret.isBlank() && !secret.contains("CHANGE_ME"))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("未配置微信小程序凭据: " + appId));
    }

    @Data
    public static class AppCredential {
        private String appId;
        private String appSecret;
    }
}
