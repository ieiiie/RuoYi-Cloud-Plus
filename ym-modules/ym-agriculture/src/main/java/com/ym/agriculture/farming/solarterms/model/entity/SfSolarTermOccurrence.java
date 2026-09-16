package com.ym.agriculture.farming.solarterms.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 具体年份节气交节时间，对应 {@code sf_solar_term_occurrence}。
 */
@Data
@NoArgsConstructor
@TableName("sf_solar_term_occurrence")
public class SfSolarTermOccurrence implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "occurrence_id", type = IdType.ASSIGN_ID)
    private Long occurrenceId;

    private Integer termYear;

    private String termCode;

    private String termName;

    private Date occurredAt;

    private Date gregorianDate;

    private String lunarDateText;

    private String algorithmVersion;

    private Date createTime;

    private Date updateTime;
}
