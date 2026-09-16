package com.ym.agriculture.shared.i18n.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.shared.i18n.client.I18nTranslationErrorCode;
import com.ym.agriculture.shared.i18n.client.I18nTranslationException;
import com.ym.agriculture.shared.i18n.client.I18nTranslationProvider;
import com.ym.agriculture.shared.i18n.config.SmartFarmingTranslationProperties;
import com.ym.agriculture.shared.i18n.dao.SfI18nTextMapper;
import com.ym.agriculture.shared.i18n.event.I18nTextsRegisteredEvent;
import com.ym.agriculture.shared.i18n.model.I18nResourceResolution;
import com.ym.agriculture.shared.i18n.model.I18nTextLookup;
import com.ym.agriculture.shared.i18n.model.I18nTextSource;
import com.ym.agriculture.shared.i18n.model.constants.I18nLocale;
import com.ym.agriculture.shared.i18n.model.constants.I18nTranslationOrigin;
import com.ym.agriculture.shared.i18n.model.constants.I18nTranslationStatus;
import com.ym.agriculture.shared.i18n.model.entity.SfI18nText;
import com.ym.agriculture.shared.i18n.model.vo.SfI18nTextVo;
import com.ym.agriculture.shared.i18n.service.ISfI18nTextService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** 租户级中文到维文词条服务。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SfI18nTextServiceImpl implements ISfI18nTextService {

    private static final int[] RETRY_MINUTES = {1, 5, 30};

    private final SfI18nTextMapper textMapper;
    private final I18nTranslationProvider translationProvider;
    private final SmartFarmingTranslationProperties properties;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerTexts(String tenantId, Collection<I18nTextSource> sources) {
        if (StringUtils.isBlank(tenantId) || CollUtil.isEmpty(sources)) {
            return;
        }
        Map<I18nResourceIdentity, I18nTextSource> requested = deduplicateSources(sources);
        registerPhrases(tenantId, requested, Map.of());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerTextsWithPreferredTranslations(String tenantId, Collection<I18nTextSource> sources,
        Map<I18nTextSource, String> preferredTranslations) {
        if (StringUtils.isBlank(tenantId) || CollUtil.isEmpty(sources)) {
            return;
        }
        Map<I18nResourceIdentity, I18nTextSource> requested = deduplicateSources(sources);
        Map<I18nResourceIdentity, String> preferredByResource = new LinkedHashMap<>();
        if (preferredTranslations != null) {
            for (Map.Entry<I18nTextSource, String> entry : preferredTranslations.entrySet()) {
                I18nTextSource source = entry.getKey();
                if (source == null || !isValidSource(source) || StringUtils.isBlank(entry.getValue())) {
                    continue;
                }
                I18nResourceIdentity identity = I18nResourceIdentity.from(source);
                I18nTextSource requestedSource = requested.get(identity);
                if (requestedSource != null && Objects.equals(requestedSource.sourceText(), source.sourceText())) {
                    preferredByResource.put(identity, entry.getValue().trim());
                }
            }
        }
        registerPhrases(tenantId, requested, preferredByResource);
    }

    @Override
    public Map<I18nTextLookup, String> resolveUyghurTexts(String tenantId, Collection<I18nTextLookup> lookups) {
        if (StringUtils.isBlank(tenantId) || CollUtil.isEmpty(lookups)) {
            return Map.of();
        }
        Set<I18nTextLookup> requested = lookups.stream()
            .filter(Objects::nonNull)
            .filter(lookup -> StringUtils.isNotBlank(lookup.sourceText()))
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<PhraseKey, SfI18nText> translated = loadExactPhrases(tenantId,
            requested.stream().map(I18nTextLookup::sourceText).toList(), true);
        Map<I18nTextLookup, String> result = new LinkedHashMap<>();
        for (I18nTextLookup lookup : requested) {
            SfI18nText row = translated.get(PhraseKey.from(lookup.sourceText()));
            if (isSuccessfulTranslation(row)) {
                result.put(lookup, row.getTranslatedText());
            }
        }
        return result;
    }

    @Override
    public Map<I18nTextSource, String> resolveUyghurTextsByResources(String tenantId,
        Collection<I18nTextSource> sources) {
        if (StringUtils.isBlank(tenantId) || CollUtil.isEmpty(sources)) {
            return Map.of();
        }
        Collection<I18nTextSource> requested = deduplicateSources(sources).values();
        Map<PhraseKey, SfI18nText> translated = loadExactPhrases(tenantId,
            requested.stream().map(I18nTextSource::sourceText).toList(), true);
        return mapTranslations(requested, translated);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public I18nResourceResolution resolveAndRegisterMissingUyghurTextsByResources(String tenantId,
        Collection<I18nTextSource> sources) {
        if (StringUtils.isBlank(tenantId) || CollUtil.isEmpty(sources)) {
            return I18nResourceResolution.empty();
        }
        Map<I18nResourceIdentity, I18nTextSource> requested = deduplicateSources(sources);
        RegistrationOutcome outcome = registerPhrases(tenantId, requested, Map.of());
        return new I18nResourceResolution(outcome.translations(), outcome.registeredCount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean correct(String tenantId, Long translationId, String translatedText, String sourceHash) {
        if (StringUtils.isBlank(tenantId) || translationId == null || StringUtils.isBlank(translatedText)
            || StringUtils.isBlank(sourceHash)) {
            return false;
        }
        return textMapper.correctTranslation(tenantId, translationId, sourceHash.trim().toLowerCase(),
            translatedText.trim(), LoginHelper.getUserId(), new Date());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean retry(String tenantId, Long translationId) {
        if (StringUtils.isBlank(tenantId) || translationId == null) {
            return false;
        }
        boolean reset = textMapper.resetForRetry(tenantId, translationId, LoginHelper.getUserId(), new Date());
        if (reset) {
            eventPublisher.publishEvent(new I18nTextsRegisteredEvent(tenantId));
        }
        return reset;
    }

    @Override
    public List<SfI18nTextVo> list(String tenantId, String status, String sourceKeyword) {
        if (StringUtils.isBlank(tenantId)) {
            return List.of();
        }
        List<SfI18nText> rows = textMapper.selectList(Wrappers.<SfI18nText>lambdaQuery()
            .eq(SfI18nText::getTenantId, tenantId)
            .eq(StringUtils.isNotBlank(status), SfI18nText::getTranslationStatus, status)
            .like(StringUtils.isNotBlank(sourceKeyword), SfI18nText::getSourceText, sourceKeyword)
            .eq(SfI18nText::getDelFlag, SystemConstants.NORMAL)
            .orderByDesc(SfI18nText::getUpdateTime)
            .orderByDesc(SfI18nText::getTranslationId));
        return MapstructUtils.convert(rows, SfI18nTextVo.class);
    }

    @Override
    public PageResult<SfI18nTextVo> page(String tenantId, String status, String sourceKeyword,
        PageQuery pageQuery) {
        if (StringUtils.isBlank(tenantId)) {
            return com.ym.agriculture.shared.common.AgriculturePageResults.build();
        }
        Page<SfI18nTextVo> page = textMapper.selectVoPage(pageQuery.build(),
            Wrappers.<SfI18nText>lambdaQuery()
                .eq(SfI18nText::getTenantId, tenantId)
                .eq(StringUtils.isNotBlank(status), SfI18nText::getTranslationStatus, status)
                .like(StringUtils.isNotBlank(sourceKeyword), SfI18nText::getSourceText, sourceKeyword)
                .eq(SfI18nText::getDelFlag, SystemConstants.NORMAL)
                .orderByDesc(SfI18nText::getUpdateTime)
                .orderByDesc(SfI18nText::getTranslationId));
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(page);
    }

    @Override
    public int processPending() {
        SmartFarmingTranslationProperties.Worker worker = properties.getWorker();
        int maxAttempts = Math.max(0, worker.getMaxRetryCount()) + 1;
        Date scanTime = new Date();
        TenantHelper.ignore(() -> textMapper.expireExhaustedLeases(scanTime, maxAttempts));
        List<SfI18nText> rows = TenantHelper.ignore(() -> textMapper.selectPendingForWorker(scanTime,
            maxAttempts, Math.max(1, worker.getBatchSize())));
        int claimed = 0;
        for (SfI18nText row : rows) {
            if (!isValidWorkerRow(row)) {
                continue;
            }
            Date claimTime = new Date();
            String token = UUID.randomUUID().toString();
            Date leaseUntil = new Date(claimTime.getTime()
                + Duration.ofMinutes(Math.max(1, worker.getLeaseMinutes())).toMillis());
            boolean accepted = TenantHelper.dynamic(row.getTenantId(), () -> textMapper.claim(row.getTenantId(),
                row.getTranslationId(), row.getSourceHash(), claimTime, leaseUntil, token, maxAttempts));
            if (!accepted) {
                continue;
            }
            claimed++;
            processClaimed(row, token, maxAttempts);
        }
        return claimed;
    }

    private RegistrationOutcome registerPhrases(String tenantId,
        Map<I18nResourceIdentity, I18nTextSource> requested,
        Map<I18nResourceIdentity, String> preferredByResource) {
        if (requested == null || requested.isEmpty()) {
            return RegistrationOutcome.empty();
        }

        Map<PhraseKey, List<I18nTextSource>> phraseSources = new LinkedHashMap<>();
        Map<PhraseKey, String> preferredByPhrase = new LinkedHashMap<>();
        for (Map.Entry<I18nResourceIdentity, I18nTextSource> entry : requested.entrySet()) {
            I18nTextSource source = entry.getValue();
            PhraseKey phrase = PhraseKey.from(source.sourceText());
            phraseSources.computeIfAbsent(phrase, ignored -> new ArrayList<>()).add(source);
            String preferred = preferredByResource.get(entry.getKey());
            if (StringUtils.isNotBlank(preferred)) {
                String previous = preferredByPhrase.putIfAbsent(phrase, preferred);
                if (previous != null && !Objects.equals(previous, preferred)) {
                    throw new IllegalArgumentException("同一租户词条不能在一次登记中包含不同优选译文");
                }
            }
        }
        validateNoInMemoryHashCollision(phraseSources.keySet());

        Set<String> hashes = phraseSources.keySet().stream().map(PhraseKey::sourceHash)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<PhraseKey, SfI18nText> existing = indexAndValidateRows(tenantId, phraseSources.keySet(),
            TenantHelper.ignore(() -> textMapper.selectBySourceHashes(tenantId, I18nLocale.UG_CN, hashes)));
        Date now = new Date();
        List<SfI18nText> rowsToUpsert = new ArrayList<>();
        for (PhraseKey phrase : phraseSources.keySet()) {
            SfI18nText row = existing.get(phrase);
            String preferred = preferredByPhrase.get(phrase);
            if (row == null) {
                rowsToUpsert.add(StringUtils.isBlank(preferred)
                    ? newPending(tenantId, phrase, now)
                    : newPreferred(tenantId, phrase, preferred, now));
                continue;
            }
            if (!SystemConstants.NORMAL.equals(row.getDelFlag())) {
                rowsToUpsert.add(StringUtils.isBlank(preferred) || isSuccessfulTranslation(row)
                    ? newPending(tenantId, phrase, now)
                    : newPreferred(tenantId, phrase, preferred, now));
            } else if (!isSuccessfulTranslation(row) && StringUtils.isNotBlank(preferred)) {
                rowsToUpsert.add(newPreferred(tenantId, phrase, preferred, now));
            }
        }

        if (!rowsToUpsert.isEmpty()) {
            TenantHelper.ignore(() -> textMapper.upsertPhraseBatch(rowsToUpsert));
        }
        Map<PhraseKey, SfI18nText> finalRows = indexAndValidateRows(tenantId, phraseSources.keySet(),
            TenantHelper.ignore(() -> textMapper.selectBySourceHashes(tenantId, I18nLocale.UG_CN, hashes)));
        for (PhraseKey phrase : phraseSources.keySet()) {
            SfI18nText row = finalRows.get(phrase);
            if (row == null || !SystemConstants.NORMAL.equals(row.getDelFlag())) {
                throw new IllegalStateException("租户翻译词条登记失败");
            }
        }
        boolean requiresWorker = !rowsToUpsert.isEmpty() && finalRows.values().stream().anyMatch(row ->
            SystemConstants.NORMAL.equals(row.getDelFlag())
                && I18nTranslationStatus.PENDING.equals(row.getTranslationStatus())
                && row.getNextRetryTime() != null && !row.getNextRetryTime().after(now));
        if (requiresWorker) {
            eventPublisher.publishEvent(new I18nTextsRegisteredEvent(tenantId));
        }
        return new RegistrationOutcome(mapTranslations(requested.values(), finalRows), rowsToUpsert.size());
    }

    private Map<PhraseKey, SfI18nText> loadExactPhrases(String tenantId, Collection<String> sourceTexts,
        boolean onlySuccessful) {
        if (sourceTexts == null || sourceTexts.isEmpty()) {
            return Map.of();
        }
        Set<PhraseKey> phrases = sourceTexts.stream().filter(StringUtils::isNotBlank)
            .map(PhraseKey::from).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> hashes = phrases.stream().map(PhraseKey::sourceHash).collect(Collectors.toSet());
        Map<PhraseKey, SfI18nText> rows = indexAndValidateRows(tenantId, phrases,
            TenantHelper.ignore(() -> textMapper.selectBySourceHashes(tenantId, I18nLocale.UG_CN, hashes)));
        if (!onlySuccessful) {
            return rows;
        }
        rows.entrySet().removeIf(entry -> !SystemConstants.NORMAL.equals(entry.getValue().getDelFlag())
            || !isSuccessfulTranslation(entry.getValue()));
        return rows;
    }

    private static void validateNoInMemoryHashCollision(Collection<PhraseKey> phrases) {
        Map<String, String> sourceByHash = new LinkedHashMap<>();
        for (PhraseKey phrase : phrases) {
            String previous = sourceByHash.putIfAbsent(phrase.sourceHash(), phrase.sourceText());
            if (previous != null && !Objects.equals(previous, phrase.sourceText())) {
                throw new IllegalStateException(I18nTranslationErrorCode.SOURCE_HASH_COLLISION);
            }
        }
    }

    private static Map<PhraseKey, SfI18nText> indexAndValidateRows(String tenantId, Collection<PhraseKey> requested,
        Collection<SfI18nText> rows) {
        Map<String, Set<String>> requestedTextByHash = requested.stream().collect(Collectors.groupingBy(
            PhraseKey::sourceHash, LinkedHashMap::new,
            Collectors.mapping(PhraseKey::sourceText, Collectors.toCollection(LinkedHashSet::new))));
        Map<PhraseKey, SfI18nText> indexed = new LinkedHashMap<>();
        if (rows == null) {
            return indexed;
        }
        for (SfI18nText row : rows) {
            if (!Objects.equals(tenantId, row.getTenantId()) || !Objects.equals(I18nLocale.UG_CN, row.getLocale())
                || StringUtils.isBlank(row.getSourceHash()) || row.getSourceText() == null) {
                continue;
            }
            Set<String> expectedTexts = requestedTextByHash.get(row.getSourceHash());
            if (expectedTexts == null) {
                continue;
            }
            if (!expectedTexts.contains(row.getSourceText())) {
                throw new IllegalStateException(I18nTranslationErrorCode.SOURCE_HASH_COLLISION);
            }
            PhraseKey key = new PhraseKey(row.getSourceHash(), row.getSourceText());
            SfI18nText previous = indexed.putIfAbsent(key, row);
            if (previous != null && !Objects.equals(previous.getTranslationId(), row.getTranslationId())) {
                throw new IllegalStateException("租户翻译词条唯一性异常");
            }
        }
        return indexed;
    }

    private static Map<I18nTextSource, String> mapTranslations(Collection<I18nTextSource> sources,
        Map<PhraseKey, SfI18nText> rows) {
        Map<I18nTextSource, String> result = new LinkedHashMap<>();
        for (I18nTextSource source : sources) {
            SfI18nText row = rows.get(PhraseKey.from(source.sourceText()));
            if (SystemConstants.NORMAL.equals(row == null ? null : row.getDelFlag())
                && isSuccessfulTranslation(row)) {
                result.put(source, row.getTranslatedText());
            }
        }
        return result;
    }

    private void processClaimed(SfI18nText row, String token, int maxAttempts) {
        String translatedText;
        try {
            incrementMetric("sf.i18n.translation.provider.call");
            translatedText = translationProvider.translateZhToUyghur(row.getSourceText());
        } catch (Exception e) {
            int attempt = (row.getRetryCount() == null ? 0 : row.getRetryCount()) + 1;
            Date failureTime = new Date();
            I18nTranslationException failure = toTranslationFailure(e);
            String errorCode = failure.getErrorCode();
            String message = StrUtil.subWithLength(
                StrUtil.blankToDefault(failure.getMessage(), "翻译服务调用失败"), 0, 500);
            boolean written = TenantHelper.dynamic(row.getTenantId(), () -> textMapper.markFailure(
                row.getTenantId(), row.getTranslationId(), token, row.getSourceHash(), attempt, maxAttempts,
                failure.isRetryable(), nextRetryTime(attempt, failureTime), errorCode, message, failureTime));
            if (written) {
                incrementMetric("sf.i18n.translation.phrase.failure");
            }
            log.warn("业务文本翻译失败 translationId={}, tenantId={}, errorCode={}", row.getTranslationId(),
                row.getTenantId(), errorCode);
            return;
        }

        // 数据库写回异常不是翻译服务失败。让异常上抛并保留 PROCESSING 租约，
        // 后续由租约恢复重新处理，避免把基础设施故障误标为不可重试的 FAILED。
        boolean written = TenantHelper.dynamic(row.getTenantId(), () -> textMapper.markSuccess(
            row.getTenantId(), row.getTranslationId(), token, row.getSourceHash(), translatedText,
            translationProvider.providerName(), new Date()));
        if (written) {
            incrementMetric("sf.i18n.translation.phrase.success");
        }
    }

    private SfI18nText newPending(String tenantId, PhraseKey phrase, Date now) {
        SfI18nText row = basePhrase(tenantId, phrase, now);
        row.setTranslationStatus(I18nTranslationStatus.PENDING);
        row.setTranslationOrigin(I18nTranslationOrigin.MACHINE);
        row.setRetryCount(0);
        row.setNextRetryTime(now);
        return row;
    }

    private SfI18nText newPreferred(String tenantId, PhraseKey phrase, String translatedText, Date now) {
        SfI18nText row = basePhrase(tenantId, phrase, now);
        row.setTranslatedText(translatedText);
        row.setTranslationStatus(I18nTranslationStatus.SUCCESS);
        row.setTranslationOrigin(I18nTranslationOrigin.MACHINE);
        row.setRetryCount(0);
        row.setProvider("dictionary-snapshot");
        return row;
    }

    private SfI18nText basePhrase(String tenantId, PhraseKey phrase, Date now) {
        Long operator = LoginHelper.getUserId();
        SfI18nText row = new SfI18nText();
        row.setTranslationId(IdWorker.getId());
        row.setTenantId(tenantId);
        row.setLocale(I18nLocale.UG_CN);
        row.setSourceText(phrase.sourceText());
        row.setSourceHash(phrase.sourceHash());
        row.setCreateBy(operator);
        row.setCreateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now));
        row.setUpdateBy(operator);
        row.setUpdateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now));
        row.setDelFlag(SystemConstants.NORMAL);
        return row;
    }

    private static boolean isValidSource(I18nTextSource source) {
        // Change 015 后持久化身份只由 tenant + locale + 原文决定；资源元数据仅用于调用方定位，
        // 不能再成为登记或响应本地化的必要条件。
        return StringUtils.isNotBlank(source.sourceText());
    }

    private static Map<I18nResourceIdentity, I18nTextSource> deduplicateSources(
        Collection<I18nTextSource> sources) {
        Map<I18nResourceIdentity, I18nTextSource> unique = new LinkedHashMap<>();
        for (I18nTextSource source : sources) {
            if (source == null || !isValidSource(source)) {
                continue;
            }
            I18nResourceIdentity identity = I18nResourceIdentity.from(source);
            unique.putIfAbsent(identity, source);
        }
        return unique;
    }

    private static boolean isSuccessfulTranslation(SfI18nText row) {
        return row != null && I18nTranslationStatus.SUCCESS.equals(row.getTranslationStatus())
            && StringUtils.isNotBlank(row.getTranslatedText());
    }

    private static boolean isValidWorkerRow(SfI18nText row) {
        return row != null && row.getTranslationId() != null && StringUtils.isNotBlank(row.getTenantId())
            && StringUtils.isNotBlank(row.getLocale()) && StringUtils.isNotBlank(row.getSourceHash())
            && row.getSourceText() != null;
    }

    private Date nextRetryTime(int attempt, Date now) {
        int index = Math.min(Math.max(1, attempt), RETRY_MINUTES.length) - 1;
        return new Date(now.getTime() + Duration.ofMinutes(RETRY_MINUTES[index]).toMillis());
    }

    private static I18nTranslationException toTranslationFailure(Exception exception) {
        if (exception instanceof I18nTranslationException translationException) {
            return translationException;
        }
        return new I18nTranslationException(I18nTranslationErrorCode.UNKNOWN, false, "翻译服务调用失败",
            exception);
    }

    private void incrementMetric(String metricName) {
        MeterRegistry registry = meterRegistryProvider.getIfAvailable();
        if (registry != null) {
            registry.counter(metricName).increment();
        }
    }

    private static String sha256(String sourceText) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(sourceText.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("业务文本摘要计算失败", e);
        }
    }

    private record PhraseKey(String sourceHash, String sourceText) {
        private static PhraseKey from(String sourceText) {
            return new PhraseKey(sha256(sourceText), sourceText);
        }
    }

    private record I18nResourceIdentity(String resourceType, Long resourceId, String fieldKey, String sourceText) {
        private static I18nResourceIdentity from(I18nTextSource source) {
            return new I18nResourceIdentity(source.resourceType(), source.resourceId(), source.fieldKey(),
                source.sourceText());
        }
    }

    private record RegistrationOutcome(Map<I18nTextSource, String> translations, int registeredCount) {
        private static RegistrationOutcome empty() {
            return new RegistrationOutcome(Map.of(), 0);
        }
    }
}
