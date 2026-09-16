package com.ym.agriculture.farming.bigscreen.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 土壤墒情分层测点。
 */
@Data
public class SfBigscreenSoilDepthVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 埋深，单位：cm */
    private Integer depth;

    /** 温度，单位：℃ */
    private BigDecimal temp;

    /** 湿度，单位：% */
    private BigDecimal humidity;

    /** 电导率 EC */
    private BigDecimal ec;
}
