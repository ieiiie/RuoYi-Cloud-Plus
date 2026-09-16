package com.ym.agriculture.farming.algback.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ym.agriculture.farming.integration.ai.algback.dto.common.AlgBackPageVo;
import com.ym.agriculture.farming.integration.ai.algback.dto.model.AlgBackAlgorithmModelListRequest;
import com.ym.agriculture.farming.integration.ai.algback.dto.model.AlgBackAlgorithmModelPageRequest;

/**
 * 算法中台模型列表（需已配置 {@code ym.alg-back} 且中台 token 有效）。
 */
public interface ISfAlgBackAlgorithmModelService {

    /**
     * 非分页列表；中台 {@code getAlgorithmModelList}，{@code data} 一般为 JSON 数组节点。
     */
    JsonNode list(AlgBackAlgorithmModelListRequest request);

    /**
     * 分页列表；中台 {@code getAlgorithmModelListPageVo}。
     */
    AlgBackPageVo page(AlgBackAlgorithmModelPageRequest request);
}
