package com.ym.web.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import cn.dev33.satoken.util.SaTokenConsts;
import jakarta.servlet.http.HttpServletResponse;
import com.ym.common.core.constant.HttpStatus;
import com.ym.common.core.utils.ServletUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dbo 使用自身的独立登录态鉴权，不依赖 SaaS 网关会话。
 */
@Configuration
public class DboAuthConfiguration {

    @Bean
    public SaServletFilter dboAuthFilter() {
        return new SaServletFilter()
            .addInclude("/**")
            .addExclude(
                "/auth/code",
                "/auth/login",
                "/auth/logout",
                "/auth/binding/*",
                "/auth/register",
                "/actuator",
                "/actuator/**",
                "/error",
                "/*/v3/api-docs",
                "/*/v3/api-docs/**")
            .setAuth(obj -> StpUtil.checkLogin())
            .setError(error -> {
                HttpServletResponse response = ServletUtils.getResponse();
                response.setContentType(SaTokenConsts.CONTENT_TYPE_APPLICATION_JSON);
                String message = error instanceof NotLoginException
                    ? error.getMessage()
                    : "认证失败，无法访问系统资源";
                return SaResult.error(message).setCode(HttpStatus.UNAUTHORIZED);
            });
    }
}
