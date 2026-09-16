package com.ym.agriculture.farming.crop.model.bo;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 作物状态更新对象。
 */
@Data
public class SfCropStatusBo {

    @Pattern(regexp = "^[01]$", message = "状态只能为0或1")
    private String status;
}
