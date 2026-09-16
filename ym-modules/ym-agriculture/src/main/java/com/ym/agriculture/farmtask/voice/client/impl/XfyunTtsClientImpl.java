package com.ym.agriculture.farmtask.voice.client.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ym.agriculture.farmtask.voice.client.XfyunAuthHelper;
import com.ym.agriculture.farmtask.voice.client.XfyunClientException;
import com.ym.agriculture.farmtask.voice.client.XfyunTtsClient;
import com.ym.agriculture.farmtask.voice.config.YmStaskVoiceProperties;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 讯飞语音合成 WebSocket 客户端。
 *
 * <p>客户端实例与连接池由 Spring 单例 Bean 复用。异常只携带稳定错误码和可重试属性，
 * 不向业务层暴露第三方完整响应。</p>
 */
@Component
public class XfyunTtsClientImpl implements XfyunTtsClient {

    private static final String DISABLED_CODE = "VOICE_TTS_DISABLED";
    private static final String CONFIG_INVALID_CODE = "VOICE_TTS_CONFIG_INVALID";
    private static final String TIMEOUT_CODE = "VOICE_TTS_TIMEOUT";
    private static final String RATE_LIMITED_CODE = "VOICE_TTS_RATE_LIMITED";
    private static final String SERVER_ERROR_CODE = "VOICE_TTS_SERVER_ERROR";
    private static final String AUTH_FAILED_CODE = "VOICE_TTS_AUTH_FAILED";
    private static final String REQUEST_REJECTED_CODE = "VOICE_TTS_REQUEST_REJECTED";
    private static final String CONNECTION_FAILED_CODE = "VOICE_TTS_CONNECTION_FAILED";
    private static final String RESPONSE_INVALID_CODE = "VOICE_TTS_RESPONSE_INVALID";
    private static final String EMPTY_AUDIO_CODE = "VOICE_TTS_EMPTY_AUDIO";
    private static final String INTERRUPTED_CODE = "VOICE_TTS_INTERRUPTED";

    private final YmStaskVoiceProperties properties;
    private final OkHttpClient httpClient;

    @Autowired
    public XfyunTtsClientImpl(YmStaskVoiceProperties properties) {
        this(properties, buildHttpClient(properties));
    }

    XfyunTtsClientImpl(YmStaskVoiceProperties properties, OkHttpClient httpClient) {
        this.properties = properties;
        this.httpClient = httpClient;
    }

    @Override
    public byte[] synthesizeUyghur(String uyghurText) {
        ensureEnabled();
        Request request = new Request.Builder()
            .url(XfyunAuthHelper.authorizedUrl(properties.getTtsUrl(), "GET", properties))
            .build();
        CountDownLatch done = new CountDownLatch(1);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        AtomicReference<XfyunClientException> failure = new AtomicReference<>();
        AtomicBoolean terminalFrame = new AtomicBoolean(false);
        WebSocket webSocket = httpClient.newWebSocket(request,
            listener(uyghurText, out, failure, terminalFrame, done));
        try {
            boolean completed = done.await(Math.max(1, properties.getTimeout().getRead()), TimeUnit.SECONDS);
            if (!completed) {
                webSocket.cancel();
                throw new XfyunClientException(TIMEOUT_CODE, true, "voice TTS request timed out");
            }
            if (failure.get() != null) {
                throw failure.get();
            }
            byte[] bytes = out.toByteArray();
            if (bytes.length == 0) {
                throw new XfyunClientException(EMPTY_AUDIO_CODE, false, "voice TTS returned empty audio");
            }
            return bytes;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            webSocket.cancel();
            throw new XfyunClientException(INTERRUPTED_CODE, false, "voice TTS wait was interrupted", e);
        }
    }

    private WebSocketListener listener(String uyghurText, ByteArrayOutputStream out,
        AtomicReference<XfyunClientException> failure, AtomicBoolean terminalFrame, CountDownLatch done) {
        return new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                webSocket.send(buildTtsRequest(uyghurText));
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    JSONObject payload = JSON.parseObject(text);
                    int code = payload.getIntValue("code");
                    if (code != 0) {
                        failure.compareAndSet(null, providerFailure(code));
                        done.countDown();
                        webSocket.close(1000, "provider error");
                        return;
                    }
                    JSONObject data = payload.getJSONObject("data");
                    if (data == null) {
                        failure.compareAndSet(null, invalidResponse());
                        done.countDown();
                        webSocket.close(1000, "invalid response");
                        return;
                    }
                    String audio = data.getString("audio");
                    if (StrUtil.isNotBlank(audio)) {
                        out.writeBytes(Base64.getDecoder().decode(audio));
                    }
                    if (data.getIntValue("status") == 2) {
                        terminalFrame.set(true);
                        done.countDown();
                        webSocket.close(1000, "done");
                    }
                } catch (Exception e) {
                    failure.compareAndSet(null, invalidResponse(e));
                    done.countDown();
                    webSocket.close(1000, "invalid response");
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                out.writeBytes(bytes.toByteArray());
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable cause, Response response) {
                if (!terminalFrame.get()) {
                    failure.compareAndSet(null, connectionFailure(cause, response));
                }
                done.countDown();
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                if (!terminalFrame.get()) {
                    failure.compareAndSet(null, invalidResponse());
                }
                done.countDown();
            }
        };
    }

    private String buildTtsRequest(String text) {
        JSONObject body = new JSONObject();
        String tte = StrUtil.blankToDefault(properties.getTtsTextEncoding(), "unicode");
        body.put("common", JSONObject.of("app_id", properties.getAppId()));
        body.put("business", JSONObject.of(
            "aue", StrUtil.blankToDefault(properties.getTtsAue(), "lame"),
            "ent", StrUtil.blankToDefault(properties.getTtsEnt(), "intp65"),
            "vcn", properties.getVoiceName(),
            "tte", tte
        ));
        body.put("data", JSONObject.of(
            "status", 2,
            "text", Base64.getEncoder().encodeToString(text.getBytes(ttsTextCharset(tte)))
        ));
        return body.toJSONString();
    }

    private static Charset ttsTextCharset(String tte) {
        if ("unicode".equalsIgnoreCase(tte)) {
            return StandardCharsets.UTF_16LE;
        }
        return StandardCharsets.UTF_8;
    }

    private void ensureEnabled() {
        if (!properties.isEnabled()) {
            throw new XfyunClientException(DISABLED_CODE, false, "voice TTS is disabled");
        }
        if (StrUtil.hasBlank(properties.getTtsUrl(), properties.getAppId(), properties.getApiKey(),
            properties.getApiSecret(), properties.getVoiceName())) {
            throw new XfyunClientException(CONFIG_INVALID_CODE, false, "voice TTS configuration is incomplete");
        }
    }

    private static OkHttpClient buildHttpClient(YmStaskVoiceProperties properties) {
        YmStaskVoiceProperties.Timeout timeout = properties.getTimeout();
        return new OkHttpClient.Builder()
            .connectTimeout(Math.max(1, timeout.getConnect()), TimeUnit.SECONDS)
            .readTimeout(Math.max(1, timeout.getRead()), TimeUnit.SECONDS)
            .writeTimeout(Math.max(1, timeout.getRead()), TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build();
    }

    private static XfyunClientException providerFailure(int providerCode) {
        if (providerCode == 429) {
            return new XfyunClientException(RATE_LIMITED_CODE, true, "voice TTS provider rate limited request");
        }
        if (providerCode >= 500 && providerCode < 600) {
            return new XfyunClientException(SERVER_ERROR_CODE, true, "voice TTS provider server error");
        }
        if (providerCode == 401 || providerCode == 403) {
            return new XfyunClientException(AUTH_FAILED_CODE, false, "voice TTS provider authentication failed");
        }
        return new XfyunClientException(REQUEST_REJECTED_CODE, false, "voice TTS provider rejected request");
    }

    private static XfyunClientException connectionFailure(Throwable cause, Response response) {
        if (response != null) {
            return providerFailure(response.code());
        }
        if (hasTimeoutCause(cause)) {
            return new XfyunClientException(TIMEOUT_CODE, true, "voice TTS connection timed out", cause);
        }
        return new XfyunClientException(CONNECTION_FAILED_CODE, false, "voice TTS connection failed", cause);
    }

    private static boolean hasTimeoutCause(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static XfyunClientException invalidResponse() {
        return new XfyunClientException(RESPONSE_INVALID_CODE, false, "voice TTS response is invalid");
    }

    private static XfyunClientException invalidResponse(Throwable cause) {
        return new XfyunClientException(RESPONSE_INVALID_CODE, false, "voice TTS response is invalid", cause);
    }
}
