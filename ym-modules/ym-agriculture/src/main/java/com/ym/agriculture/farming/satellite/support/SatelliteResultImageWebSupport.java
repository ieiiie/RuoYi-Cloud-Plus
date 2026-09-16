package com.ym.agriculture.farming.satellite.support;

import cn.hutool.core.io.FileUtil;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.core.utils.file.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 遥感转存影像 Web 预览：MIME 判定与 ImageIO 标准化。
 */
@Slf4j
public final class SatelliteResultImageWebSupport {

    /** 系统默认 MinIO 对象键前缀（与 {@code sys_oss_config.prefix} 一致）。 */
    public static final String LOCAL_MINIO_OBJECT_KEY_PREFIX = "ymsf/";

    private SatelliteResultImageWebSupport() {
    }

    /**
     * HTTP 下载结果。
     *
     * @param content             响应体字节
     * @param responseContentType 响应头 Content-Type，可为空
     */
    public record DownloadPayload(byte[] content, String responseContentType) {
    }

    /**
     * 标准化后的上传载荷。
     *
     * @param content     字节内容
     * @param contentType MIME 类型
     * @param extension   带点后缀，如 {@code .png}
     */
    public record NormalizedImage(byte[] content, String contentType, String extension) {
    }

    /**
     * 解析上传 MIME：响应头 &gt; 上游后缀 &gt; Magic 字节。
     */
    public static String resolveUploadContentType(String upstreamObjectKey1, String responseContentType, byte[] content) {
        String fromHeader = normalizeImageContentType(responseContentType);
        if (StringUtils.isNotBlank(fromHeader)) {
            return fromHeader;
        }
        String suffix = extractSuffixWithDot(upstreamObjectKey1);
        if (StringUtils.isNotBlank(suffix)) {
            String fromSuffix = FileUtils.getMimeType(suffix);
            if (StringUtils.isNotBlank(fromSuffix) && fromSuffix.startsWith("image/")) {
                return fromSuffix;
            }
        }
        return detectImageContentTypeFromMagic(content);
    }

    /**
     * 将影像标准化为浏览器可 inline 预览的 PNG/JPEG；失败时回退原始字节。
     */
    public static NormalizedImage normalizeForWebPreview(byte[] content, String contentType, String upstreamObjectKey1) {
        if (content == null || content.length == 0) {
            return new NormalizedImage(content, contentType, extractSuffixWithDot(upstreamObjectKey1));
        }
        String resolvedType = StringUtils.isNotBlank(contentType)
            ? contentType
            : resolveUploadContentType(upstreamObjectKey1, null, content);
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
            if (image == null) {
                return fallbackNormalized(content, resolvedType, upstreamObjectKey1);
            }
            if (shouldKeepJpeg(resolvedType, upstreamObjectKey1)) {
                byte[] jpegBytes = writeJpeg(image);
                if (jpegBytes != null) {
                    return new NormalizedImage(jpegBytes, MediaType.IMAGE_JPEG_VALUE, ".jpg");
                }
            }
            byte[] pngBytes = writePng(image);
            if (pngBytes != null) {
                return new NormalizedImage(pngBytes, MediaType.IMAGE_PNG_VALUE, ".png");
            }
        } catch (IOException e) {
            log.warn("遥感影像 Web 标准化失败，回退原始字节 upstreamKey={}", upstreamObjectKey1, e);
        }
        return fallbackNormalized(content, resolvedType, upstreamObjectKey1);
    }

    static String normalizeImageContentType(String rawContentType) {
        if (StringUtils.isBlank(rawContentType)) {
            return null;
        }
        String type = rawContentType.trim();
        int semi = type.indexOf(';');
        if (semi >= 0) {
            type = type.substring(0, semi).trim();
        }
        if (type.startsWith("image/")) {
            return type;
        }
        return null;
    }

    static String detectImageContentTypeFromMagic(byte[] content) {
        if (content == null || content.length < 4) {
            return MediaType.IMAGE_PNG_VALUE;
        }
        if (content[0] == (byte) 0x89 && content[1] == 0x50 && content[2] == 0x4E && content[3] == 0x47) {
            return MediaType.IMAGE_PNG_VALUE;
        }
        if (content[0] == (byte) 0xFF && content[1] == (byte) 0xD8) {
            return MediaType.IMAGE_JPEG_VALUE;
        }
        if (content[0] == 0x47 && content[1] == 0x49 && content[2] == 0x46) {
            return MediaType.IMAGE_GIF_VALUE;
        }
        if (content.length >= 12
            && content[0] == 0x52 && content[1] == 0x49 && content[2] == 0x46 && content[3] == 0x46
            && content[8] == 0x57 && content[9] == 0x45 && content[10] == 0x42 && content[11] == 0x50) {
            return "image/webp";
        }
        if ((content[0] == 0x49 && content[1] == 0x49 && content[2] == 0x2A && content[3] == 0x00)
            || (content[0] == 0x4D && content[1] == 0x4D && content[2] == 0x00 && content[3] == 0x2A)) {
            return "image/tiff";
        }
        return MediaType.IMAGE_PNG_VALUE;
    }

    static String extractSuffixWithDot(String fileName) {
        String ext = FileUtil.extName(StringUtils.blankToDefault(fileName, ""));
        if (StringUtils.isBlank(ext)) {
            return ".png";
        }
        return "." + ext;
    }

    private static NormalizedImage fallbackNormalized(byte[] content, String contentType, String upstreamObjectKey1) {
        String ext = extensionForContentType(contentType, upstreamObjectKey1);
        return new NormalizedImage(content, contentType, ext);
    }

    private static String extensionForContentType(String contentType, String upstreamObjectKey1) {
        if (MediaType.IMAGE_JPEG_VALUE.equals(contentType)) {
            return ".jpg";
        }
        if ("image/gif".equals(contentType)) {
            return ".gif";
        }
        if ("image/webp".equals(contentType)) {
            return ".webp";
        }
        if ("image/tiff".equals(contentType)) {
            return ".tif";
        }
        return extractSuffixWithDot(upstreamObjectKey1);
    }

    private static boolean shouldKeepJpeg(String contentType, String upstreamObjectKey1) {
        if (MediaType.IMAGE_JPEG_VALUE.equals(contentType)) {
            return true;
        }
        String ext = FileUtil.extName(StringUtils.blankToDefault(upstreamObjectKey1, "")).toLowerCase();
        return "jpg".equals(ext) || "jpeg".equals(ext);
    }

    private static byte[] writePng(BufferedImage image) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (ImageIO.write(image, "png", out)) {
                return out.toByteArray();
            }
        } catch (IOException e) {
            log.warn("PNG 写出失败", e);
        }
        return null;
    }

    private static byte[] writeJpeg(BufferedImage image) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (ImageIO.write(image, "jpg", out)) {
                return out.toByteArray();
            }
        } catch (IOException e) {
            log.warn("JPEG 写出失败", e);
        }
        return null;
    }
}
