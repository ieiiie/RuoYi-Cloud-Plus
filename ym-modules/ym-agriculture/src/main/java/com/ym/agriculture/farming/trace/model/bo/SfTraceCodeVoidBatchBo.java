package com.ym.agriculture.farming.trace.model.bo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量作废溯源码请求体。
 */
@Data
public class SfTraceCodeVoidBatchBo {

    @NotEmpty(message = "溯源码ID列表不能为空")
    private List<Long> traceCodeIds;
}
