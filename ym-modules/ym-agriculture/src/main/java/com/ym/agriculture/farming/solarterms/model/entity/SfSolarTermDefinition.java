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
 * 二十四节气定义内容，对应 {@code sf_solar_term_definition}。
 * <p>平台全局表，无 tenant_id，需加入 tenant.excludes。
 */
@Data
@NoArgsConstructor
@TableName("sf_solar_term_definition")
public class SfSolarTermDefinition implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "term_code", type = IdType.INPUT)
    private String termCode;

    private String termName;

    private Integer termOrder;

    private Integer solarLongitude;

    private String intro;

    private String seasonalDescription;

    /** JSON 数组字符串 */
    private String customsJson;

    private String contentVersion;

    private Integer enabledFlag;

    private Date createTime;

    private Date updateTime;
}
