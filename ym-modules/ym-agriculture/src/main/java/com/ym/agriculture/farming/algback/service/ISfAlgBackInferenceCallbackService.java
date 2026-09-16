package com.ym.agriculture.farming.algback.service;

import com.ym.agriculture.farming.algback.model.dto.AlgBackInferencePushDto;

/**
 * 算法中台推理结果 HTTP 推送落库。
 */
public interface ISfAlgBackInferenceCallbackService {

    void handleInferencePush(AlgBackInferencePushDto dto, String rawJson);
}
