package com.ym.agriculture.api.farming.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/** 农业小程序写命令。 */
@Data
public class RemoteAgricultureMobileCommandBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "requestId不能为空")
    private String requestId;
    @NotBlank(message = "businessId不能为空")
    private String businessId;
    private Map<String, Object> payload = new LinkedHashMap<>();
}
