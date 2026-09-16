package com.ym.agriculture.farming.market.model.bo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/** 手动处理农业行情采集队列参数。 */
@Data
public class SfMarketIngestProcessBo {
    /** 单批处理数量，范围 1 至 200。 */
    @Min(1)
    @Max(200)
    private Integer limit = 50;
}
