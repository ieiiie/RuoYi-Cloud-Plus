package com.ym.agriculture.farmtask.voice.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.shared.i18n.client.I18nTranslationException;
import com.ym.agriculture.shared.i18n.model.I18nTextSource;
import com.ym.agriculture.shared.i18n.model.constants.I18nLocale;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import com.ym.agriculture.shared.i18n.service.ISfI18nTextService;
import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import com.ym.agriculture.shared.i18n.StaskMessageResolver;
import com.ym.agriculture.farmtask.voice.client.XfyunClientException;
import com.ym.agriculture.farmtask.voice.client.XfyunTranslateClient;
import com.ym.agriculture.farmtask.voice.client.XfyunTtsClient;
import com.ym.agriculture.farmtask.voice.config.YmStaskVoiceProperties;
import com.ym.agriculture.farmtask.voice.dao.SfStaskVoiceBroadcastMapper;
import com.ym.agriculture.farmtask.voice.model.constants.StaskVoiceBroadcastStatus;
import com.ym.agriculture.farmtask.voice.model.entity.SfStaskVoiceBroadcast;
import com.ym.agriculture.farmtask.voice.model.vo.SfStaskVoiceBroadcastVo;
import com.ym.agriculture.farmtask.voice.service.ISfStaskVoiceBroadcastService;
import com.ym.agriculture.farmtask.voice.support.StaskVoiceMasterOssAccessor;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderMapper;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeAccessor;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import com.ym.resource.api.domain.RemoteFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * stask 任务维语语音播报服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SfStaskVoiceBroadcastServiceImpl implements ISfStaskVoiceBroadcastService {

    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String AUDIO_CONTENT_TYPE = "audio/mpeg";
    private static final String NEW_LANGUAGE = I18nLocale.UG_CN;
    private static final List<String> COMPATIBLE_LANGUAGES = List.of(NEW_LANGUAGE, "ug_CN", "ug", "uy");
    private static final List<String> LEGACY_LANGUAGES = List.of("ug_CN", "ug", "uy");
    private static final String CONTENT_CHANGED_CODE = "VOICE_CONTENT_CHANGED";
    private static final String RETRY_EXHAUSTED_CODE = "VOICE_RETRY_EXHAUSTED";
    private static final String TRANSLATION_LOOKUP_FAILED_CODE = "VOICE_TRANSLATION_LOOKUP_FAILED";
    private static final String TRANSLATION_EMPTY_CODE = "VOICE_TRANSLATION_EMPTY";
    private static final String STORAGE_FAILED_CODE = "VOICE_STORAGE_FAILED";
    private static final String STORAGE_RESULT_INVALID_CODE = "VOICE_STORAGE_RESULT_INVALID";
    private static final String INTERNAL_ERROR_CODE = "VOICE_INTERNAL_ERROR";
    private static final int[] DEFAULT_RETRY_DELAYS = {1, 5, 30};

    private final StaskMessageResolver messages;
    private final SfStaskVoiceBroadcastMapper broadcastMapper;
    private final SfStaskWorkOrderMapper workOrderMapper;
    private final SfStaskEmployeeAccessor employeeAccessor;
    private final ISfI18nTextService i18nTextService;
    private final XfyunTranslateClient translateClient;
    private final XfyunTtsClient ttsClient;
    private final StaskVoiceMasterOssAccessor ossAccessor;
    private final YmStaskVoiceProperties properties;

    @Override
    public void preGenerateForOrder(Long orderId) {
        try {
            enqueueForOrder(orderId);
        } catch (Exception e) {
            log.warn("stask voice enqueue failed orderId={}, errorType={}", orderId,
                e.getClass().getSimpleName());
        }
    }

    @Override
    public int processPendingBroadcasts() {
        Date scanTime = new Date();
        RetryWindows scanWindows = retryWindows(scanTime);
        TenantHelper.ignore(() -> broadcastMapper.failExpiredExhausted(scanWindows.processingBefore(),
            maxAttempts(), scanTime, RETRY_EXHAUSTED_CODE));
        List<SfStaskVoiceBroadcast> rows = TenantHelper.ignore(() -> broadcastMapper.selectPendingForWorker(
            scanWindows.processingBefore(), scanWindows.retryOneBefore(), scanWindows.retryTwoBefore(),
            scanWindows.retryThreeBefore(), maxAttempts(), workerBatchSize()));
        int claimed = 0;
        for (SfStaskVoiceBroadcast row : rows) {
            try {
                if (StringUtils.isBlank(row.getTenantId())) {
                    continue;
                }
                RetryWindows claimWindows = retryWindows(new Date());
                boolean accepted = TenantHelper.dynamic(row.getTenantId(), () -> claimAndProcess(
                    row.getTenantId(), row.getBroadcastId(), claimWindows));
                if (accepted) {
                    claimed++;
                }
            } catch (Exception e) {
                log.warn("stask voice worker failed broadcastId={}, errorType={}", row.getBroadcastId(),
                    e.getClass().getSimpleName());
            }
        }
        if (!rows.isEmpty()) {
            log.info("stask voice worker scan completed candidates={}, claimed={}", rows.size(), claimed);
        }
        return claimed;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public SfStaskVoiceBroadcastVo queryLatestForOrder(Long orderId) {
        SfStaskWorkOrder order = workOrderMapper.selectById(orderId);
        if (order == null || !Objects.equals(order.getTenantId(), requireTenantId())) {
            return notGenerated(orderId);
        }
        VoiceDraft currentDraft = loadVoiceDraft(order, false);
        String currentHash = fingerprint(currentDraft);
        SfStaskVoiceBroadcast current = broadcastMapper.selectLatestForOrder(order.getTenantId(), orderId,
            COMPATIBLE_LANGUAGES, currentHash);
        if (current != null && StaskVoiceBroadcastStatus.SUCCESS.equals(current.getStatus())) {
            return toVo(current);
        }
        SfStaskVoiceBroadcast legacy = broadcastMapper.selectSuccessByOrderAndHash(order.getTenantId(), orderId,
            LEGACY_LANGUAGES, legacyFingerprint(currentDraft));
        if (legacy != null) {
            if (current == null) {
                try {
                    enqueueForOrder(orderId);
                } catch (Exception e) {
                    log.warn("stask voice legacy migration enqueue failed orderId={}, errorType={}", orderId,
                        e.getClass().getSimpleName());
                }
            }
            return toVo(legacy);
        }
        return current == null ? enqueueForOrder(orderId) : toVo(current);
    }

    /**
     * 为当前工单内容和翻译快照建立可幂等领取的语音记录。
     */
    protected SfStaskVoiceBroadcastVo enqueueForOrder(Long orderId) {
        SfStaskWorkOrder order = workOrderMapper.selectById(orderId);
        if (order == null) {
            throw messages.exception(StaskMessageKeys.ERROR_WORKORDER_NOT_FOUND);
        }
        VoiceDraft draft = loadVoiceDraft(order, false);
        if (StringUtils.isBlank(draft.sourceText())) {
            throw messages.exception(StaskMessageKeys.ERROR_VOICE_BROADCAST_CONTENT_EMPTY);
        }
        String textHash = fingerprint(draft);
        SfStaskVoiceBroadcast success = broadcastMapper.selectSuccessByOrderAndHash(order.getTenantId(), orderId,
            COMPATIBLE_LANGUAGES, textHash);
        if (success != null) {
            return toVo(success);
        }
        SfStaskVoiceBroadcast row = broadcastMapper.selectByOrderAndHash(order.getTenantId(), orderId, NEW_LANGUAGE,
            textHash);
        Date now = new Date();
        if (row == null) {
            row = new SfStaskVoiceBroadcast();
            row.setBroadcastId(IdWorker.getId());
            row.setTenantId(order.getTenantId());
            row.setOrderId(orderId);
            row.setSourceText(draft.sourceText());
            row.setTextHash(textHash);
            row.setLanguage(NEW_LANGUAGE);
            row.setVoiceName(properties.getVoiceName());
            row.setStatus(StaskVoiceBroadcastStatus.QUEUED);
            row.setRetryCount(0);
            row.setCreateTime(now);
            row.setUpdateTime(now);
            try {
                broadcastMapper.insert(row);
            } catch (DuplicateKeyException e) {
                SfStaskVoiceBroadcast existing = broadcastMapper.selectByOrderAndHash(order.getTenantId(), orderId,
                    NEW_LANGUAGE, textHash);
                if (existing != null) {
                    return toVo(existing);
                }
                throw e;
            }
        } else if (!StaskVoiceBroadcastStatus.SUCCESS.equals(row.getStatus())
            && !StaskVoiceBroadcastStatus.PROCESSING.equals(row.getStatus())
            && !StaskVoiceBroadcastStatus.GENERATING.equals(row.getStatus())) {
            broadcastMapper.resetForQueue(row.getBroadcastId(), draft.sourceText(), properties.getVoiceName(), now);
            row.setSourceText(draft.sourceText());
            row.setVoiceName(properties.getVoiceName());
            row.setStatus(StaskVoiceBroadcastStatus.QUEUED);
            row.setFailReason(null);
            row.setRetryCount(0);
            row.setUpdateTime(now);
        }
        return toVo(row);
    }

    private boolean claimAndProcess(String tenantId, Long broadcastId, RetryWindows windows) {
        Date claimTime = new Date();
        boolean claimed = broadcastMapper.claimForProcessing(tenantId, broadcastId,
            windows.processingBefore(), windows.retryOneBefore(), windows.retryTwoBefore(),
            windows.retryThreeBefore(), maxAttempts(), claimTime) > 0;
        if (!claimed) {
            return false;
        }
        SfStaskVoiceBroadcast row = broadcastMapper.selectById(broadcastId);
        if (row == null) {
            return true;
        }
        int attempt = Math.max(1, Objects.requireNonNullElse(row.getRetryCount(), 1));
        try {
            SfStaskWorkOrder order = workOrderMapper.selectById(row.getOrderId());
            if (order == null) {
                throw new VoiceProcessingException(CONTENT_CHANGED_CODE, false);
            }
            VoiceDraft draft = loadVoiceDraft(order, true);
            String currentHash = fingerprint(draft);
            if (!Objects.equals(row.getTextHash(), currentHash)) {
                broadcastMapper.failAttempt(row.getBroadcastId(), attempt, maxAttempts(), CONTENT_CHANGED_CODE,
                    new Date());
                enqueueCurrentDraft(row.getOrderId());
                return true;
            }

            String uyghurText = buildUyghurScript(draft, true);
            if (!broadcastMapper.markGenerating(row.getBroadcastId(), attempt, uyghurText, new Date())) {
                return true;
            }
            byte[] audio = ttsClient.synthesizeUyghur(uyghurText);
            RemoteFile oss = uploadAudio(row, audio);
            broadcastMapper.completeSuccess(row.getBroadcastId(), attempt, uyghurText, oss.getOssId(), oss.getUrl(),
                new Date());
        } catch (Exception e) {
            VoiceFailure failure = classifyFailure(e);
            int persistedAttempt = failure.retryable() && attempt < maxAttempts() ? attempt : maxAttempts();
            broadcastMapper.failAttempt(row.getBroadcastId(), attempt, persistedAttempt, failure.errorCode(),
                new Date());
        }
        return true;
    }

    private void enqueueCurrentDraft(Long orderId) {
        try {
            enqueueForOrder(orderId);
        } catch (Exception e) {
            log.warn("stask voice refresh enqueue failed orderId={}, errorType={}", orderId,
                e.getClass().getSimpleName());
        }
    }

    private RemoteFile uploadAudio(SfStaskVoiceBroadcast row, byte[] audio) {
        try {
            RemoteFile oss = ossAccessor.uploadBytes(row.getTenantId(), audio, audioFileName(row), AUDIO_CONTENT_TYPE);
            if (oss == null || oss.getOssId() == null || StringUtils.isBlank(oss.getUrl())) {
                throw new VoiceProcessingException(STORAGE_RESULT_INVALID_CODE, false);
            }
            return oss;
        } catch (VoiceProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new VoiceProcessingException(STORAGE_FAILED_CODE, true, e);
        }
    }

    private VoiceDraft loadVoiceDraft(SfStaskWorkOrder order, boolean strictTranslationLookup) {
        String planDate = formatDate(order.getPlanDate());
        String leaderName = resolveLeaderName(order.getLeaderId());
        List<I18nTextSource> sources = new ArrayList<>();
        VoiceField greenhouse = voiceField(order, "greenhouseNameSnapshot", order.getGreenhouseNameSnapshot(),
            StaskMessageKeys.VOICE_GREENHOUSE, sources);
        VoiceField workItem = voiceField(order, "workItemNameSnapshot", order.getWorkItemNameSnapshot(),
            StaskMessageKeys.VOICE_WORK_ITEM, sources);
        VoiceField managerRequirement = voiceField(order, "managerRequirement", order.getManagerRequirement(),
            StaskMessageKeys.VOICE_MANAGER_REQUIREMENT, sources);
        VoiceField techInstruction = voiceField(order, "techInstruction", order.getTechInstruction(),
            StaskMessageKeys.VOICE_TECH_INSTRUCTION, sources);
        VoiceField overallTechNote = voiceField(order, "overallTechNote", order.getOverallTechNote(),
            StaskMessageKeys.VOICE_OVERALL_TECH_NOTE, sources);
        Map<I18nTextSource, String> translations = resolveTranslations(order, sources, strictTranslationLookup);
        VoiceDraft draft = new VoiceDraft(planDate, leaderName,
            withTranslation(greenhouse, translations),
            withTranslation(workItem, translations),
            withTranslation(managerRequirement, translations),
            withTranslation(techInstruction, translations),
            withTranslation(overallTechNote, translations), null, null);
        String sourceText = buildChineseScript(draft);
        String uyghurPreview = buildUyghurScript(draft, false);
        return draft.withScripts(sourceText, uyghurPreview);
    }

    private Map<I18nTextSource, String> resolveTranslations(SfStaskWorkOrder order, List<I18nTextSource> sources,
        boolean strictTranslationLookup) {
        if (sources.isEmpty()) {
            return Map.of();
        }
        try {
            return i18nTextService.resolveUyghurTextsByResources(order.getTenantId(), sources);
        } catch (Exception e) {
            log.warn("stask voice translation lookup failed orderId={}, errorType={}", order.getOrderId(),
                e.getClass().getSimpleName());
            if (strictTranslationLookup) {
                throw new VoiceProcessingException(TRANSLATION_LOOKUP_FAILED_CODE, true, e);
            }
            return Map.of();
        }
    }

    private VoiceField voiceField(SfStaskWorkOrder order, String fieldKey, String sourceText, String labelKey,
        List<I18nTextSource> sources) {
        I18nTextSource source = null;
        if (StringUtils.isNotBlank(sourceText)) {
            source = new I18nTextSource(I18nResourceType.STASK_WORK_ORDER, order.getOrderId(), fieldKey, sourceText);
            sources.add(source);
        }
        return new VoiceField(labelKey, sourceText, source, null);
    }

    private static VoiceField withTranslation(VoiceField field, Map<I18nTextSource, String> translations) {
        if (field.source() == null) {
            return field;
        }
        return field.withTranslation(translations.get(field.source()));
    }

    private String buildChineseScript(VoiceDraft draft) {
        StringBuilder script = new StringBuilder();
        appendLine(script, messages.chinese(StaskMessageKeys.VOICE_PLAN_DATE), draft.planDate());
        appendChineseField(script, draft.greenhouse());
        appendChineseField(script, draft.workItem());
        appendLine(script, messages.chinese(StaskMessageKeys.VOICE_LEADER), draft.leaderName());
        appendChineseField(script, draft.managerRequirement());
        appendChineseField(script, draft.techInstruction());
        appendChineseField(script, draft.overallTechNote());
        return script.toString().trim();
    }

    private String buildUyghurScript(VoiceDraft draft, boolean translateMissing) {
        StringBuilder script = new StringBuilder();
        appendLine(script, messages.uyghur(StaskMessageKeys.VOICE_PLAN_DATE), draft.planDate());
        appendUyghurField(script, draft.greenhouse(), translateMissing);
        appendUyghurField(script, draft.workItem(), translateMissing);
        appendLine(script, messages.uyghur(StaskMessageKeys.VOICE_LEADER), draft.leaderName());
        appendUyghurField(script, draft.managerRequirement(), translateMissing);
        appendUyghurField(script, draft.techInstruction(), translateMissing);
        appendUyghurField(script, draft.overallTechNote(), translateMissing);
        return script.toString().trim();
    }

    private void appendChineseField(StringBuilder script, VoiceField field) {
        appendLine(script, messages.chinese(field.labelKey()), field.sourceText());
    }

    private void appendUyghurField(StringBuilder script, VoiceField field, boolean translateMissing) {
        String label = messages.uyghur(field.labelKey());
        String value = translateMissing ? resolveUyghurValue(field) : field.previewValue();
        appendLine(script, label, value);
    }

    private String resolveUyghurValue(VoiceField field) {
        if (StringUtils.isBlank(field.sourceText()) || StringUtils.isNotBlank(field.translation())) {
            return StringUtils.isNotBlank(field.translation()) ? field.translation() : field.sourceText();
        }
        String translated = translateClient.translateZhToUyghur(field.sourceText());
        if (StringUtils.isBlank(translated)) {
            throw new VoiceProcessingException(TRANSLATION_EMPTY_CODE, false);
        }
        return translated;
    }

    private String resolveLeaderName(Long leaderId) {
        if (leaderId == null) {
            return null;
        }
        List<SysEmployeeVo> employees = employeeAccessor.queryByIds(List.of(leaderId));
        if (employees == null || employees.isEmpty()) {
            return null;
        }
        return employees.get(0).getName();
    }

    private static void appendLine(StringBuilder script, String label, String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        if (!script.isEmpty()) {
            script.append('\n');
        }
        script.append(label).append('：').append(value.trim());
    }

    private static String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return DATE_FORMAT.format(date.toInstant().atZone(CHINA_ZONE).toLocalDate());
    }

    private String fingerprint(VoiceDraft draft) {
        String material = draft.sourceText() + "\n\u001e" + draft.uyghurPreview() + "\n\u001e"
            + Objects.toString(properties.getVoiceName(), "") + "\n\u001e"
            + Objects.toString(properties.getCacheVersion(), "");
        return sha256Hex(material);
    }

    private String legacyFingerprint(VoiceDraft draft) {
        return sha256Hex(draft.sourceText());
    }

    private String audioFileName(SfStaskVoiceBroadcast row) {
        return "stask/voice/" + row.getTenantId() + "/" + row.getOrderId() + "-" + row.getTextHash() + ".mp3";
    }

    private RetryWindows retryWindows(Date now) {
        YmStaskVoiceProperties.Worker worker = properties.getWorker();
        Date processingBefore = before(now, Math.max(1, worker.getProcessingTimeoutMinutes()));
        return new RetryWindows(processingBefore,
            before(now, retryDelayMinutes(0)),
            before(now, retryDelayMinutes(1)),
            before(now, retryDelayMinutes(2)));
    }

    private int retryDelayMinutes(int index) {
        List<Integer> configured = properties.getWorker().getRetryDelayMinutes();
        if (configured == null || configured.size() <= index || configured.get(index) == null) {
            return DEFAULT_RETRY_DELAYS[index];
        }
        return Math.max(0, configured.get(index));
    }

    private static Date before(Date now, int minutes) {
        return new Date(now.getTime() - Duration.ofMinutes(minutes).toMillis());
    }

    private int workerBatchSize() {
        return Math.max(1, properties.getWorker().getBatchSize());
    }

    private int maxAttempts() {
        return Math.max(1, Math.max(0, properties.getWorker().getMaxRetryCount()) + 1);
    }

    private static SfStaskVoiceBroadcastVo notGenerated(Long orderId) {
        SfStaskVoiceBroadcastVo vo = new SfStaskVoiceBroadcastVo();
        vo.setOrderId(orderId);
        vo.setStatus(StaskVoiceBroadcastStatus.NOT_GENERATED);
        return vo;
    }

    private SfStaskVoiceBroadcastVo toVo(SfStaskVoiceBroadcast row) {
        SfStaskVoiceBroadcastVo vo = new SfStaskVoiceBroadcastVo();
        vo.setBroadcastId(row.getBroadcastId());
        vo.setOrderId(row.getOrderId());
        vo.setSourceText(row.getSourceText());
        vo.setUyghurText(row.getUyghurText());
        vo.setOssId(row.getOssId());
        vo.setAudioUrl(row.getAudioUrl());
        vo.setLanguage(row.getLanguage());
        vo.setVoiceName(row.getVoiceName());
        vo.setStatus(row.getStatus());
        if (StaskVoiceBroadcastStatus.FAILED.equals(row.getStatus())) {
            vo.setFailReason(messages.message(StaskMessageKeys.VOICE_GENERATION_FAILED));
        }
        return vo;
    }

    private String sha256Hex(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw messages.exception(StaskMessageKeys.ERROR_VOICE_CONTENT_HASH_FAILED);
        }
    }

    private String requireTenantId() {
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            throw messages.exception(StaskMessageKeys.ERROR_COMMON_TENANT_CONTEXT_MISSING);
        }
        return tenantId;
    }

    private static VoiceFailure classifyFailure(Exception failure) {
        if (failure instanceof I18nTranslationException translationFailure) {
            return new VoiceFailure(translationFailure.getErrorCode(), translationFailure.isRetryable());
        }
        if (failure instanceof XfyunClientException clientFailure) {
            return new VoiceFailure(clientFailure.getErrorCode(), clientFailure.isRetryable());
        }
        if (failure instanceof VoiceProcessingException processingFailure) {
            return new VoiceFailure(processingFailure.errorCode(), processingFailure.retryable());
        }
        return new VoiceFailure(INTERNAL_ERROR_CODE, false);
    }

    private record RetryWindows(Date processingBefore, Date retryOneBefore, Date retryTwoBefore,
                                Date retryThreeBefore) {
    }

    private record VoiceFailure(String errorCode, boolean retryable) {
    }

    private record VoiceField(String labelKey, String sourceText, I18nTextSource source, String translation) {

        private VoiceField withTranslation(String translatedText) {
            return new VoiceField(labelKey, sourceText, source, translatedText);
        }

        private String previewValue() {
            return StringUtils.isNotBlank(translation) ? translation : sourceText;
        }
    }

    private record VoiceDraft(String planDate, String leaderName, VoiceField greenhouse, VoiceField workItem,
                              VoiceField managerRequirement, VoiceField techInstruction, VoiceField overallTechNote,
                              String sourceText, String uyghurPreview) {

        private VoiceDraft withScripts(String chineseScript, String preview) {
            return new VoiceDraft(planDate, leaderName, greenhouse, workItem, managerRequirement, techInstruction,
                overallTechNote, chineseScript, preview);
        }
    }

    private static final class VoiceProcessingException extends RuntimeException {

        private final String errorCode;
        private final boolean retryable;

        private VoiceProcessingException(String errorCode, boolean retryable) {
            this(errorCode, retryable, null);
        }

        private VoiceProcessingException(String errorCode, boolean retryable, Throwable cause) {
            super(errorCode, cause);
            this.errorCode = errorCode;
            this.retryable = retryable;
        }

        private String errorCode() {
            return errorCode;
        }

        private boolean retryable() {
            return retryable;
        }
    }
}
