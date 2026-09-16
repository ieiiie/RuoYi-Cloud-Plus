package com.ym.agriculture.farmtask.workorder.model.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 技术员任务农事项的库存物资需求。
 *
 * <p>数量由库存领域统一校验为一位小数；零数量在服务端规范化时忽略。</p>
 */
@Data
public class SfStaskTaskMaterialBo {

    /** 新库存物资 ID。 */
    @NotNull(message = "物资ID不能为空")
    private Long inventoryMaterialId;

    /** 当前农事项在整个任务包中的物资总量。 */
    @NotNull(message = "物资数量不能为空")
    private BigDecimal quantity;
}
