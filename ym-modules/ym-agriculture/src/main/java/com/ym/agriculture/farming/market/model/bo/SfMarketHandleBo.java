package com.ym.agriculture.farming.market.model.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 行情异常忽略或告警确认参数。 */
@Data
public class SfMarketHandleBo {
    /** 人工处理说明。 */
    @NotBlank
    @Size(max = 500)
    private String comment;
}
