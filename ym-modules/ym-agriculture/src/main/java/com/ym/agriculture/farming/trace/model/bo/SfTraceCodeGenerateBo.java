package com.ym.agriculture.farming.trace.model.bo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 批量生成溯源码请求体。
 */
@Data
public class SfTraceCodeGenerateBo {

    @NotNull(message = "生成数量不能为空")
    @Min(value = 1, message = "生成数量至少为1")
    @Max(value = 5000, message = "单次生成数量不能超过5000")
    private Integer quantity;
}
