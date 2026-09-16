package com.ym.agriculture.farming.bigscreen.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 水肥机按天聚合的施肥记录（磷/钾/氮当日合计）。
 */
@Data
public class SfBigscreenFertilizerHistoryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 施肥日期 yyyy-MM-dd */
    private String fertilizeDate;

    /** 当天最后一次施肥时刻 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date fertilizedAt;

    /** 磷肥当日施肥量合计，单位 kg；无数据为 null */
    private BigDecimal phosphorusKg;

    /** 钾肥当日施肥量合计，单位 kg；无数据为 null */
    private BigDecimal potassiumKg;

    /** 氮肥当日施肥量合计，单位 kg；无数据为 null */
    private BigDecimal nitrogenKg;

    /** 当日施肥时长合计（分钟，各批次取最大后累加） */
    private Integer durationMinutes;

    /** 展示文案，如：2026-06-04 磷肥 228kg，钾肥 152kg，氮肥 0kg */
    private String summaryText;
}
