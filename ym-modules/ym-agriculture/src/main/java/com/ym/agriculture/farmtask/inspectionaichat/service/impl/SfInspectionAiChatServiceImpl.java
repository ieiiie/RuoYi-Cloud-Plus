package com.ym.agriculture.farmtask.inspectionaichat.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.enums.PushSourceEnum;
import com.ym.common.core.enums.PushTypeEnum;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.redis.utils.RedisUtils;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.push.dto.PushPayloadDTO;
import com.ym.common.push.helper.PushHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farmtask.inspectionaichat.client.SfInspectionAiDashScopeClient;
import com.ym.agriculture.farmtask.inspectionaichat.config.SfInspectionAiProperties;
import com.ym.agriculture.farmtask.inspectionaichat.dao.SfInspectionAiContextSummaryMapper;
import com.ym.agriculture.farmtask.inspectionaichat.dao.SfInspectionAiConversationMapper;
import com.ym.agriculture.farmtask.inspectionaichat.dao.SfInspectionAiMessageMapper;
import com.ym.agriculture.farmtask.inspectionaichat.dao.SfInspectionAiMessagePhotoMapper;
import com.ym.agriculture.farmtask.inspectionaichat.model.bo.SfInspectionAiRegenerateBo;
import com.ym.agriculture.farmtask.inspectionaichat.model.bo.SfInspectionAiSendBo;
import com.ym.agriculture.farmtask.inspectionaichat.model.entity.SfInspectionAiContextSummary;
import com.ym.agriculture.farmtask.inspectionaichat.model.entity.SfInspectionAiConversation;
import com.ym.agriculture.farmtask.inspectionaichat.model.entity.SfInspectionAiMessage;
import com.ym.agriculture.farmtask.inspectionaichat.model.entity.SfInspectionAiMessagePhoto;
import com.ym.agriculture.farmtask.inspectionaichat.model.vo.SfInspectionAiAttachmentVo;
import com.ym.agriculture.farmtask.inspectionaichat.model.vo.SfInspectionAiConversationDetailVo;
import com.ym.agriculture.farmtask.inspectionaichat.model.vo.SfInspectionAiConversationVo;
import com.ym.agriculture.farmtask.inspectionaichat.model.vo.SfInspectionAiMessageVo;
import com.ym.agriculture.farmtask.inspectionaichat.model.vo.SfInspectionAiSendVo;
import com.ym.agriculture.farmtask.inspectionaichat.service.ISfInspectionAiChatService;
import com.ym.agriculture.farmtask.inspectionphotoarchive.dao.SfInspectionPhotoArchiveFieldMapper;
import com.ym.agriculture.farmtask.inspectionphotoarchive.dao.SfInspectionPhotoArchiveMapper;
import com.ym.agriculture.farmtask.inspectionphotoarchive.dao.SfInspectionPhotoArchivePhotoMapper;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.entity.SfInspectionPhotoArchive;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.entity.SfInspectionPhotoArchiveField;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.entity.SfInspectionPhotoArchivePhoto;
import com.ym.agriculture.farmtask.inspectionphotoarchive.support.SfInspectionPhotoArchiveMasterOssAccessor;
import com.ym.resource.api.domain.RemoteFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** 巡查照片 AI 问答服务实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SfInspectionAiChatServiceImpl implements ISfInspectionAiChatService {
    private static final DateTimeFormatter HISTORY_PHOTO_DATE_FORMATTER = DateTimeFormatter.ofPattern("M月d日");
    private static final DateTimeFormatter HISTORY_TEXT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String ROLE_USER = "USER";
    private static final String ROLE_ASSISTANT = "ASSISTANT";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_STREAMING = "STREAMING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_SUPERSEDED = "SUPERSEDED";
    private static final String SYSTEM_PROMPT = "你是农业巡查照片辅助分析助手。仅依据照片、照片元数据和对话回答。先描述可见现象，再说明可能原因与不确定性，最后给出补拍或人工复核建议。不得给出确定病害诊断、农药剂量或替代人工生产决策。";

    private final SfInspectionAiConversationMapper conversationMapper;
    private final SfInspectionAiMessageMapper messageMapper;
    private final SfInspectionAiMessagePhotoMapper messagePhotoMapper;
    private final SfInspectionAiContextSummaryMapper summaryMapper;
    private final SfInspectionPhotoArchiveMapper archiveMapper;
    private final SfInspectionPhotoArchiveFieldMapper archiveFieldMapper;
    private final SfInspectionPhotoArchivePhotoMapper archivePhotoMapper;
    private final SfInspectionPhotoArchiveMasterOssAccessor masterOssAccessor;
    private final SfInspectionAiDashScopeClient dashScopeClient;
    private final SfInspectionAiProperties properties;
    private final ExecutorService executor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "sf-inspection-ai-worker");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public PageResult<SfInspectionAiConversationVo> queryPage(PageQuery pageQuery) {
        String tenantId = requireTenantId();
        PageResult<SfInspectionAiConversationVo> page = com.ym.agriculture.shared.common.AgriculturePageResults.build(conversationMapper.selectPage(pageQuery.build(), Wrappers.<SfInspectionAiConversation>lambdaQuery()
            .eq(SfInspectionAiConversation::getTenantId, tenantId)
            .orderByDesc(SfInspectionAiConversation::getLastQuestionTime)
            .orderByDesc(SfInspectionAiConversation::getConversationId)).convert(this::toConversationVo));
        fillHistoryTitles(tenantId, new ArrayList<>(page.getRows()));
        return page;
    }

    @Override
    public SfInspectionAiConversationDetailVo getDetail(Long conversationId) {
        String tenantId = requireTenantId();
        return buildDetail(tenantId, requireConversation(tenantId, conversationId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SfInspectionAiSendVo send(SfInspectionAiSendBo bo, String sseToken) {
        String tenantId = requireTenantId();
        Long userId = requireUserId();
        validateSend(bo);
        SfInspectionAiMessage existing = messageMapper.selectByRequestId(tenantId, userId, bo.getClientRequestId());
        if (existing != null) return buildSendVo(tenantId, existing);
        ensureRateLimit(tenantId, userId);
        SfInspectionAiConversation conversation = bo.getConversationId() == null ? createConversation(tenantId, userId, bo) : requireConversation(tenantId, bo.getConversationId());
        rejectIfGenerating(tenantId, conversation.getConversationId());
        List<SfInspectionPhotoArchivePhoto> photos = resolvePhotos(tenantId, bo.getPhotoIds());
        int nextSequence = nextMessageSeq(tenantId, conversation.getConversationId());
        Date now = new Date();
        SfInspectionAiMessage userMessage = newMessage(tenantId, userId, conversation.getConversationId(), nextSequence, ROLE_USER);
        userMessage.setContent(StrUtil.trim(bo.getContent()));
        userMessage.setClientRequestId(bo.getClientRequestId());
        userMessage.setGenerationStatus(STATUS_COMPLETED);
        userMessage.setCreateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now));
        messageMapper.insert(userMessage);
        savePhotoSnapshots(tenantId, userMessage.getMessageId(), photos);
        SfInspectionAiMessage assistant = newMessage(tenantId, userId, conversation.getConversationId(), nextSequence + 1, ROLE_ASSISTANT);
        assistant.setReplyToMessageId(userMessage.getMessageId());
        assistant.setGenerationAttempt(1);
        assistant.setGenerationStatus(STATUS_PENDING);
        assistant.setModelId(properties.getModelId());
        assistant.setCreateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now));
        messageMapper.insert(assistant);
        conversationMapper.update(null, Wrappers.<SfInspectionAiConversation>lambdaUpdate()
            .eq(SfInspectionAiConversation::getConversationId, conversation.getConversationId())
            .set(SfInspectionAiConversation::getLastQuestionTime, now)
            .set(SfInspectionAiConversation::getUpdateBy, userId)
            .set(SfInspectionAiConversation::getUpdateTime,
                com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now)));
        submitGeneration(tenantId, userId, sseToken, conversation.getConversationId(), userMessage.getMessageId(), assistant.getMessageId());
        return sendVo(tenantId, conversation, userMessage, assistant);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SfInspectionAiSendVo regenerate(Long userMessageId, SfInspectionAiRegenerateBo bo, String sseToken) {
        String tenantId = requireTenantId();
        Long userId = requireUserId();
        if (!isUuid(bo.getClientRequestId())) throw new ServiceException("请求标识必须为 UUID");
        SfInspectionAiMessage previousRequest = messageMapper.selectByRequestId(tenantId, userId, bo.getClientRequestId());
        if (previousRequest != null) return buildSendVo(tenantId, previousRequest);
        SfInspectionAiMessage userMessage = messageMapper.selectById(userMessageId);
        if (userMessage == null || !tenantId.equals(userMessage.getTenantId()) || !ROLE_USER.equals(userMessage.getRole())) throw new ServiceException("问题不存在");
        rejectIfGenerating(tenantId, userMessage.getConversationId());
        SfInspectionAiMessage latest = messageMapper.selectLatestReply(tenantId, userMessageId);
        int attempt = latest == null ? 1 : latest.getGenerationAttempt() + 1;
        if (latest != null) messageMapper.update(null, Wrappers.<SfInspectionAiMessage>lambdaUpdate()
            .eq(SfInspectionAiMessage::getMessageId, latest.getMessageId()).set(SfInspectionAiMessage::getGenerationStatus, STATUS_SUPERSEDED));
        SfInspectionAiMessage assistant = newMessage(tenantId, userId, userMessage.getConversationId(), nextMessageSeq(tenantId, userMessage.getConversationId()), ROLE_ASSISTANT);
        assistant.setReplyToMessageId(userMessageId);
        assistant.setGenerationAttempt(attempt);
        assistant.setClientRequestId(bo.getClientRequestId());
        assistant.setGenerationStatus(STATUS_PENDING);
        assistant.setModelId(properties.getModelId());
        assistant.setCreateTime(java.time.LocalDateTime.now());
        messageMapper.insert(assistant);
        submitGeneration(tenantId, userId, sseToken, userMessage.getConversationId(), userMessageId, assistant.getMessageId());
        return sendVo(tenantId, requireConversation(tenantId, userMessage.getConversationId()), userMessage, assistant);
    }

    @Override
    public int failStaleGenerations() {
        Date threshold = new Date(System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(properties.getStaleGenerationSeconds()));
        return messageMapper.update(null, Wrappers.<SfInspectionAiMessage>lambdaUpdate()
            .in(SfInspectionAiMessage::getGenerationStatus, STATUS_PENDING, STATUS_STREAMING)
            .lt(SfInspectionAiMessage::getCreateTime, threshold)
            .set(SfInspectionAiMessage::getGenerationStatus, STATUS_FAILED)
            .set(SfInspectionAiMessage::getErrorCode, "GENERATION_STALE")
            .set(SfInspectionAiMessage::getErrorMessage, "生成任务已超时，请重新生成")
            .set(SfInspectionAiMessage::getCompletedTime, new Date()));
    }

    private void submitGeneration(String tenantId, Long userId, String token, Long conversationId, Long userMessageId, Long assistantMessageId) {
        String requestId = UUID.randomUUID().toString();
        executor.execute(() -> TenantHelper.dynamic(tenantId, () -> runGeneration(tenantId, userId, token, requestId, conversationId, userMessageId, assistantMessageId)));
    }

    private void runGeneration(String tenantId, Long userId, String token, String requestId, Long conversationId, Long userMessageId, Long assistantMessageId) {
        RLock lock = RedisUtils.getClient().getLock("inspection-ai:conversation:" + conversationId);
        boolean locked = false;
        try {
            locked = lock.tryLock(1, TimeUnit.SECONDS);
            if (!locked) throw new ServiceException("会话正在生成，请稍后重试");
            messageMapper.update(null, Wrappers.<SfInspectionAiMessage>lambdaUpdate()
                .eq(SfInspectionAiMessage::getMessageId, assistantMessageId)
                .set(SfInspectionAiMessage::getGenerationStatus, STATUS_STREAMING)
                .set(SfInspectionAiMessage::getStartedTime, new Date()));
            maybeSummarize(tenantId, conversationId);
            List<JSONObject> messages = buildVisionMessages(tenantId, conversationId, userMessageId);
            StringBuilder content = new StringBuilder();
            int[] sequence = {0};
            SfInspectionAiDashScopeClient.Usage usage = dashScopeClient.streamVision(messages, delta -> {
                content.append(delta);
                persistPartial(assistantMessageId, content.toString());
                publish(token, userId, event("delta", requestId, conversationId, assistantMessageId, ++sequence[0], delta));
            });
            complete(assistantMessageId, content.toString(), usage);
            publish(token, userId, event("completed", requestId, conversationId, assistantMessageId, ++sequence[0], content.toString()));
        } catch (Exception exception) {
            log.warn("巡查照片 AI 生成失败 conversationId={}, assistantMessageId={}", conversationId, assistantMessageId, exception);
            fail(assistantMessageId, exception.getMessage());
            publish(token, userId, event("failed", requestId, conversationId, assistantMessageId, 0, "AI 生成失败，请重新生成"));
        } finally {
            if (locked && lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    private void maybeSummarize(String tenantId, Long conversationId) {
        List<SfInspectionAiMessage> messages = messageMapper.selectByConversation(tenantId, conversationId);
        int keep = properties.getSummary().getRetainRecentRounds() * 2;
        if (messages.size() <= keep + 2) return;
        int cover = messages.get(messages.size() - keep - 1).getMessageSeq();
        SfInspectionAiContextSummary latest = summaryMapper.selectLatestCompleted(tenantId, conversationId);
        if (latest != null && latest.getCoveredThroughMessageSeq() >= cover) return;
        String source = messages.stream().filter(item -> item.getMessageSeq() <= cover && STATUS_COMPLETED.equals(item.getGenerationStatus()))
            .map(item -> item.getRole() + "：" + item.getContent()).collect(Collectors.joining("\n"));
        if (StrUtil.isBlank(source)) return;
        SfInspectionAiContextSummary summary = new SfInspectionAiContextSummary();
        summary.setSummaryId(IdWorker.getId()); summary.setTenantId(tenantId); summary.setConversationId(conversationId);
        summary.setCoveredThroughMessageSeq(cover); summary.setGenerationStatus(STATUS_PENDING); summary.setStartedTime(new Date()); summary.setCreateTime(new Date());
        summaryMapper.insert(summary);
        try {
            SfInspectionAiDashScopeClient.Completion completion = dashScopeClient.summarize(properties.getSummary().getModelId(), source);
            completeSummary(summary.getSummaryId(), completion, properties.getSummary().getModelId(), false);
        } catch (Exception exception) {
            if (isTransientSummaryFailure(exception)) {
                try {
                    SfInspectionAiDashScopeClient.Completion fallback = dashScopeClient.summarize(properties.getSummary().getFallbackModelId(), source);
                    completeSummary(summary.getSummaryId(), fallback, properties.getSummary().getFallbackModelId(), true);
                    return;
                } catch (Exception fallbackException) {
                    log.warn("巡查 AI 摘要主备模型均失败 conversationId={}", conversationId, fallbackException);
                }
            }
            summaryMapper.update(null, Wrappers.<SfInspectionAiContextSummary>lambdaUpdate().eq(SfInspectionAiContextSummary::getSummaryId, summary.getSummaryId())
                .set(SfInspectionAiContextSummary::getGenerationStatus, STATUS_FAILED).set(SfInspectionAiContextSummary::getErrorCode, "SUMMARY_FAILED")
                .set(SfInspectionAiContextSummary::getErrorMessage, "历史整理失败").set(SfInspectionAiContextSummary::getCompletedTime, new Date()));
        }
    }

    private List<JSONObject> buildVisionMessages(String tenantId, Long conversationId, Long currentUserMessageId) {
        List<SfInspectionAiMessage> all = messageMapper.selectByConversation(tenantId, conversationId);
        List<SfInspectionAiMessage> completed = all.stream().filter(item -> ROLE_USER.equals(item.getRole()) || STATUS_COMPLETED.equals(item.getGenerationStatus())).toList();
        SfInspectionAiContextSummary summary = summaryMapper.selectLatestCompleted(tenantId, conversationId);
        int keep = properties.getSummary().getRetainRecentRounds() * 2;
        List<SfInspectionAiMessage> tail = completed.stream().skip(Math.max(0, completed.size() - keep)).toList();
        List<JSONObject> result = new ArrayList<>();
        result.add(textMessage("system", SYSTEM_PROMPT));
        if (summary != null) result.add(textMessage("system", "历史摘要：" + summary.getContent()));
        Set<Long> attached = new HashSet<>();
        for (SfInspectionAiMessage message : tail) result.add(toModelMessage(tenantId, message, attached, currentUserMessageId.equals(message.getMessageId())));
        return result;
    }

    private JSONObject toModelMessage(String tenantId, SfInspectionAiMessage message, Set<Long> attached, boolean current) {
        if (!ROLE_USER.equals(message.getRole())) return textMessage("assistant", StrUtil.blankToDefault(message.getContent(), ""));
        JSONArray content = new JSONArray();
        List<SfInspectionAiMessagePhoto> photos = messagePhotoMapper.selectByMessageIds(tenantId, List.of(message.getMessageId()));
        Map<Long, RemoteFile> oss = masterOssAccessor.listByIds(tenantId, photos.stream().map(SfInspectionAiMessagePhoto::getOssId).toList(), Duration.ofSeconds(properties.getSignedUrlTtlSeconds())).stream()
            .collect(Collectors.toMap(RemoteFile::getOssId, item -> item, (left, right) -> left));
        for (SfInspectionAiMessagePhoto photo : photos) {
            if (attached.size() >= properties.getMaxImagesPerRequest()) break;
            RemoteFile file = oss.get(photo.getOssId());
            if (file == null || StrUtil.isBlank(file.getUrl()) || !attached.add(photo.getPhotoId())) continue;
            JSONObject image = new JSONObject(); image.put("type", "image_url"); JSONObject imageUrl = new JSONObject(); imageUrl.put("url", file.getUrl()); image.put("image_url", imageUrl); content.add(image);
        }
        JSONObject text = new JSONObject(); text.put("type", "text"); text.put("text", message.getContent()); content.add(text);
        JSONObject result = new JSONObject(); result.put("role", "user"); result.put("content", content); return result;
    }

    private void savePhotoSnapshots(String tenantId, Long messageId, List<SfInspectionPhotoArchivePhoto> photos) {
        if (photos.isEmpty()) return;
        Set<Long> fieldIds = photos.stream().map(SfInspectionPhotoArchivePhoto::getArchiveFieldId).collect(Collectors.toSet());
        Map<Long, SfInspectionPhotoArchiveField> fields = archiveFieldMapper.selectList(Wrappers.<SfInspectionPhotoArchiveField>lambdaQuery().in(SfInspectionPhotoArchiveField::getArchiveFieldId, fieldIds)).stream().collect(Collectors.toMap(SfInspectionPhotoArchiveField::getArchiveFieldId, item -> item));
        Map<Long, SfInspectionPhotoArchive> archives = archiveMapper.selectList(Wrappers.<SfInspectionPhotoArchive>lambdaQuery().in(SfInspectionPhotoArchive::getArchiveId, fields.values().stream().map(SfInspectionPhotoArchiveField::getArchiveId).toList())).stream().collect(Collectors.toMap(SfInspectionPhotoArchive::getArchiveId, item -> item));
        Date now = new Date(); int order = 0;
        for (SfInspectionPhotoArchivePhoto photo : photos) {
            SfInspectionPhotoArchiveField field = fields.get(photo.getArchiveFieldId()); SfInspectionPhotoArchive archive = field == null ? null : archives.get(field.getArchiveId());
            SfInspectionAiMessagePhoto row = new SfInspectionAiMessagePhoto(); row.setMessagePhotoId(IdWorker.getId()); row.setTenantId(tenantId); row.setMessageId(messageId); row.setPhotoId(photo.getPhotoId()); row.setOssId(photo.getOssId()); row.setOriginalName(photo.getOriginalName()); row.setSortOrder(order++); row.setCreateTime(now);
            if (field != null) { row.setArchiveFieldId(field.getArchiveFieldId()); row.setArchiveId(field.getArchiveId()); row.setGreenhouseCodeSnapshot(field.getFieldCode()); row.setGreenhouseNameSnapshot(field.getFieldName()); }
            if (archive != null) row.setArchiveDateSnapshot(archive.getArchiveDate());
            messagePhotoMapper.insert(row);
        }
    }

    private List<SfInspectionPhotoArchivePhoto> resolvePhotos(String tenantId, List<Long> photoIds) {
        if (photoIds == null || photoIds.isEmpty()) {
            return List.of();
        }
        if (photoIds.size() > properties.getMaxImagesPerRequest()) {
            throw new ServiceException("单次提问最多附带 " + properties.getMaxImagesPerRequest() + " 张照片");
        }
        List<SfInspectionPhotoArchivePhoto> photos = archivePhotoMapper.selectList(Wrappers.<SfInspectionPhotoArchivePhoto>lambdaQuery()
            .eq(SfInspectionPhotoArchivePhoto::getTenantId, tenantId)
            .in(SfInspectionPhotoArchivePhoto::getPhotoId, photoIds));
        if (photos.size() != photoIds.size()) {
            throw new ServiceException("存在无效或无权限的巡查照片");
        }
        return photos.stream().sorted(Comparator.comparingInt(item -> photoIds.indexOf(item.getPhotoId()))).toList();
    }

    private SfInspectionAiConversation createConversation(String tenantId, Long userId, SfInspectionAiSendBo bo) {
        SfInspectionAiConversation row = new SfInspectionAiConversation(); Date now = new Date();
        row.setConversationId(IdWorker.getId()); row.setTenantId(tenantId); row.setCreateBy(userId); row.setCreateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now)); row.setUpdateBy(userId); row.setUpdateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now)); row.setFirstQuestionTime(now); row.setLastQuestionTime(now); row.setTitle(StrUtil.sub(StrUtil.trim(bo.getContent()), 0, 50));
        conversationMapper.insert(row); return row;
    }

    private SfInspectionAiMessage newMessage(String tenantId, Long userId, Long conversationId, int sequence, String role) {
        SfInspectionAiMessage row = new SfInspectionAiMessage(); row.setMessageId(IdWorker.getId()); row.setTenantId(tenantId); row.setCreateBy(userId); row.setConversationId(conversationId); row.setMessageSeq(sequence); row.setRole(role); row.setGenerationAttempt(0); return row;
    }

    private int nextMessageSeq(String tenantId, Long conversationId) {
        return messageMapper.selectByConversation(tenantId, conversationId).stream().map(SfInspectionAiMessage::getMessageSeq).max(Integer::compareTo).orElse(0) + 1;
    }

    private void rejectIfGenerating(String tenantId, Long conversationId) {
        boolean running = messageMapper.selectByConversation(tenantId, conversationId).stream().anyMatch(item -> STATUS_PENDING.equals(item.getGenerationStatus()) || STATUS_STREAMING.equals(item.getGenerationStatus()));
        if (running) throw new ServiceException("该会话正在生成回答，请稍后再提问");
    }

    private void validateSend(SfInspectionAiSendBo bo) {
        String content = StrUtil.trim(bo.getContent());
        if (StrUtil.isBlank(content) || content.length() > properties.getMaxQuestionLength()) throw new ServiceException("问题长度不符合要求");
        if (!isUuid(bo.getClientRequestId())) throw new ServiceException("请求标识必须为 UUID");
        if (bo.getPhotoIds() == null) {
            bo.setPhotoIds(List.of());
        } else {
            bo.setPhotoIds(bo.getPhotoIds().stream().filter(item -> item != null).distinct().toList());
        }
        long cjk = content.codePoints().filter(code -> code >= 0x4E00 && code <= 0x9FFF).count();
        long effective = content.codePoints().filter(code -> Character.isLetterOrDigit(code)).count();
        if (effective > 0 && cjk * 10 < effective * 3) throw new ServiceException("请使用中文描述巡查问题");
    }

    private void ensureRateLimit(String tenantId, Long userId) {
        RLock lock = RedisUtils.getClient().getLock("inspection-ai:rate:" + tenantId + ":" + userId + ":" + (System.currentTimeMillis() / 60_000));
        boolean locked = false;
        try { locked = lock.tryLock(1, TimeUnit.SECONDS); if (!locked) throw new ServiceException("请求过于频繁，请稍后重试");
            String key = "inspection-ai:rate-count:" + tenantId + ":" + userId + ":" + (System.currentTimeMillis() / 60_000);
            long count = RedisUtils.getClient().getAtomicLong(key).incrementAndGet();
            RedisUtils.getClient().getAtomicLong(key).expire(Duration.ofMinutes(2));
            if (count > properties.getPerUserPerMinute()) throw new ServiceException("每分钟最多发起 " + properties.getPerUserPerMinute() + " 次 AI 问答");
        } catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new ServiceException("请求被中断，请重试"); }
        finally { if (locked && lock.isHeldByCurrentThread()) lock.unlock(); }
    }

    private SfInspectionAiConversation requireConversation(String tenantId, Long conversationId) { SfInspectionAiConversation row = conversationMapper.selectTenantById(tenantId, conversationId); if (row == null) throw new ServiceException("会话不存在或无权访问"); return row; }
    private String requireTenantId() { String tenantId = TenantHelper.getTenantId(); if (StringUtils.isBlank(tenantId)) throw new ServiceException("未获取到当前租户"); return tenantId; }
    private Long requireUserId() { Long userId = LoginHelper.getUserId(); if (userId == null) throw new ServiceException("未获取到当前用户"); return userId; }
    private static boolean isUuid(String value) { try { UUID.fromString(value); return true; } catch (Exception ignored) { return false; } }
    private static JSONObject textMessage(String role, String text) { JSONObject row = new JSONObject(); row.put("role", role); row.put("content", text); return row; }
    private void persistPartial(Long messageId, String content) { messageMapper.update(null, Wrappers.<SfInspectionAiMessage>lambdaUpdate().eq(SfInspectionAiMessage::getMessageId, messageId).set(SfInspectionAiMessage::getContent, content)); }
    private void complete(Long messageId, String content, SfInspectionAiDashScopeClient.Usage usage) { messageMapper.update(null, Wrappers.<SfInspectionAiMessage>lambdaUpdate().eq(SfInspectionAiMessage::getMessageId, messageId).set(SfInspectionAiMessage::getContent, content).set(SfInspectionAiMessage::getGenerationStatus, STATUS_COMPLETED).set(SfInspectionAiMessage::getProviderRequestId, usage.providerRequestId()).set(SfInspectionAiMessage::getInputTokens, usage.inputTokens()).set(SfInspectionAiMessage::getOutputTokens, usage.outputTokens()).set(SfInspectionAiMessage::getTotalTokens, usage.totalTokens()).set(SfInspectionAiMessage::getCompletedTime, new Date())); }
    private void fail(Long messageId, String message) { messageMapper.update(null, Wrappers.<SfInspectionAiMessage>lambdaUpdate().eq(SfInspectionAiMessage::getMessageId, messageId).set(SfInspectionAiMessage::getGenerationStatus, STATUS_FAILED).set(SfInspectionAiMessage::getErrorCode, "GENERATION_FAILED").set(SfInspectionAiMessage::getErrorMessage, StrUtil.sub(StrUtil.blankToDefault(message, "AI 生成失败"), 0, 500)).set(SfInspectionAiMessage::getCompletedTime, new Date())); }
    /**
     * SSE 载荷中的雪花 ID 必须写成字符串。
     * Fastjson 默认把 Long 写成数字，浏览器 JSON.parse 会丢精度，导致前端按 messageId/conversationId 匹配失败、界面一直停在“正在准备分析”。
     */
    private JSONObject event(String type, String requestId, Long conversationId, Long messageId, int seq, String delta) {
        JSONObject row = new JSONObject();
        row.put("channel", "inspection-ai");
        row.put("type", type);
        row.put("requestId", requestId);
        row.put("conversationId", String.valueOf(conversationId));
        row.put("assistantMessageId", String.valueOf(messageId));
        row.put("deltaSeq", seq);
        row.put("delta", delta);
        return row;
    }
    private void publish(String token, Long userId, JSONObject event) {
        if (StrUtil.isBlank(token) || userId == null) return;
        event.put("token", token);
        PushHelper.publishMessage(List.of(userId), PushPayloadDTO.of(
            PushTypeEnum.LLM, PushSourceEnum.LLM, JSON.toJSONString(event), event));
    }
    private SfInspectionAiSendVo sendVo(String tenantId, SfInspectionAiConversation conversation, SfInspectionAiMessage user, SfInspectionAiMessage assistant) {
        List<SfInspectionAiMessagePhoto> photos = messagePhotoMapper.selectByMessageIds(tenantId, List.of(user.getMessageId()));
        SfInspectionAiSendVo vo = new SfInspectionAiSendVo();
        vo.setRequestId(UUID.randomUUID().toString());
        vo.setConversation(toConversationVo(conversation));
        vo.setUserMessage(toMessageVo(user, toAttachments(tenantId, photos)));
        vo.setAssistantMessage(toMessageVo(assistant, List.of()));
        return vo;
    }
    private SfInspectionAiSendVo buildSendVo(String tenantId, SfInspectionAiMessage user) {
        SfInspectionAiMessage actualUser = ROLE_USER.equals(user.getRole()) ? user : messageMapper.selectById(user.getReplyToMessageId());
        SfInspectionAiMessage assistant = messageMapper.selectLatestReply(tenantId, actualUser.getMessageId());
        return sendVo(tenantId, requireConversation(tenantId, actualUser.getConversationId()), actualUser, assistant);
    }
    private SfInspectionAiConversationDetailVo buildDetail(String tenantId, SfInspectionAiConversation conversation) { SfInspectionAiConversationDetailVo vo = new SfInspectionAiConversationDetailVo(); copyConversation(conversation, vo); List<SfInspectionAiMessage> messages = messageMapper.selectByConversation(tenantId, conversation.getConversationId()); Map<Long, List<SfInspectionAiMessagePhoto>> photoMap = messagePhotoMapper.selectByMessageIds(tenantId, messages.stream().map(SfInspectionAiMessage::getMessageId).toList()).stream().collect(Collectors.groupingBy(SfInspectionAiMessagePhoto::getMessageId)); vo.setMessages(messages.stream().filter(item -> !STATUS_SUPERSEDED.equals(item.getGenerationStatus())).map(item -> toMessageVo(item, toAttachments(tenantId, photoMap.getOrDefault(item.getMessageId(), List.of())))).toList()); return vo; }
    private List<SfInspectionAiAttachmentVo> toAttachments(String tenantId, List<SfInspectionAiMessagePhoto> photos) { if (photos.isEmpty()) return List.of(); Map<Long, SfInspectionPhotoArchivePhoto> current = archivePhotoMapper.selectList(Wrappers.<SfInspectionPhotoArchivePhoto>lambdaQuery().eq(SfInspectionPhotoArchivePhoto::getTenantId, tenantId).in(SfInspectionPhotoArchivePhoto::getPhotoId, photos.stream().map(SfInspectionAiMessagePhoto::getPhotoId).toList())).stream().collect(Collectors.toMap(SfInspectionPhotoArchivePhoto::getPhotoId, item -> item)); Map<Long, RemoteFile> oss = masterOssAccessor.listByIds(tenantId, photos.stream().map(SfInspectionAiMessagePhoto::getOssId).toList()).stream().collect(Collectors.toMap(RemoteFile::getOssId, item -> item, (left, right) -> left)); return photos.stream().map(photo -> { SfInspectionAiAttachmentVo vo = new SfInspectionAiAttachmentVo(); SfInspectionPhotoArchivePhoto active = current.get(photo.getPhotoId()); RemoteFile file = oss.get(photo.getOssId()); boolean available = active != null && active.getOssId().equals(photo.getOssId()) && file != null && StrUtil.isNotBlank(file.getUrl()); vo.setPhotoId(photo.getPhotoId()); vo.setOriginalName(photo.getOriginalName()); vo.setArchiveDate(photo.getArchiveDateSnapshot()); vo.setGreenhouseCode(photo.getGreenhouseCodeSnapshot()); vo.setGreenhouseName(photo.getGreenhouseNameSnapshot()); vo.setAvailable(available); vo.setUrl(available ? file.getUrl() : null); return vo; }).toList(); }
    private void completeSummary(Long summaryId, SfInspectionAiDashScopeClient.Completion completion, String modelId, boolean fallback) { summaryMapper.update(null, Wrappers.<SfInspectionAiContextSummary>lambdaUpdate().eq(SfInspectionAiContextSummary::getSummaryId, summaryId).set(SfInspectionAiContextSummary::getContent, completion.content()).set(SfInspectionAiContextSummary::getModelId, modelId).set(SfInspectionAiContextSummary::getFallbackUsed, fallback).set(SfInspectionAiContextSummary::getGenerationStatus, STATUS_COMPLETED).set(SfInspectionAiContextSummary::getProviderRequestId, completion.providerRequestId()).set(SfInspectionAiContextSummary::getInputTokens, completion.usage().inputTokens()).set(SfInspectionAiContextSummary::getOutputTokens, completion.usage().outputTokens()).set(SfInspectionAiContextSummary::getTotalTokens, completion.usage().totalTokens()).set(SfInspectionAiContextSummary::getCompletedTime, new Date())); }
    private static boolean isTransientSummaryFailure(Exception exception) { String message = StrUtil.blankToDefault(exception.getMessage(), ""); return message.contains("网络异常") || message.contains("服务繁忙"); }
    private SfInspectionAiMessageVo toMessageVo(SfInspectionAiMessage row, List<SfInspectionAiAttachmentVo> attachments) { if (row == null) return null; SfInspectionAiMessageVo vo = new SfInspectionAiMessageVo(); vo.setMessageId(row.getMessageId()); vo.setRole(row.getRole()); vo.setContent(row.getContent()); vo.setGenerationStatus(row.getGenerationStatus()); vo.setReplyToMessageId(row.getReplyToMessageId()); vo.setModelId(row.getModelId()); vo.setInputTokens(row.getInputTokens()); vo.setOutputTokens(row.getOutputTokens()); vo.setTotalTokens(row.getTotalTokens()); vo.setErrorMessage(row.getErrorMessage()); vo.setCreateTime(com.ym.agriculture.shared.common.AgricultureTimes.toDate(row.getCreateTime())); vo.setAttachments(attachments); return vo; }
    /**
     * 会话列表不展示首问正文：含照片的会话以首个照片快照的归档日期和大棚名称命名，
     * 纯文本会话则以首问时间命名。会话详情仍保留原始标题。
     */
    private void fillHistoryTitles(String tenantId, List<SfInspectionAiConversationVo> conversations) {
        if (conversations == null || conversations.isEmpty()) return;
        Map<Long, SfInspectionAiConversationVo> conversationMap = conversations.stream()
            .collect(Collectors.toMap(SfInspectionAiConversationVo::getConversationId, item -> item));
        List<SfInspectionAiMessage> messages = messageMapper.selectByConversationIds(tenantId, new ArrayList<>(conversationMap.keySet()));
        List<Long> userMessageIds = messages.stream()
            .filter(item -> ROLE_USER.equals(item.getRole()))
            .map(SfInspectionAiMessage::getMessageId)
            .toList();
        Map<Long, List<SfInspectionAiMessagePhoto>> photoMap = messagePhotoMapper.selectByMessageIds(tenantId, userMessageIds)
            .stream().collect(Collectors.groupingBy(SfInspectionAiMessagePhoto::getMessageId));
        Map<Long, SfInspectionAiMessagePhoto> firstPhotoMap = new HashMap<>();
        for (SfInspectionAiMessage message : messages) {
            if (!ROLE_USER.equals(message.getRole()) || firstPhotoMap.containsKey(message.getConversationId())) continue;
            List<SfInspectionAiMessagePhoto> photos = photoMap.get(message.getMessageId());
            if (photos != null && !photos.isEmpty()) firstPhotoMap.put(message.getConversationId(), photos.get(0));
        }
        for (SfInspectionAiConversationVo conversation : conversations) {
            SfInspectionAiMessagePhoto photo = firstPhotoMap.get(conversation.getConversationId());
            conversation.setTitle(photo == null ? textHistoryTitle(conversation) : photoHistoryTitle(photo));
        }
    }

    private String photoHistoryTitle(SfInspectionAiMessagePhoto photo) {
        LocalDate archiveDate = photo.getArchiveDateSnapshot();
        String date = archiveDate == null ? "巡查" : HISTORY_PHOTO_DATE_FORMATTER.format(archiveDate);
        String greenhouse = StrUtil.blankToDefault(photo.getGreenhouseNameSnapshot(), photo.getGreenhouseCodeSnapshot());
        return StrUtil.isBlank(greenhouse) ? date + "巡查照片问题" : date + " " + greenhouse + "照片问题";
    }

    private String textHistoryTitle(SfInspectionAiConversationVo conversation) {
        Date firstQuestionTime = conversation.getFirstQuestionTime();
        if (firstQuestionTime == null) firstQuestionTime = conversation.getCreateTime();
        if (firstQuestionTime == null) return "提问";
        return HISTORY_TEXT_DATE_FORMATTER.format(firstQuestionTime.toInstant()
            .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()) + " 提问";
    }

    private SfInspectionAiConversationVo toConversationVo(SfInspectionAiConversation row) { SfInspectionAiConversationVo vo = new SfInspectionAiConversationVo(); copyConversation(row, vo); return vo; }
    private void copyConversation(SfInspectionAiConversation source, SfInspectionAiConversationVo target) { target.setConversationId(source.getConversationId()); target.setTitle(source.getTitle()); target.setCreateBy(source.getCreateBy()); target.setCreateTime(com.ym.agriculture.shared.common.AgricultureTimes.toDate(source.getCreateTime())); target.setFirstQuestionTime(source.getFirstQuestionTime()); target.setLastQuestionTime(source.getLastQuestionTime()); }
}
