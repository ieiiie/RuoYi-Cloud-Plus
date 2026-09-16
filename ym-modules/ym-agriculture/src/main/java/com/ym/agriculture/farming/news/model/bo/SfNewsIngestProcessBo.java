package com.ym.agriculture.farming.news.model.bo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 手动触发采集暂存处理的入参。 */
@Data
public class SfNewsIngestProcessBo {

    /** 本批最多处理条数，范围1-100。 */
    @Min(value = 1, message = "处理数量不能小于1")
    @Max(value = 100, message = "处理数量不能超过100")
    @NotNull(message = "处理数量不能为空")
    private Integer limit = 20;
}
