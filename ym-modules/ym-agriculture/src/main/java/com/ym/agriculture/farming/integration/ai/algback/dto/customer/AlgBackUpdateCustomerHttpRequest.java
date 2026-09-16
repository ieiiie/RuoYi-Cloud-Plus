package com.ym.agriculture.farming.integration.ai.algback.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新客户 HTTP 回调 {@code POST /customer/updateCustomerHttp}。
 *
 * @author ym-cloud
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgBackUpdateCustomerHttpRequest {

    private String customerNo;

    private String httpReqUrl;

    /** 不传则中台可能保留库中原有头；传则须为合法 JSON 字符串。 */
    private String httpReqHeader;
}
