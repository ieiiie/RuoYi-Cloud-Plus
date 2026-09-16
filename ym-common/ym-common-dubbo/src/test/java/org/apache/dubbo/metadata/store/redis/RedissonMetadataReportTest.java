package org.apache.dubbo.metadata.store.redis;

import org.apache.dubbo.common.URL;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("dev")
class RedissonMetadataReportTest {
    @Test
    void defaultsToUnprefixedDatabaseZeroWithoutAuthentication() {
        var config = RedissonMetadataReport.metadataConfig(URL.valueOf("redis://default@localhost:6379"));
        var server = config.useSingleServer();
        assertThat(server.getDatabase()).isZero();
        assertThat(server.getUsername()).isNull();
        assertThat(server.getPassword()).isNull();
        assertThat(config.getNameMapper().map("DUBBO_GROUP:mapping")).isEqualTo("DUBBO_GROUP:mapping");
    }

    @Test
    void honorsMetadataEndpointDatabaseCredentialsAndTimeout() {
        var config = RedissonMetadataReport.metadataConfig(
            URL.valueOf("redis://metadata-user:test-password@localhost:6380?database=3&timeout=12s"));
        var server = config.useSingleServer();
        assertThat(server.getAddress()).isEqualTo("redis://localhost:6380");
        assertThat(server.getDatabase()).isEqualTo(3);
        assertThat(server.getUsername()).isEqualTo("metadata-user");
        assertThat(server.getPassword()).isEqualTo("test-password");
        assertThat(server.getTimeout()).isEqualTo(12000);
    }

    @Test
    void supportsTlsAndNumericTimeout() {
        var config = RedissonMetadataReport.metadataConfig(URL.valueOf("redis://localhost:6379?ssl=true&timeout=5000"));
        assertThat(config.useSingleServer().getAddress()).isEqualTo("rediss://localhost:6379");
        assertThat(config.useSingleServer().getTimeout()).isEqualTo(5000);
    }

    @Test
    void destroysOnlyOwnedClientOnceAndCannotReconnect() {
        RedissonClient client = mock(RedissonClient.class, RETURNS_DEEP_STUBS);
        try (var redisson = mockStatic(Redisson.class)) {
            redisson.when(() -> Redisson.create(any(Config.class))).thenReturn(client);
            var report = new RedissonMetadataReport(URL.valueOf("redis://localhost:6379?cycle-report=false&file="));
            report.getConfigItem("interface", "mapping");
            report.destroy();
            report.destroy();
            verify(client, times(1)).shutdown();
            assertThatThrownBy(() -> report.getConfigItem("interface", "mapping"))
                .hasMessageContaining("Metadata report has been destroyed");
            redisson.verify(() -> Redisson.create(any(Config.class)), times(1));
        }
    }
}
