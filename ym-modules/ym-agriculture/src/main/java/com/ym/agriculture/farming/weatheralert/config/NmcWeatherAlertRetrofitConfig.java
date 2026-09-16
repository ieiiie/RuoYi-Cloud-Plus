package com.ym.agriculture.farming.weatheralert.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ym.agriculture.farming.weatheralert.remote.NmcWeatherAlertApi;
import lombok.RequiredArgsConstructor;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 中央气象台预警 HTTP 客户端：固定主机、超时与响应体限制。
 */
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "nmc-weather-alert", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NmcWeatherAlertRetrofitConfig {

    private static final long MAX_BODY_BYTES = 2L * 1024 * 1024;

    private final NmcWeatherAlertProperties properties;

    @Bean(name = "nmcWeatherAlertOkHttpClient")
    public OkHttpClient nmcWeatherAlertOkHttpClient() {
        NmcWeatherAlertProperties.Timeout t = properties.getTimeout();
        String allowedHost = properties.getAllowedHost() == null ? "www.nmc.cn" : properties.getAllowedHost().trim().toLowerCase();
        Interceptor hostGuard = chain -> {
            HttpUrl url = chain.request().url();
            String host = url.host().toLowerCase();
            if (!allowedHost.equals(host)) {
                throw new IOException("拒绝访问非预期主机: " + host);
            }
            Response response = chain.proceed(chain.request());
            okhttp3.ResponseBody body = response.body();
            if (body != null && body.contentLength() > MAX_BODY_BYTES) {
                body.close();
                throw new IOException("响应体过大: " + body.contentLength());
            }
            return response;
        };
        return new OkHttpClient.Builder()
            .connectTimeout(Math.max(1, t.getConnect()), TimeUnit.SECONDS)
            .readTimeout(Math.max(1, t.getRead()), TimeUnit.SECONDS)
            .writeTimeout(Math.max(1, t.getRead()), TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .followRedirects(false)
            .followSslRedirects(false)
            .addInterceptor(hostGuard)
            .build();
    }

    @Bean(name = "nmcWeatherAlertRetrofit")
    public Retrofit nmcWeatherAlertRetrofit(Jackson2ObjectMapperBuilder builder,
                                            @Qualifier("nmcWeatherAlertOkHttpClient") OkHttpClient client) {
        ObjectMapper mapper = builder
            .createXmlMapper(false)
            .featuresToDisable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
        HttpUrl parsed = HttpUrl.parse(properties.getBaseUrl().trim());
        if (parsed == null) {
            throw new IllegalStateException("nmc-weather-alert.base-url 非法: " + properties.getBaseUrl());
        }
        String base = parsed.newBuilder().encodedPath("/").build().toString();
        return new Retrofit.Builder()
            .baseUrl(base)
            .client(client)
            .addConverterFactory(JacksonConverterFactory.create(mapper))
            .build();
    }

    @Bean
    public NmcWeatherAlertApi nmcWeatherAlertApi(@Qualifier("nmcWeatherAlertRetrofit") Retrofit retrofit) {
        return retrofit.create(NmcWeatherAlertApi.class);
    }
}
