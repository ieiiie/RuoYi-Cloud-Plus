package com.ym.agriculture.farmtask.voice.client;

import cn.hutool.core.util.StrUtil;
import com.ym.agriculture.farmtask.voice.config.YmStaskVoiceProperties;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;

/**
 * 讯飞 WebAPI 鉴权工具。
 */
public final class XfyunAuthHelper {

    private static final DateTimeFormatter RFC1123 = DateTimeFormatter.RFC_1123_DATE_TIME
        .withLocale(Locale.US)
        .withZone(ZoneOffset.UTC);

    private XfyunAuthHelper() {
    }

    public static String authorizationHeader(String url, String method, YmStaskVoiceProperties properties, String date) {
        String signatureOrigin = signatureOrigin(url, method, date);
        String signatureSha = hmacSha256Base64(signatureOrigin, properties.getApiSecret());
        String authorizationOrigin = "api_key=\"" + properties.getApiKey()
            + "\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\""
            + signatureSha + "\"";
        return Base64.getEncoder().encodeToString(authorizationOrigin.getBytes(StandardCharsets.UTF_8));
    }

    public static String signatureOrigin(String url, String method, String date) {
        URI uri = URI.create(url);
        String host = uri.getHost();
        String path = StrUtil.blankToDefault(uri.getRawPath(), "/");
        if (StrUtil.isNotBlank(uri.getRawQuery())) {
            path = path + "?" + uri.getRawQuery();
        }
        return "host: " + host + "\n"
            + "date: " + date + "\n"
            + method.toUpperCase(Locale.ROOT) + " " + path + " HTTP/1.1";
    }

    public static String authorizedUrl(String url, String method, YmStaskVoiceProperties properties) {
        String date = rfc1123Date();
        URI uri = URI.create(url);
        if (StrUtil.hasBlank(uri.getScheme(), uri.getHost())) {
            throw new XfyunClientException("VOICE_PROVIDER_INVALID_URL", false,
                "voice provider URL is invalid");
        }
        String separator = StrUtil.isBlank(uri.getRawQuery()) ? "?" : "&";
        return url + separator
            + "authorization=" + encodeQueryParam(authorizationHeader(url, method, properties, date))
            + "&date=" + encodeQueryParam(date)
            + "&host=" + encodeQueryParam(uri.getHost());
    }

    public static String digestHeader(byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "SHA-256=" + Base64.getEncoder().encodeToString(digest.digest(body));
        } catch (Exception e) {
            throw new XfyunClientException("VOICE_PROVIDER_DIGEST_FAILED", false,
                "voice provider request digest failed", e);
        }
    }

    public static String httpDigestAuthorizationHeader(String url, String method, YmStaskVoiceProperties properties,
        String date, String digest) {
        String signatureOrigin = digestSignatureOrigin(url, method, date, digest);
        String signatureSha = hmacSha256Base64(signatureOrigin, properties.getApiSecret());
        return "api_key=\"" + properties.getApiKey()
            + "\", algorithm=\"hmac-sha256\", headers=\"host date request-line digest\", signature=\""
            + signatureSha + "\"";
    }

    public static String digestSignatureOrigin(String url, String method, String date, String digest) {
        URI uri = URI.create(url);
        String host = uri.getHost();
        String path = StrUtil.blankToDefault(uri.getRawPath(), "/");
        if (StrUtil.isNotBlank(uri.getRawQuery())) {
            path = path + "?" + uri.getRawQuery();
        }
        return "host: " + host + "\n"
            + "date: " + date + "\n"
            + method.toUpperCase(Locale.ROOT) + " " + path + " HTTP/1.1\n"
            + "digest: " + digest;
    }

    public static String rfc1123Date() {
        return RFC1123.format(ZonedDateTime.now(ZoneOffset.UTC));
    }

    private static String encodeQueryParam(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String hmacSha256Base64(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new XfyunClientException("VOICE_PROVIDER_AUTH_FAILED", false,
                "voice provider authorization signature failed", e);
        }
    }
}
