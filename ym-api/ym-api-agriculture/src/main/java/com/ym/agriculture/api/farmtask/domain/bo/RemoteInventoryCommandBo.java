package com.ym.agriculture.api.farmtask.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/** 库存 V1.5 写命令；requestId 必须由调用方在重试时保持不变。 */
@Data
public class RemoteInventoryCommandBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "requestId不能为空")
    private String requestId;

    @NotBlank(message = "businessId不能为空")
    private String businessId;

    private Map<String, Object> payload = new LinkedHashMap<>();
}
