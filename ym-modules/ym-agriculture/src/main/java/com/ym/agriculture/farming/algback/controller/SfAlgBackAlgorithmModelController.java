package com.ym.agriculture.farming.algback.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.ym.agriculture.farming.integration.ai.algback.dto.common.AlgBackPageVo;
import com.ym.agriculture.farming.integration.ai.algback.dto.model.AlgBackAlgorithmModelListRequest;
import com.ym.agriculture.farming.integration.ai.algback.dto.model.AlgBackAlgorithmModelPageRequest;
import com.ym.common.core.domain.R;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.web.core.BaseController;
import com.ym.agriculture.farming.algback.service.ISfAlgBackAlgorithmModelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 算法中台模型列表与分页（供前端选择 {@code modelNo} 创建任务）。
 *
 * @author ym-cloud
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/smart-farming/ai/algorithm-models")
public class SfAlgBackAlgorithmModelController extends BaseController {

    private final ISfAlgBackAlgorithmModelService algorithmModelService;

    /**
     * 查询模型列表（非分页）
     *
     * @param body 筛选条件，可空表示默认列表
     * @return 统一响应，{@code data} 为中台返回的 JSON 节点
     */
    @Log(title = "算法中台模型列表", businessType = BusinessType.OTHER)
    @PostMapping("/list")
    public R<JsonNode> list(@RequestBody(required = false) AlgBackAlgorithmModelListRequest body) {
        return R.ok(algorithmModelService.list(body));
    }

    /**
     * 模型分页查询
     *
     * @param body 分页请求（页码、条数等）
     * @return 统一响应，{@code data} 为中台分页结构 {@link AlgBackPageVo}
     */
    @Log(title = "算法中台模型分页", businessType = BusinessType.OTHER)
    @PostMapping("/page")
    public R<AlgBackPageVo> page(@Valid @RequestBody AlgBackAlgorithmModelPageRequest body) {
        return R.ok(algorithmModelService.page(body));
    }
}
