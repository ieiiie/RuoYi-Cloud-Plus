package com.ym.agriculture.farming.integration.ai.algback.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新客户 {@code POST /customer/updateCustomer}；更新时 {@code id}、{@code name}、{@code mobileNum} 等按中台校验。
 * <p>
 * 须将 {@link #status} 置 {@code 1} 启用，否则推理结果不推送 {@code httpReqUrl}。
 *
 * @author ym-cloud
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgBackCustomerUpdateRequest {

    private long id;

    private String name;

    private String mobileNum;

    /** {@code 1} 启用 / {@code 0} 停用。 */
    private Byte status;

    private String httpReqUrl;

    private String httpReqHeader;

    private Integer taskAmountLimit;
}
