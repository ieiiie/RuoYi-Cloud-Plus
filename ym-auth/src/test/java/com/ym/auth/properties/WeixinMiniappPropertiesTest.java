package com.ym.auth.properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("dev")
class WeixinMiniappPropertiesTest {

    @Test
    void resolvesConfiguredSecretByAppId() {
        WeixinMiniappProperties properties = new WeixinMiniappProperties();
        WeixinMiniappProperties.AppCredential credential = new WeixinMiniappProperties.AppCredential();
        credential.setAppId("wx-agriculture");
        credential.setAppSecret("test-secret");
        properties.setApps(List.of(credential));

        assertEquals("test-secret", properties.requireSecret("wx-agriculture"));
    }

    @Test
    void rejectsMissingAndPlaceholderCredentials() {
        WeixinMiniappProperties properties = new WeixinMiniappProperties();
        WeixinMiniappProperties.AppCredential credential = new WeixinMiniappProperties.AppCredential();
        credential.setAppId("wx-agriculture");
        credential.setAppSecret("CHANGE_ME");
        properties.setApps(List.of(credential));

        assertThrows(IllegalArgumentException.class, () -> properties.requireSecret(" "));
        assertThrows(IllegalStateException.class, () -> properties.requireSecret("wx-agriculture"));
        assertThrows(IllegalStateException.class, () -> properties.requireSecret("wx-farm-task"));
    }
}
