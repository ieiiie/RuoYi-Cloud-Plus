package com.ym.agriculture.farming.integration.ai.algback.dto.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型列表（非分页）{@code POST /algorithmModel/getAlgorithmModelList}。
 *
 * @author ym-cloud
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlgBackAlgorithmModelListRequest {

    /**
     * 文档字段；当前中台实现可能未参与查询，保留以与 Swagger/文档一致。限条请用分页接口。
     */
    private Integer limit;
}
