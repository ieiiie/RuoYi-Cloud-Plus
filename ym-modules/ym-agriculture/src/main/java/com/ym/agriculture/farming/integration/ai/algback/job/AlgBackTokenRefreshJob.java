package com.ym.agriculture.farming.integration.ai.algback.job;

import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.ym.agriculture.farming.integration.ai.algback.auth.AlgBackAuthService;
import com.ym.agriculture.farming.integration.ai.algback.auth.AlgBackTokenHolder;
import com.ym.agriculture.farming.integration.ai.algback.autoconfigure.YmAlgBackProperties;
import com.ym.agriculture.shared.job.DomainJobExecutorSupport;
import com.ym.common.core.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/** 算法中台本地 Token 的 Snail Job 广播刷新入口。 */
@Slf4j
@Component
@RequiredArgsConstructor
@JobExecutor(name = "algBackTokenRefreshJob", method = "refresh")
public class AlgBackTokenRefreshJob {

    private final Optional<AlgBackAuthService> authService;
    private final Optional<AlgBackTokenHolder> tokenHolder;
    private final YmAlgBackProperties properties;
    private final DomainJobExecutorSupport executorSupport;

    public void refresh(JobArgs args) {
        executorSupport.executeGlobal(this::refreshLocalToken);
    }

    private void refreshLocalToken() {
        YmAlgBackProperties.TokenRefresh refresh = properties.getTokenRefresh();
        YmAlgBackProperties.Auth auth = properties.getAuth();
        if (!refresh.isEnabled() || authService.isEmpty() || tokenHolder.isEmpty()
            || auth == null || StringUtils.isBlank(auth.getUsername()) || StringUtils.isBlank(auth.getPassword())) {
            return;
        }
        long now = Instant.now().getEpochSecond();
        if (refresh.isSkipWhenJwtValid() && refresh.getRenewBeforeExpireSeconds() > 0
            && tokenHolder.get().isLikelyValidUntil(now, refresh.getRenewBeforeExpireSeconds())) {
            return;
        }
        authService.get().login(auth.getUsername().trim(), auth.getPassword());
        log.debug("ym.alg-back token refreshed by Snail Job");
    }
}
