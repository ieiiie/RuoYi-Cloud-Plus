package org.apache.dubbo.metadata.store.redis;

import com.ym.common.redis.handler.KeyPrefixHandler;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MappingListener;
import org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.redisson.Redisson;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/** 使用独立临时 Redis，禁止指向业务 Redis。 */
@Tag("dev")
@EnabledIfSystemProperty(named = "dubbo.test.redis.port", matches = "\\d+")
class RedissonMetadataIsolationTest {
    private URL metadataUrl(String group) {
        return URL.valueOf("redis://127.0.0.1:" + System.getProperty("dubbo.test.redis.port")
            + "?database=0&group=" + group + "&cycle-report=false&file=");
    }

    @Test
    void readsSharedMappingAndReceivesChangesWithoutTouchingBusinessDatabase() throws Exception {
        String group = "test-" + UUID.randomUUID();
        URL url = metadataUrl(group);
        Config businessConfig = new Config();
        businessConfig.setCodec(StringCodec.INSTANCE).setNameMapper(new KeyPrefixHandler("dbo:"));
        businessConfig.useSingleServer().setAddress("redis://127.0.0.1:" + url.getPort()).setDatabase(2);
        var business = Redisson.create(businessConfig);
        var provider = new RedissonMetadataReport(url);
        var consumer = new RedissonMetadataReport(url);
        String service = "test.SaasSystemControlService";
        try {
            business.getBucket(group).set("business-value");
            assertThat(provider.registerServiceAppMapping(service, "mapping", "ym-system", null)).isTrue();
            assertThat(consumer.getServiceAppMapping(service, url)).isEqualTo(Set.of("ym-system"));
            CountDownLatch change = new CountDownLatch(1);
            MappingListener listener = mock(MappingListener.class);
            doAnswer(invocation -> { change.countDown(); return null; }).when(listener).onEvent(any());
            consumer.getServiceAppMapping(service, listener, url);
            assertThat(provider.registerServiceAppMapping(service, "mapping", "ym-system,second", "ym-system")).isTrue();
            assertThat(change.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(consumer.getServiceAppMapping(service, url)).containsExactlyInAnyOrder("ym-system", "second");
            assertThat(business.getMap(group + ":mapping").isExists()).isFalse();
            assertThat(business.getBucket(group).get()).isEqualTo("business-value");
            consumer.destroy();
            assertThat(business.isShutdown()).isFalse();
            assertThat(business.getBucket(group).get()).isEqualTo("business-value");
        } finally {
            consumer.destroy();
            provider.destroy();
            business.shutdown();
        }
    }

    @Test
    void exchangesApplicationMetadataBetweenIndependentClients() {
        URL url = metadataUrl("test-" + UUID.randomUUID());
        var provider = new RedissonMetadataReport(url);
        var consumer = new RedissonMetadataReport(url);
        try {
            String app = "test-system-" + UUID.randomUUID();
            var metadata = new MetadataInfo(app);
            metadata.addService(URL.valueOf("dubbo://127.0.0.1:20880/test.SaasSystemControlService?application=" + app));
            metadata.calAndGetRevision();
            var id = new SubscriberMetadataIdentifier(app, metadata.getRevision());
            provider.publishAppMetadata(id, metadata);
            assertThat(consumer.getAppMetadata(id, Map.of()).getApp()).isEqualTo(app);
        } finally {
            consumer.destroy();
            provider.destroy();
        }
    }
}
