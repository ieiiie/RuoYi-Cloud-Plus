package com.ym.agriculture.api.farmtask.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 农事任务写命令公共幂等字段。 */
@Data
public class RemoteTaskCommandBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 客户端生成且重试保持不变的请求号。 */
    private String requestId;

    /** 可用于业务追踪的稳定业务号。 */
    private String businessId;
}
