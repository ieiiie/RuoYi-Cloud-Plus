package com.ym.agriculture.farming.integration.ai.algback.dto.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 模型分页 {@code POST /algorithmModel/getAlgorithmModelListPageVo}。
 *
 * @author ym-cloud
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgBackAlgorithmModelPageRequest {

    private int pageNum;

    private int pageSize;

    private String searchKey;

    private Long algorithmType;

    private Byte status;

    /** 按模型号列表筛选。 */
    private List<String> modelNoList;
}
