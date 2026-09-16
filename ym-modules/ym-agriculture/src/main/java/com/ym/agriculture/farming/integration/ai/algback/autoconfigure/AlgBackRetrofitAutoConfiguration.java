package com.ym.agriculture.farming.integration.ai.algback.autoconfigure;

import com.ym.agriculture.farming.integration.ai.algback.auth.AlgBackAuthService;
import com.ym.agriculture.farming.integration.ai.algback.client.AlgBackRetrofitClients;
import com.ym.agriculture.farming.integration.ai.algback.api.AlgBackApi;
import com.ym.agriculture.farming.integration.ai.algback.auth.AlgBackAuthPaths;
import com.ym.agriculture.farming.integration.ai.algback.auth.AlgBackEagerLoginInterceptor;
import com.ym.agriculture.farming.integration.ai.algback.auth.AlgBackTokenHolder;
import com.ym.agriculture.farming.integration.ai.algback.auth.AlgBackTokenInterceptor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import retrofit2.Retrofit;

import java.util.concurrent.TimeUnit;

/**
 * 算法中台 Spring Boot 自动配置：{@code ym.alg-back.base-url} 非空时注册 OkHttp、Retrofit、{@link AlgBackApi}、{@link AlgBackAuthService} 及 token 相关 Bean。
 * <p>
 * 经 {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} 加载；未配置 base-url 时不生效。
 * 可选启动自动登录、{@link AlgBackEagerLoginInterceptor} 首请求前补登、Snail Job 广播刷新与 HTTP 401 静默重登。
 * <p>
 * 适用于 Spring Boot 3+。
 *
 * @author ym-cloud
 */
@AutoConfiguration(after = YmAlgBackPropertiesConfiguration.class)
@ConditionalOnExpression("'${ym.alg-back.base-url:}' != ''")
@Slf4j
public class AlgBackRetrofitAutoConfiguration {

    @Bean
    public AlgBackTokenHolder algBackTokenHolder() {
        return new AlgBackTokenHolder();
    }

    @Bean
    public AlgBackTokenInterceptor algBackTokenInterceptor(AlgBackTokenHolder algBackTokenHolder) {
        return AlgBackRetrofitClients.tokenInterceptor(algBackTokenHolder);
    }

    @Bean
    public AlgBackEagerLoginInterceptor algBackEagerLoginInterceptor(AlgBackTokenHolder algBackTokenHolder,
                                                                     YmAlgBackProperties properties,
                                                                     ObjectProvider<AlgBackAuthService> algBackAuthServiceProvider) {
        return new AlgBackEagerLoginInterceptor(algBackTokenHolder, properties, algBackAuthServiceProvider);
    }

    @Bean(name = "algBackOkHttpClient")
    public OkHttpClient algBackOkHttpClient(YmAlgBackProperties properties,
                                            AlgBackEagerLoginInterceptor algBackEagerLoginInterceptor,
                                            AlgBackTokenInterceptor algBackTokenInterceptor,
                                            ObjectProvider<AlgBackAuthService> algBackAuthServiceProvider,
                                            AlgBackTokenHolder algBackTokenHolder) {
        YmAlgBackProperties.Timeout t = properties.getTimeout();
        return AlgBackRetrofitClients.defaultOkHttpBuilder()
            .addInterceptor(algBackEagerLoginInterceptor)
            .addInterceptor(algBackTokenInterceptor)
            .authenticator((route, response) -> algBackReauthenticate(response, properties, algBackAuthServiceProvider, algBackTokenHolder))
            .connectTimeout(t.getConnectSeconds(), TimeUnit.SECONDS)
            .readTimeout(t.getReadSeconds(), TimeUnit.SECONDS)
            .writeTimeout(t.getWriteSeconds(), TimeUnit.SECONDS)
            .build();
    }

    /**
     * HTTP 401 时用配置账号静默重登一次并重试原请求（仅一次，避免死循环）。
     */
    private okhttp3.Request algBackReauthenticate(Response response,
                                                  YmAlgBackProperties properties,
                                                  ObjectProvider<AlgBackAuthService> authServiceProvider,
                                                  AlgBackTokenHolder holder) {
        if (response.code() != 401) {
            return null;
        }
        if (AlgBackAuthPaths.isLogin(response.request().url())) {
            return null;
        }
        if (response.priorResponse() != null) {
            return null;
        }
        try {
            authServiceProvider.getObject().loginWithConfiguredCredentials(properties);
        } catch (Exception e) {
            log.warn("ym.alg-back HTTP 401 后自动重登失败", e);
            return null;
        }
        String newToken = holder.get();
        if (newToken == null || newToken.isEmpty()) {
            return null;
        }
        return response.request().newBuilder().header("token", newToken).build();
    }

    @Bean(name = "algBackRetrofit")
    public Retrofit algBackRetrofit(YmAlgBackProperties properties,
                                    @Qualifier("algBackOkHttpClient") OkHttpClient algBackOkHttpClient) {
        return AlgBackRetrofitClients.retrofit(properties.getBaseUrl().trim(), algBackOkHttpClient);
    }

    @Bean
    public AlgBackApi algBackApi(@Qualifier("algBackRetrofit") Retrofit algBackRetrofit) {
        return AlgBackRetrofitClients.api(algBackRetrofit);
    }

    @Bean
    public AlgBackAuthService algBackAuthService(AlgBackApi algBackApi, AlgBackTokenHolder algBackTokenHolder) {
        return new AlgBackAuthService(algBackApi, algBackTokenHolder);
    }

    @Bean
    @ConditionalOnProperty(prefix = "ym.alg-back.auth", name = "auto-login-on-startup", havingValue = "true")
    @ConditionalOnExpression("'${ym.alg-back.auth.username:}' != '' && '${ym.alg-back.auth.password:}' != ''")
    public ApplicationRunner algBackAutoLoginRunner(AlgBackAuthService algBackAuthService,
                                                      YmAlgBackProperties properties) {
        return args -> {
            try {
                YmAlgBackProperties.Auth a = properties.getAuth();
                algBackAuthService.login(a.getUsername(), a.getPassword());
            } catch (Exception e) {
                log.warn("ym.alg-back 启动自动登录失败", e);
            }
        };
    }
}
