package com.ym.agriculture.farming.algback.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.ym.agriculture.farming.integration.ai.algback.api.AlgBackApi;
import com.ym.agriculture.farming.integration.ai.algback.client.AlgBackClientException;
import com.ym.agriculture.farming.integration.ai.algback.client.AlgBackSync;
import com.ym.agriculture.farming.integration.ai.algback.dto.common.AlgBackPageVo;
import com.ym.agriculture.farming.integration.ai.algback.dto.model.AlgBackAlgorithmModelListRequest;
import com.ym.agriculture.farming.integration.ai.algback.dto.model.AlgBackAlgorithmModelPageRequest;
import com.ym.common.core.exception.ServiceException;
import com.ym.agriculture.farming.algback.service.ISfAlgBackAlgorithmModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 模型列表不依赖租户 {@code customerNo}，使用中台登录 token 即可调用。
 */
@RequiredArgsConstructor
@Service
public class SfAlgBackAlgorithmModelServiceImpl implements ISfAlgBackAlgorithmModelService {

    private final ObjectProvider<AlgBackApi> algBackApiProvider;

    @Override
    @Transactional(rollbackFor = Exception.class, readOnly = true)
    public JsonNode list(AlgBackAlgorithmModelListRequest request) {
        AlgBackApi api = requireApi();
        AlgBackAlgorithmModelListRequest body = request != null ? request : new AlgBackAlgorithmModelListRequest();
        try {
            JsonNode data = AlgBackSync.execute(api.getAlgorithmModelList(body));
            return data != null ? data : JsonNodeFactory.instance.arrayNode();
        } catch (AlgBackClientException e) {
            throw new ServiceException("调用算法中台失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, readOnly = true)
    public AlgBackPageVo page(AlgBackAlgorithmModelPageRequest request) {
        AlgBackApi api = requireApi();
        if (request == null) {
            throw new ServiceException("分页参数不能为空");
        }
        if (request.getPageNum() < 1 || request.getPageSize() < 1) {
            throw new ServiceException("pageNum、pageSize 须 ≥ 1");
        }
        if (request.getPageSize() > 1000) {
            throw new ServiceException("pageSize 不能超过 1000");
        }
        try {
            AlgBackPageVo data = AlgBackSync.execute(api.getAlgorithmModelListPageVo(request));
            return data != null ? data : new AlgBackPageVo();
        } catch (AlgBackClientException e) {
            throw new ServiceException("调用算法中台失败: " + e.getMessage());
        }
    }

    private AlgBackApi requireApi() {
        AlgBackApi api = algBackApiProvider.getIfAvailable();
        if (api == null) {
            throw new ServiceException("未配置算法中台，请在应用中配置 ym.alg-back.base-url 并完成登录");
        }
        return api;
    }
}
