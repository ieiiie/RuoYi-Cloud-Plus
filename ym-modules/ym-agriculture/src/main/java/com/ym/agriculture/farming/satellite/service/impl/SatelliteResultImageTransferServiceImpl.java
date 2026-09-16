package com.ym.agriculture.farming.satellite.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.farming.satellite.dao.SfSatelliteTaskResultMapper;
import com.ym.agriculture.farming.satellite.model.entity.SfSatelliteTaskResult;
import com.ym.agriculture.farming.satellite.service.ISatelliteResultImageTransferService;
import com.ym.agriculture.farming.satellite.support.SatelliteMasterOssAccessor;
import com.ym.agriculture.farming.satellite.support.SatelliteResultImageWebSupport;
import com.ym.agriculture.farming.satellite.support.SatelliteResultImageWebSupport.DownloadPayload;
import com.ym.agriculture.farming.satellite.support.SatelliteResultImageWebSupport.NormalizedImage;
import com.ym.agriculture.farming.satellite.support.SatelliteResultOssUrlSupport;
import com.ym.resource.api.domain.RemoteFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 遥感结果影像转存：object_key1 → 重新 buildGisOssUrl → HTTP 下载 → 系统 MinIO。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SatelliteResultImageTransferServiceImpl implements ISatelliteResultImageTransferService {

    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 120_000;

    private final SatelliteMasterOssAccessor masterOssAccessor;
    private final SfSatelliteTaskResultMapper taskResultMapper;

    @Override
    public Optional<TransferResult> tryTransfer(String tenantId, String dkId, String taskType, String imageDate,
                                                String upstreamObjectKey1, String existingOssUrl) {
        String sourceUrl = SatelliteResultOssUrlSupport.resolveTransferDownloadUrl(upstreamObjectKey1);
        return doTransfer(tenantId, dkId, taskType, imageDate, upstreamObjectKey1, sourceUrl);
    }

    @Override
    public Optional<TransferResult> tryTransferResult(SfSatelliteTaskResult result) {
        if (result == null) {
            return Optional.empty();
        }
        if (migrateHistoricalResult(result) != MigrateOutcome.MIGRATED) {
            return Optional.empty();
        }
        return Optional.of(new TransferResult(result.getObjectKey1(), result.getOssUrl()));
    }

    @Override
    public MigrateOutcome migrateHistoricalResult(SfSatelliteTaskResult result) {
        if (result == null) {
            return MigrateOutcome.SKIPPED;
        }
        String objectKey1 = result.getObjectKey1();
        if (StringUtils.isBlank(objectKey1) || SatelliteResultOssUrlSupport.isLocalObjectKey(objectKey1)) {
            return MigrateOutcome.SKIPPED;
        }
        String freshGisUrl = SatelliteResultOssUrlSupport.resolveTransferDownloadUrl(objectKey1);
        if (StringUtils.isBlank(freshGisUrl)) {
            log.warn("遥感影像迁移失败：无法由 object_key1 重新生成 gis URL resultId={} objectKey1={}",
                result.getResultId(), objectKey1);
            return MigrateOutcome.FAILED;
        }

        Optional<TransferResult> transferred = doTransfer(
            result.getTenantId(),
            result.getDkId(),
            result.getTaskType(),
            result.getImageDate(),
            objectKey1,
            freshGisUrl);
        if (transferred.isPresent()) {
            applyTransfer(result, transferred.get());
            return MigrateOutcome.MIGRATED;
        }
        if (clearNonLocalOssUrlIfPresent(result)) {
            return MigrateOutcome.URL_REFRESHED;
        }
        return MigrateOutcome.FAILED;
    }

    @Override
    public RepairOutcome repairPreviewableResult(SfSatelliteTaskResult result) {
        if (result == null || result.getResultId() == null) {
            return RepairOutcome.SKIPPED;
        }
        if (!SatelliteResultOssUrlSupport.isLegacyLocalObjectKey(result.getObjectKey1())) {
            return RepairOutcome.SKIPPED;
        }
        if (!SatelliteResultOssUrlSupport.isLocalMinioOssUrl(result.getOssUrl())) {
            return RepairOutcome.SKIPPED;
        }
        Optional<TransferResult> repaired = reuploadFromLocalUrl(result);
        if (repaired.isEmpty()) {
            log.warn("遥感影像预览修复失败 resultId={} ossUrl={}", result.getResultId(), result.getOssUrl());
            return RepairOutcome.FAILED;
        }
        applyTransfer(result, repaired.get());
        log.info("遥感影像预览修复成功 resultId={} dkId={} newKey={}",
            result.getResultId(), result.getDkId(), repaired.get().localObjectKey());
        return RepairOutcome.REPAIRED;
    }

    /**
     * 下载 + 上传的实际执行逻辑，sourceUrl 由调用方预先 resolve 避免重复调用 OSS。
     */
    private Optional<TransferResult> doTransfer(String tenantId, String dkId, String taskType, String imageDate,
                                                 String upstreamObjectKey1, String sourceUrl) {
        if (StringUtils.isBlank(sourceUrl)) {
            log.warn("遥感影像转存跳过：无法生成上游下载 URL dkId={} objectKey1={}", dkId, upstreamObjectKey1);
            return Optional.empty();
        }
        try {
            DownloadPayload downloaded = downloadFromUrl(sourceUrl);
            if (downloaded == null || downloaded.content() == null || downloaded.content().length == 0) {
                log.warn("遥感影像转存失败：下载内容为空 dkId={} url={}", dkId, sourceUrl);
                return Optional.empty();
            }
            return uploadNormalizedImage(
                tenantId,
                dkId,
                taskType,
                imageDate,
                upstreamObjectKey1,
                downloaded.content(),
                downloaded.responseContentType());
        } catch (Exception e) {
            log.error("遥感影像转存失败 dkId={} objectKey1={} sourceUrl={}", dkId, upstreamObjectKey1, sourceUrl, e);
            return Optional.empty();
        }
    }

    private Optional<TransferResult> reuploadFromLocalUrl(SfSatelliteTaskResult result) {
        String sourceUrl = result.getOssUrl();
        if (StringUtils.isBlank(sourceUrl)) {
            return Optional.empty();
        }
        try {
            DownloadPayload downloaded = downloadFromUrl(sourceUrl);
            if (downloaded == null || downloaded.content() == null || downloaded.content().length == 0) {
                return Optional.empty();
            }
            String upstreamKey = StringUtils.blankToDefault(result.getObjectKey1(), "repair.png");
            return uploadNormalizedImage(
                result.getTenantId(),
                result.getDkId(),
                result.getTaskType(),
                result.getImageDate(),
                upstreamKey,
                downloaded.content(),
                downloaded.responseContentType());
        } catch (Exception e) {
            log.error("遥感影像预览修复失败 resultId={} ossUrl={}", result.getResultId(), sourceUrl, e);
            return Optional.empty();
        }
    }

    private Optional<TransferResult> uploadNormalizedImage(String tenantId, String dkId, String taskType,
                                                           String imageDate, String upstreamObjectKey1,
                                                           byte[] rawContent, String responseContentType) {
        String contentType = SatelliteResultImageWebSupport.resolveUploadContentType(
            upstreamObjectKey1, responseContentType, rawContent);
        NormalizedImage normalized = SatelliteResultImageWebSupport.normalizeForWebPreview(
            rawContent, contentType, upstreamObjectKey1);
        String originalFileName = buildLocalOriginalFileName(
            tenantId, dkId, taskType, imageDate, upstreamObjectKey1, normalized.extension());
        String resolvedTenantId = StringUtils.isNotBlank(tenantId) ? tenantId : "000000";
        RemoteFile oss = masterOssAccessor.uploadBytes(
            resolvedTenantId, normalized.content(), originalFileName, normalized.contentType());
        return Optional.of(new TransferResult(oss.getFileName(), oss.getUrl()));
    }

    @Override
    public void refreshGisOssUrl(SfSatelliteTaskResult result) {
        clearNonLocalOssUrlIfPresent(result);
    }

    /**
     * 清除库内非 MinIO 的 {@code oss_url}（如 gis 阿里云签名链），避免前端误用不可预览地址。
     */
    private boolean clearNonLocalOssUrlIfPresent(SfSatelliteTaskResult result) {
        if (result == null || result.getResultId() == null || StringUtils.isBlank(result.getOssUrl())) {
            return false;
        }
        if (SatelliteResultOssUrlSupport.isLocalMinioOssUrl(result.getOssUrl())) {
            return false;
        }
        SfSatelliteTaskResult patch = new SfSatelliteTaskResult();
        patch.setResultId(result.getResultId());
        patch.setOssUrl(null);
        taskResultMapper.updateById(patch);
        result.setOssUrl(null);
        log.info("遥感影像已清除非 MinIO oss_url resultId={} dkId={}", result.getResultId(), result.getDkId());
        return true;
    }

    @Override
    public void applyTransfer(SfSatelliteTaskResult result, TransferResult transferResult) {
        if (result == null || transferResult == null || result.getResultId() == null) {
            return;
        }
        SfSatelliteTaskResult patch = new SfSatelliteTaskResult();
        patch.setResultId(result.getResultId());
        patch.setObjectKey1(transferResult.localObjectKey());
        patch.setOssUrl(transferResult.localUrl());
        taskResultMapper.updateById(patch);
        result.setObjectKey1(transferResult.localObjectKey());
        result.setOssUrl(transferResult.localUrl());
    }

    static DownloadPayload downloadFromUrl(String sourceUrl) {
        try (HttpResponse response = HttpRequest.get(sourceUrl)
            .setConnectionTimeout(CONNECT_TIMEOUT_MS)
            .setReadTimeout(READ_TIMEOUT_MS)
            .execute()) {
            if (!response.isOk()) {
                log.warn("遥感影像 HTTP 下载失败 status={} url={}", response.getStatus(), sourceUrl);
                return null;
            }
            return new DownloadPayload(response.bodyBytes(), response.header("Content-Type"));
        }
    }

    static String buildLocalOriginalFileName(String tenantId, String dkId, String taskType, String imageDate,
                                             String upstreamObjectKey1) {
        return buildLocalOriginalFileName(tenantId, dkId, taskType, imageDate, upstreamObjectKey1, null);
    }

    static String buildLocalOriginalFileName(String tenantId, String dkId, String taskType, String imageDate,
                                             String upstreamObjectKey1, String extensionOverride) {
        String ext = StringUtils.isNotBlank(extensionOverride)
            ? extensionOverride
            : extractExtension(upstreamObjectKey1);
        String safeTenant = sanitizePathSegment(StringUtils.blankToDefault(tenantId, "000000"));
        String safeDkId = sanitizePathSegment(StringUtils.blankToDefault(dkId, "unknown"));
        String safeDate = sanitizePathSegment(StringUtils.blankToDefault(imageDate, "unknown"));
        String safeTaskType = sanitizePathSegment(StringUtils.blankToDefault(taskType, "unknown"));
        return SatelliteResultOssUrlSupport.LOCAL_OBJECT_KEY_PREFIX
            + safeTenant + "/"
            + safeDkId + "/"
            + safeDate + "/"
            + safeTaskType
            + ext;
    }

    private static String extractExtension(String upstreamObjectKey1) {
        String ext = FileUtil.extName(StringUtils.blankToDefault(upstreamObjectKey1, ""));
        if (StringUtils.isBlank(ext)) {
            return ".png";
        }
        return "." + ext;
    }

    private static String sanitizePathSegment(String segment) {
        return segment.trim().replace('/', '_').replace('\\', '_');
    }
}
