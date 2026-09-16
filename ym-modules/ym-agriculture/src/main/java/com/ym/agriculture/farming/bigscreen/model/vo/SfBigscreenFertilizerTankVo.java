package com.ym.agriculture.farming.bigscreen.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 大屏水肥机单罐液位。
 */
@Data
public class SfBigscreenFertilizerTankVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 罐号：1 / 2 / 3 */
    private Integer tankNo;

    /** 展示名称：磷肥 / 钾肥 / 氮肥（与 tankNo 1/2/3 对应） */
    private String name;

    /** 液位高度，单位：米；无实时数据时为 null */
    private BigDecimal liquidLevelM;

    /** 液位单位，固定 m */
    private String unit;
}
