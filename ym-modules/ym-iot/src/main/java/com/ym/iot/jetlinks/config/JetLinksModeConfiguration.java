package com.ym.iot.jetlinks.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Refuse a partial deployment cutover; no hidden per-device routing or parallel old integrations.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnJetLinks
public class JetLinksModeConfiguration implements InitializingBean {
    private final Environment env;

    public JetLinksModeConfiguration(Environment env) {
        this.env = env;
    }

    @Override
    public void afterPropertiesSet() {
        for (String key :
                java.util.List.of(
                        "ym.mqtt.enabled",
                        "mqtt.enabled",
                        "mqtt.client.enabled",
                        "mica.mqtt.enabled",
                        "ym.iot.fertilizer.enabled",
                        "ym.iot.motorvalve.enabled",
                        "ym.iot.hfzk.enabled",
                        "ym.iot.wvp.enabled",
                        "ym.iot.emqx-presence.enabled"))
            if (env.getProperty(key, Boolean.class, false))
                throw new IllegalStateException(
                        "JetLinks active mode requires coordinated cutover: disable " + key);
    }
}
