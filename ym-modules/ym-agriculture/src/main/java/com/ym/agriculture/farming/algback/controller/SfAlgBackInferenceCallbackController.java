package com.ym.agriculture.farming.algback.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ym.agriculture.farming.algback.model.dto.AlgBackInferencePushDto;
import com.ym.agriculture.farming.algback.model.vo.AlgBackCallbackAckVo;
import com.ym.agriculture.farming.algback.service.ISfAlgBackInferenceCallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 算法中台推理结果推送入口；公网需可达并配置在中台客户 {@code httpReqUrl}。
 * <p>
 * 响应须满足中台约定：JSON 中 {@code code} 为 Integer 且为 200。已加入 {@code security.excludes} 与 XSS 排除。
 *
 * @author ym-cloud
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/smart-farming/ai/callback")
public class SfAlgBackInferenceCallbackController {

    private final ISfAlgBackInferenceCallbackService callbackService;
    private final ObjectMapper objectMapper;

    /**
     * 接收算法中台推理推送（原始 JSON 落库并异步处理；异常仍返回 {@code code=200} 以满足中台重试策略）
     *
     * @param body 推送 JSON 反序列化前的 Map，结构与 {@link AlgBackInferencePushDto} 一致
     * @return 中台确认体，{@link AlgBackCallbackAckVo#ok()}
     */
    @PostMapping("/inference")
    public AlgBackCallbackAckVo receiveInference(@RequestBody Map<String, Object> body) {
        try {
            String raw = objectMapper.writeValueAsString(body);
            AlgBackInferencePushDto dto = objectMapper.convertValue(body, AlgBackInferencePushDto.class);
            callbackService.handleInferencePush(dto, raw);
            return AlgBackCallbackAckVo.ok();
        } catch (JsonProcessingException e) {
            log.error("算法中台回调 JSON 序列化失败", e);
            return AlgBackCallbackAckVo.ok();
        } catch (Exception e) {
            log.error("算法中台回调处理异常", e);
            return AlgBackCallbackAckVo.ok();
        }
    }
}
