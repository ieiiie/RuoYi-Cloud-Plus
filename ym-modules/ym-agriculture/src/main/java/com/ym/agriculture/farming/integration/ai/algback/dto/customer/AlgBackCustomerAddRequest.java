package com.ym.agriculture.farming.integration.ai.algback.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建客户请求 {@code POST /customer/addCustomer}。
 * <p>
 * 新客户默认停用且不返回 id/customerNo；启用需再调 updateCustomer。
 *
 * @author ym-cloud
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgBackCustomerAddRequest {

    private String name;

    /** 须符合中台 DTO 内大陆手机号正则。 */
    private String mobileNum;

    /** 推理结果 POST 推送完整 URL；与 {@code httpReqHeader} 一并入库。 */
    private String httpReqUrl;

    /** 自定义请求头 JSON 字符串；非空时须为合法 JSON。 */
    private String httpReqHeader;

    /** 任务路数上限；服务端可能写入默认 10。 */
    private Integer taskAmountLimit;

    /** 请求体传入仍会被服务端覆盖为停用；启用请用 {@link AlgBackCustomerUpdateRequest}。 */
    private Byte status;
}
