package com.ym.agriculture.farming.field.model.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 地块启停状态请求。
 *
 * @author ym-cloud
 */
@Data
public class SfFieldStatusBo {

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "^[01]$", message = "状态只能为0或1")
    private String status;
}
