package com.ym.agriculture.farmtask.inspectionaichat.model.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 重新生成回答请求。 */
@Data
public class SfInspectionAiRegenerateBo {
    /** 前端生成的幂等 UUID。 */
    @NotBlank(message = "请求标识不能为空")
    private String clientRequestId;
}
