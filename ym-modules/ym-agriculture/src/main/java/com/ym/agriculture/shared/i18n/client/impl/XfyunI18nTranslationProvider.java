package com.ym.agriculture.shared.i18n.client.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ym.agriculture.shared.i18n.client.I18nTranslationErrorCode;
import com.ym.agriculture.shared.i18n.client.I18nTranslationException;
import com.ym.agriculture.shared.i18n.client.I18nTranslationProvider;
import com.ym.agriculture.shared.i18n.config.SmartFarmingTranslationProperties;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** 讯飞机器翻译 WebAPI 适配器。 */
@Component
public class XfyunI18nTranslationProvider implements I18nTranslationProvider {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse(ContentType.JSON.getValue());
    private static final DateTimeFormatter RFC1123 = DateTimeFormatter.RFC_1123_DATE_TIME
        .withLocale(Locale.US).withZone(ZoneOffset.UTC);
    private static final String PROVIDER_UYGHUR_LANGUAGE = "uy";

    private final SmartFarmingTranslationProperties properties;
    private final OkHttpClient httpClient;

    @Autowired
    public XfyunI18nTranslationProvider(SmartFarmingTranslationProperties properties) {
        this(properties, buildHttpClient(properties));
    }

    XfyunI18nTranslationProvider(SmartFarmingTranslationProperties properties, OkHttpClient httpClient) {
        this.properties = properties;
        this.httpClient = httpClient;
    }

    @Override
    public String translateZhToUyghur(String sourceText) {
        ensureEnabled();
        if (StrUtil.isBlank(sourceText)) {
            throw new I18nTranslationException(I18nTranslationErrorCode.INVALID_REQUEST, false,
                "待翻译文本不能为空");
        }
        URI endpoint = endpoint();
        JSONObject body = new JSONObject();
        body.put("common", JSONObject.of("app_id", properties.getAppId()));
        body.put("business", JSONObject.of("from", "cn", "to", PROVIDER_UYGHUR_LANGUAGE));
        body.put("data", JSONObject.of("text", Base64.getEncoder()
            .encodeToString(sourceText.getBytes(StandardCharsets.UTF_8))));
        byte[] requestBytes = body.toJSONString().getBytes(StandardCharsets.UTF_8);
        String date = RFC1123.format(ZonedDateTime.now(ZoneOffset.UTC));
        String digest = digestHeader(requestBytes);
        Request request = new Request.Builder()
            .url(properties.getTranslateUrl())
            .header("Accept", "application/json,version=1.0")
            .header("Authorization", authorization(date, digest))
            .header("Date", date)
            .header("Digest", digest)
            .header("Host", endpoint.getHost())
            .post(RequestBody.create(JSON_MEDIA_TYPE, requestBytes))
            .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw httpFailure(response.code());
            }
            if (response.body() == null) {
                throw new I18nTranslationException(I18nTranslationErrorCode.INVALID_RESPONSE, false,
                    "翻译服务响应为空");
            }
            JSONObject result = JSON.parseObject(response.body().string());
            if (result.getIntValue("code") != 0) {
                throw providerBusinessFailure(result.getIntValue("code"));
            }
            String translated = extractText(result);
            if (StrUtil.isBlank(translated)) {
                throw new I18nTranslationException(I18nTranslationErrorCode.INVALID_RESPONSE, false,
                    "翻译服务未返回有效译文");
            }
            return translated;
        } catch (I18nTranslationException e) {
            throw e;
        } catch (SocketTimeoutException e) {
            throw new I18nTranslationException(I18nTranslationErrorCode.TIMEOUT, true,
                "翻译服务调用超时", e);
        } catch (IOException e) {
            throw new I18nTranslationException(I18nTranslationErrorCode.NETWORK_ERROR, false,
                "翻译服务网络异常", e);
        } catch (RuntimeException e) {
            throw new I18nTranslationException(I18nTranslationErrorCode.INVALID_RESPONSE, false,
                "翻译服务响应解析失败", e);
        }
    }

    private void ensureEnabled() {
        if (!properties.isEnabled()) {
            throw new I18nTranslationException(I18nTranslationErrorCode.CONFIG_DISABLED, false,
                "业务文本翻译未启用");
        }
        if (StrUtil.hasBlank(properties.getTranslateUrl(), properties.getAppId(), properties.getApiKey(),
            properties.getApiSecret())) {
            throw new I18nTranslationException(I18nTranslationErrorCode.CONFIG_INVALID, false,
                "业务文本翻译配置不完整");
        }
    }

    private URI endpoint() {
        try {
            URI endpoint = URI.create(properties.getTranslateUrl());
            if (StrUtil.isBlank(endpoint.getScheme()) || StrUtil.isBlank(endpoint.getHost())) {
                throw new IllegalArgumentException("missing scheme or host");
            }
            return endpoint;
        } catch (IllegalArgumentException e) {
            throw new I18nTranslationException(I18nTranslationErrorCode.CONFIG_INVALID, false,
                "业务文本翻译地址无效", e);
        }
    }

    private String authorization(String date, String digest) {
        URI uri = URI.create(properties.getTranslateUrl());
        String path = StrUtil.blankToDefault(uri.getRawPath(), "/");
        if (StrUtil.isNotBlank(uri.getRawQuery())) {
            path = path + "?" + uri.getRawQuery();
        }
        String origin = "host: " + uri.getHost() + "\n"
            + "date: " + date + "\n"
            + "POST " + path + " HTTP/1.1\n"
            + "digest: " + digest;
        String signature = hmacSha256Base64(origin, properties.getApiSecret());
        return "api_key=\"" + properties.getApiKey()
            + "\", algorithm=\"hmac-sha256\", headers=\"host date request-line digest\", signature=\""
            + signature + "\"";
    }

    private String digestHeader(byte[] body) {
        try {
            return "SHA-256=" + Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(body));
        } catch (Exception e) {
            throw new I18nTranslationException(I18nTranslationErrorCode.INVALID_REQUEST, false,
                "翻译请求摘要计算失败", e);
        }
    }

    private String hmacSha256Base64(String value, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new I18nTranslationException(I18nTranslationErrorCode.CONFIG_INVALID, false,
                "翻译请求签名失败", e);
        }
    }

    private static OkHttpClient buildHttpClient(SmartFarmingTranslationProperties properties) {
        SmartFarmingTranslationProperties.Timeout timeout = properties.getTimeout();
        return new OkHttpClient.Builder().protocols(List.of(Protocol.HTTP_1_1))
            .connectTimeout(Math.max(1, timeout.getConnect()), TimeUnit.SECONDS)
            .readTimeout(Math.max(1, timeout.getRead()), TimeUnit.SECONDS)
            .writeTimeout(Math.max(1, timeout.getRead()), TimeUnit.SECONDS)
            .retryOnConnectionFailure(false).build();
    }

    private static I18nTranslationException httpFailure(int statusCode) {
        if (statusCode == 401 || statusCode == 403) {
            return new I18nTranslationException(I18nTranslationErrorCode.AUTHENTICATION_FAILED, false,
                "翻译服务鉴权失败");
        }
        if (statusCode == 400 || statusCode == 404 || statusCode == 405 || statusCode == 422) {
            return new I18nTranslationException(I18nTranslationErrorCode.INVALID_REQUEST, false,
                "翻译服务请求无效");
        }
        if (statusCode == 429) {
            return new I18nTranslationException(I18nTranslationErrorCode.RATE_LIMITED, true,
                "翻译服务请求受限");
        }
        if (statusCode >= 500 || statusCode == 408 || statusCode == 425) {
            return new I18nTranslationException(I18nTranslationErrorCode.PROVIDER_UNAVAILABLE, true,
                "翻译服务暂时不可用");
        }
        return new I18nTranslationException(I18nTranslationErrorCode.INVALID_REQUEST, false,
            "翻译服务拒绝请求");
    }

    private static I18nTranslationException providerBusinessFailure(int providerCode) {
        if (providerCode == 401 || providerCode == 403) {
            return new I18nTranslationException(I18nTranslationErrorCode.AUTHENTICATION_FAILED, false,
                "翻译服务鉴权失败");
        }
        if (providerCode == 429) {
            return new I18nTranslationException(I18nTranslationErrorCode.RATE_LIMITED, true,
                "翻译服务请求受限");
        }
        if (providerCode >= 500 && providerCode < 600) {
            return new I18nTranslationException(I18nTranslationErrorCode.PROVIDER_UNAVAILABLE, true,
                "翻译服务暂时不可用");
        }
        return new I18nTranslationException(I18nTranslationErrorCode.INVALID_RESPONSE, false,
            "翻译服务拒绝请求");
    }

    private static String extractText(JSONObject result) {
        JSONObject data = result.getJSONObject("data");
        if (data == null) {
            return null;
        }
        JSONObject resultObj = data.getJSONObject("result");
        if (resultObj != null) {
            JSONObject translated = resultObj.getJSONObject("trans_result");
            if (translated != null && StrUtil.isNotBlank(translated.getString("dst"))) {
                return translated.getString("dst");
            }
            if (StrUtil.isNotBlank(resultObj.getString("dst"))) {
                return resultObj.getString("dst");
            }
            return resultObj.getString("text");
        }
        JSONObject translated = data.getJSONObject("trans_result");
        if (translated != null && StrUtil.isNotBlank(translated.getString("dst"))) {
            return translated.getString("dst");
        }
        return StrUtil.blankToDefault(data.getString("dst"), data.getString("result"));
    }
}
