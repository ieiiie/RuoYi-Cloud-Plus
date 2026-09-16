package com.ym.agriculture.farming.integration.ai.algback.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ym.agriculture.farming.integration.ai.algback.api.AlgBackApi;
import com.ym.agriculture.farming.integration.ai.algback.auth.AlgBackTokenHolder;
import com.ym.agriculture.farming.integration.ai.algback.auth.AlgBackTokenInterceptor;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.concurrent.TimeUnit;

/**
 * 算法中台 HTTP 客户端工厂：OkHttp、Jackson、Retrofit 与 {@link AlgBackApi}。
 * <p>
 * 非 Spring 场景直接调用静态方法；Spring Boot 下由 {@link com.ym.agriculture.farming.integration.ai.algback.autoconfigure.AlgBackRetrofitAutoConfiguration} 注册 Bean。
 *
 * @author ym-cloud
 */
public final class AlgBackRetrofitClients {

    private AlgBackRetrofitClients() {
    }

    /** 默认超时：连接 15s、读 120s（视频分析链路可能较慢）、写 30s。 */
    public static OkHttpClient.Builder defaultOkHttpBuilder() {
        return new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS);
    }

    /** 与 Retrofit Jackson 转换器配套；未知 JSON 字段不反序列化失败，便于中台字段扩展。 */
    public static ObjectMapper algBackObjectMapper() {
        ObjectMapper m = new ObjectMapper();
        m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return m;
    }

    /**
     * Retrofit 要求 baseUrl 以 {@code /} 结尾；一般为 {@code .../api/}，例如 {@code https://host:port/api/}。
     *
     * @throws IllegalArgumentException {@code baseUrl} 为空
     */
    public static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalArgumentException("baseUrl is blank");
        }
        String t = baseUrl.trim();
        return t.endsWith("/") ? t : t + "/";
    }

    /** 使用 {@link #algBackObjectMapper()} 构建 Retrofit。 */
    public static Retrofit retrofit(String baseUrl, OkHttpClient httpClient) {
        return retrofit(baseUrl, httpClient, algBackObjectMapper());
    }

    /** {@code httpClient} 建议已挂载 {@link #tokenInterceptor(AlgBackTokenHolder)}（登录接口除外）。 */
    public static Retrofit retrofit(String baseUrl, OkHttpClient httpClient, ObjectMapper objectMapper) {
        return new Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(baseUrl))
            .client(httpClient)
            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
            .build();
    }

    public static AlgBackApi api(Retrofit retrofit) {
        return retrofit.create(AlgBackApi.class);
    }

    /** 一步创建 Retrofit 与 API；{@code httpClient} 须已配置 token 拦截器等。 */
    public static AlgBackApi api(String baseUrl, OkHttpClient httpClient) {
        return api(retrofit(baseUrl, httpClient));
    }

    /** 从中台登录后把 token 写入 holder，后续业务请求会自动带 Header {@code token}。 */
    public static AlgBackTokenInterceptor tokenInterceptor(AlgBackTokenHolder holder) {
        return new AlgBackTokenInterceptor(holder);
    }
}
