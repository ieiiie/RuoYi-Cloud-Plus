package com.ym.agriculture.farming.bigscreen.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 大屏顶栏设备在线率。
 */
@Data
public class SfBigscreenOnlineRateVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 在线率百分比，0–100 */
    private Integer pct;

    /** 在线设备数 */
    private Integer online;

    /** 设备总数 */
    private Integer total;

    /** 离线设备数 */
    private Integer offline;

    /** 状态待确认数量，不计入在线率分母。 */
    private Integer unknown;

    private Integer confirmed;

    /** 统计范围说明，如 TENANT_ALL_DEVICES */
    private String scope;
}
