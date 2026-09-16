package com.ym.agriculture.farming.satellite.support;

import com.aizuda.oss.OSS;
import com.ym.common.core.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 遥感结果影像转存与迁移判定工具；前端接口直读 {@code sf_satellite_task_result.oss_url}，不在此动态拼链。
 * <p>
 * 历史库内 {@code oss_url} 多为回调时写入的阿里云签名快照，常已失效；转存/迁移下载须由 {@code object_key1}
 * 调用 {@link #buildGisOssUrl(String)} 重新生成，禁止使用库内 {@code oss_url} 作 HTTP 下载源。
 */
@Slf4j
public final class SatelliteResultOssUrlSupport {

    /** 上传时 {@code sys_oss.original_name} 使用的路径前缀（便于识别与排查，不再写入 {@code object_key1}）。 */
    public static final String LOCAL_OBJECT_KEY_PREFIX = "satellite/";

    /** 转存后 {@code object_key1} / {@code sys_oss.file_name} 使用的 MinIO 对象键前缀。 */
    public static final String LOCAL_MINIO_OBJECT_KEY_PREFIX =
        SatelliteResultImageWebSupport.LOCAL_MINIO_OBJECT_KEY_PREFIX;

    /** 与 {@code aizuda.oss.gis-oss} 配置键一致。 */
    public static final String GIS_OSS_PLATFORM = "gis-oss";

    private SatelliteResultOssUrlSupport() {
    }

    /**
     * 由上游 gis-oss 对象键重新生成 HTTP 访问 URL（回调转存与历史迁移下载的唯一来源）。
     */
    public static String buildGisOssUrl(String objectKey) {
        if (StringUtils.isBlank(objectKey) || isLocalObjectKey(objectKey)) {
            return null;
        }
        try {
            return OSS.fileStorage(GIS_OSS_PLATFORM).getUrl(objectKey.trim());
        } catch (Exception e) {
            log.error("OSS 生成访问 URL 失败 objectKey={}", objectKey, e);
            return null;
        }
    }

    /**
     * 转存下载 URL：仅由 {@code object_key1} 重新生成，不读库内 {@code oss_url}。
     */
    public static String resolveTransferDownloadUrl(String objectKey1) {
        return buildGisOssUrl(objectKey1);
    }

    /**
     * 是否仍需从上游 gis-oss 转存到本地 MinIO。
     */
    public static boolean needsTransfer(String objectKey1) {
        String s = buildGisOssUrl(objectKey1);
        return needsTransferGivenFreshGisUrl(objectKey1,s);
    }

    /**
     * 兼容旧签名；{@code ossUrl} 不参与判定。
     */
    public static boolean needsTransfer(String objectKey1, String ossUrl) {
        return needsTransfer(objectKey1);
    }

    /**
     * 是否已转存至本地 MinIO。
     */
    public static boolean isLocalStorageResult(String objectKey1) {
        return StringUtils.isNotBlank(objectKey1) && !needsTransfer(objectKey1);
    }

    public static boolean isLocalObjectKey(String objectKey) {
        if (StringUtils.isBlank(objectKey)) {
            return false;
        }
        String key = objectKey.trim();
        return key.startsWith(LOCAL_MINIO_OBJECT_KEY_PREFIX)
            || key.startsWith(LOCAL_OBJECT_KEY_PREFIX);
    }

    /**
     * 是否为 legacy 转存路径（{@code satellite/...}，需预览修复任务处理）。
     */
    public static boolean isLegacyLocalObjectKey(String objectKey) {
        return StringUtils.isNotBlank(objectKey)
            && objectKey.trim().startsWith(LOCAL_OBJECT_KEY_PREFIX);
    }

    /**
     * {@code oss_url} 是否指向本地 MinIO（非 gis 阿里云签名链）。
     */
    public static boolean isLocalMinioOssUrl(String ossUrl) {
        if (StringUtils.isBlank(ossUrl)) {
            return false;
        }
        String url = ossUrl.trim().toLowerCase();
        return !url.contains("aliyuncs.com")
            && !url.contains("data-satellite");
    }

    /**
     * 已知 fresh gis URL 时的转存判定（供单测注入 fresh URL，不触发 OSS）。
     */
    static boolean needsTransferGivenFreshGisUrl(String objectKey1, String freshGisUrl) {
        if (StringUtils.isBlank(objectKey1) || isLocalObjectKey(objectKey1)) {
            return false;
        }
        return StringUtils.isNotBlank(freshGisUrl);
    }
}
