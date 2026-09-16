package com.ym.agriculture.farming.farmwork.model.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * stask 农事字典排序入参。
 *
 * @author ym-cloud
 */
@Data
public class SfFarmWorkDictSortBo {

    /**
     * 农事字典主键。
     */
    @NotNull(message = "农事字典ID不能为空")
    private Long dictId;

    /**
     * 排序序号，越小越靠前。
     */
    @NotNull(message = "排序序号不能为空")
    private Integer sortOrder;
}
