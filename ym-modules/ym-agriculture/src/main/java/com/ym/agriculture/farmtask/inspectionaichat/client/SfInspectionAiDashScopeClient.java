package com.ym.agriculture.farmtask.inspectionaichat.client;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ym.common.core.exception.ServiceException;
import com.ym.agriculture.farmtask.inspectionaichat.config.SfInspectionAiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** 百炼 OpenAI 兼容接口客户端。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SfInspectionAiDashScopeClient {
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private final SfInspectionAiProperties properties;

    /** 流式调用视觉模型。 */
    public Usage streamVision(List<JSONObject> messages, Consumer<String> deltaConsumer) {
        JSONObject body = new JSONObject();
        body.put("model", properties.getModelId());
        body.put("stream", true);
        JSONObject streamOptions = new JSONObject();
        streamOptions.put("include_usage", true);
        body.put("stream_options", streamOptions);
        body.put("messages", messages);
        return executeStream(body, deltaConsumer);
    }

    /** 非流式调用摘要模型。 */
    public Completion summarize(String modelId, String prompt) {
        JSONObject body = new JSONObject();
        body.put("model", modelId);
        body.put("temperature", properties.getSummary().getTemperature());
        body.put("top_k", properties.getSummary().getTopK());
        JSONArray messages = new JSONArray();
        messages.add(message("system", "你负责将农业巡查对话压缩为结构化中文历史摘要，不得编造观察结论。"));
        messages.add(message("user", prompt));
        body.put("messages", messages);
        try (Response response = newCall(body).execute()) {
            String raw = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) throw failure(response.code(), raw);
            JSONObject data = JSON.parseObject(raw);
            JSONObject choice = data.getJSONArray("choices").getJSONObject(0);
            String content = choice.getJSONObject("message").getString("content");
            return new Completion(content, data.getString("id"), usage(data.getJSONObject("usage")));
        } catch (IOException exception) {
            throw new ServiceException("AI 服务网络异常，请稍后重试");
        }
    }

    private Usage executeStream(JSONObject body, Consumer<String> deltaConsumer) {
        try (Response response = newCall(body).execute()) {
            if (!response.isSuccessful()) {
                String raw = response.body() == null ? "" : response.body().string();
                throw failure(response.code(), raw);
            }
            ResponseBody responseBody = response.body();
            if (responseBody == null) throw new ServiceException("AI 服务返回为空");
            Usage usage = Usage.EMPTY;
            String providerRequestId = null;
            try (BufferedReader reader = new BufferedReader(responseBody.charStream())) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) continue;
                    String data = StrUtil.trim(line.substring(5));
                    if ("[DONE]".equals(data)) break;
                    JSONObject chunk = JSON.parseObject(data);
                    providerRequestId = StrUtil.blankToDefault(chunk.getString("id"), providerRequestId);
                    JSONObject chunkUsage = chunk.getJSONObject("usage");
                    if (chunkUsage != null) usage = usage(chunkUsage);
                    JSONArray choices = chunk.getJSONArray("choices");
                    if (choices == null || choices.isEmpty()) continue;
                    JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
                    String content = delta == null ? null : delta.getString("content");
                    if (StrUtil.isNotBlank(content)) deltaConsumer.accept(content);
                }
            }
            return new Usage(usage.inputTokens(), usage.outputTokens(), usage.totalTokens(), providerRequestId);
        } catch (IOException exception) {
            throw new ServiceException("AI 服务网络异常，请稍后重试");
        }
    }

    private Call newCall(JSONObject body) {
        if (!properties.isEnabled() || StrUtil.hasBlank(properties.getBaseUrl(), properties.getApiKey())) {
            throw new ServiceException("巡查照片 AI 问答未配置或未启用");
        }
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(properties.getConnectTimeoutSeconds(), TimeUnit.SECONDS)
            .readTimeout(properties.getReadTimeoutSeconds(), TimeUnit.SECONDS)
            .build();
        String url = StrUtil.removeSuffix(properties.getBaseUrl(), "/") + properties.getChatCompletionsPath();
        Request request = new Request.Builder().url(url)
            .header("Authorization", "Bearer " + properties.getApiKey())
            .header("Content-Type", "application/json")
            .post(RequestBody.create(JSON_MEDIA_TYPE, JSON.toJSONString(body))).build();
        return client.newCall(request);
    }

    private static JSONObject message(String role, Object content) {
        JSONObject message = new JSONObject();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private static Usage usage(JSONObject usage) {
        if (usage == null) return Usage.EMPTY;
        return new Usage(usage.getInteger("prompt_tokens"), usage.getInteger("completion_tokens"), usage.getInteger("total_tokens"), null);
    }

    private static ServiceException failure(int status, String body) {
        log.warn("百炼调用失败，status={}，响应长度={}", status, body == null ? 0 : body.length());
        return new ServiceException(status == 429 ? "AI 服务繁忙，请稍后重试" : "AI 服务调用失败，请稍后重试");
    }

    /** 模型调用 Token 使用量。 */
    public record Usage(Integer inputTokens, Integer outputTokens, Integer totalTokens, String providerRequestId) {
        public static final Usage EMPTY = new Usage(null, null, null, null);
    }

    /** 非流式完成结果。 */
    public record Completion(String content, String providerRequestId, Usage usage) { }
}
