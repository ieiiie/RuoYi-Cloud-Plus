package com.ym.agriculture.farmtask.workorder.model.vo;

import lombok.Data;

import java.math.BigDecimal;

/** 任务农事项物料快照，用于草稿回显和详情展示。 */
@Data
public class SfStaskTaskMaterialVo {

    private Long inventoryMaterialId;
    private String materialCode;
    private String materialName;
    private String specification;
    private String unit;
    private BigDecimal quantity;
}
