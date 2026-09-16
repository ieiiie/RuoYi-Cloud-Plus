package com.ym.agriculture.farming.weather.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ym.agriculture.farming.weather.remote.AmapWeatherApi;
import lombok.RequiredArgsConstructor;
import okhttp3.HttpUrl;
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
 * 高德天气 HTTP：复用单例 {@link OkHttpClient} + {@link Retrofit}，与遥感 {@link com.ym.agriculture.farming.satellite.config.SatelliteRetrofitConfig} 一致。
 * <p>
 * {@link AmapWeatherProperties#getBaseUrl()} 须为 Retrofit 根地址（默认 {@code https://restapi.amap.com/}，以 {@code /} 结尾）。
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "amap-weather", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AmapWeatherRetrofitConfig {

    private final AmapWeatherProperties properties;

    @Bean(name = "amapWeatherOkHttpClient")
    public OkHttpClient amapWeatherOkHttpClient() {
        AmapWeatherProperties.Timeout t = properties.getTimeout();
        return new OkHttpClient.Builder()
            .connectTimeout(Math.max(1, t.getConnect()), TimeUnit.SECONDS)
            .readTimeout(Math.max(1, t.getRead()), TimeUnit.SECONDS)
            .writeTimeout(Math.max(1, t.getRead()), TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build();
    }

    @Bean(name = "amapWeatherRetrofit")
    public Retrofit amapWeatherRetrofit(Jackson2ObjectMapperBuilder builder,
                                        @Qualifier("amapWeatherOkHttpClient") OkHttpClient amapWeatherOkHttpClient) {
        ObjectMapper mapper = builder
            .createXmlMapper(false)
            .featuresToDisable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
        HttpUrl parsed = HttpUrl.parse(properties.getBaseUrl().trim());
        if (parsed == null) {
            throw new IllegalStateException("amap-weather.base-url 非法: " + properties.getBaseUrl());
        }
        String base = parsed.newBuilder().encodedPath("/").build().toString();
        return new Retrofit.Builder()
            .baseUrl(base)
            .client(amapWeatherOkHttpClient)
            .addConverterFactory(JacksonConverterFactory.create(mapper))
            .build();
    }

    @Bean
    public AmapWeatherApi amapWeatherApi(@Qualifier("amapWeatherRetrofit") Retrofit amapWeatherRetrofit) {
        return amapWeatherRetrofit.create(AmapWeatherApi.class);
    }
}
