package com.ym.agriculture.farming.satellite.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.farming.satellite.remote.SatelliteRemoteDeleteClient;
import com.ym.agriculture.farming.satellite.remote.SatelliteRemoteClient;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.concurrent.TimeUnit;

/**
 * 仅用于遥感 HTTP 的 Retrofit：在 {@link Jackson2ObjectMapperBuilder} 上构建<strong>局部</strong>{@link ObjectMapper}，
 * 继承全局 Module / 时间格式等，与 Web 行为一致。
 * <p>
 * 与外部遥感服务的字段名通过 DTO 上 {@link com.fasterxml.jackson.annotation.JsonProperty} 声明（如 {@code dk_id}），
 * 不再使用 {@code PropertyNamingStrategies.SNAKE_CASE}，避免与注解重复；<strong>新增遥感 DTO 字段时若对方仍为 snake_case，
 * 必须加 {@code @JsonProperty}</strong>，否则 JSON 键会按 Java 属性名（camelCase）写出。
 * <p>
 * <strong>禁止</strong>将该 {@code ObjectMapper} 注册为 Spring {@code @Bean}（类型冲突会拖垮全局 MVC 反序列化）。
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "satellite", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SatelliteRetrofitConfig {

    private final SatelliteProperties satelliteProperties;

    @Bean(name = "satelliteOkHttpClient")
    public OkHttpClient satelliteOkHttpClient() {
        SatelliteProperties.Timeout t = satelliteProperties.getTimeout();
        return new OkHttpClient.Builder()
            .connectTimeout(t.getConnect(), TimeUnit.SECONDS)
            .readTimeout(t.getRead(), TimeUnit.SECONDS)
            .writeTimeout(t.getWrite(), TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "satellite", name = "base-url")
    public Retrofit satelliteRetrofit(Jackson2ObjectMapperBuilder builder,
                                      @Qualifier("satelliteOkHttpClient") OkHttpClient satelliteOkHttpClient) {
        return buildRetrofit(builder, satelliteOkHttpClient, satelliteProperties.getBaseUrl());
    }

    @Bean(name = "satelliteDeleteRetrofit")
    @ConditionalOnProperty(prefix = "satellite", name = "base-url")
    public Retrofit satelliteDeleteRetrofit(Jackson2ObjectMapperBuilder builder,
                                            @Qualifier("satelliteOkHttpClient") OkHttpClient satelliteOkHttpClient) {
        String deleteBaseUrl = StringUtils.isBlank(satelliteProperties.getDeleteBaseUrl())
            ? satelliteProperties.getBaseUrl()
            : satelliteProperties.getDeleteBaseUrl();
        return buildRetrofit(builder, satelliteOkHttpClient, deleteBaseUrl);
    }

    private Retrofit buildRetrofit(Jackson2ObjectMapperBuilder builder,
                                   OkHttpClient satelliteOkHttpClient,
                                   String baseUrl) {
        ObjectMapper satelliteMapper = builder
            .createXmlMapper(false)
            .featuresToDisable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
        return new Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(satelliteOkHttpClient)
            .addConverterFactory(JacksonConverterFactory.create(satelliteMapper))
            .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "satellite", name = "base-url")
    public SatelliteRemoteClient satelliteRemoteClient(@Qualifier("satelliteRetrofit") Retrofit satelliteRetrofit) {
        return satelliteRetrofit.create(SatelliteRemoteClient.class);
    }

    @Bean
    @ConditionalOnProperty(prefix = "satellite", name = "base-url")
    public SatelliteRemoteDeleteClient satelliteRemoteDeleteClient(@Qualifier("satelliteDeleteRetrofit") Retrofit satelliteDeleteRetrofit) {
        return satelliteDeleteRetrofit.create(SatelliteRemoteDeleteClient.class);
    }
}
