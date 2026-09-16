package com.ym.agriculture.farming.integration.ai.algback.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 客户分页请求 {@code POST /customer/getCustomerListPageVo}。
 *
 * @author ym-cloud
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgBackCustomerPageRequest {

    /** ≥ 1。 */
    private int pageNum;

    /** ≥ 1 且 ≤ 1000（与中台分页校验一致）。 */
    private int pageSize;

    /** 按名称、手机号模糊 OR 匹配。 */
    private String searchKey;

    /** {@code 0} 停用 / {@code 1} 启用；不传则不过滤。 */
    private Byte status;
}
