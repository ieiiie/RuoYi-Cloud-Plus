package com.ym.agriculture.farming.solarterms.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 指定公历时刻对应的农历与当前节气信息。
 *
 * <p>所有字段均按北京时间计算，供管理端和移动端复用。</p>
 */
@Data
public class CalendarDateInfoVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 公历日期时间，格式：yyyy-MM-dd HH:mm:ss。 */
    private String gregorianDateTime;

    /** 农历日期，如：二〇二六年七月初五。 */
    private String lunarDateText;

    /** 当前所处节气编码。 */
    private String solarTermCode;

    /** 当前所处节气名称。 */
    private String solarTermName;

    /** 当前节气的交节时刻，格式：yyyy-MM-dd HH:mm:ss。 */
    private String solarTermOccurredAt;

    /** 固定为 Asia/Shanghai。 */
    private String timeZone;

    /** 农历与节气计算算法版本。 */
    private String algorithmVersion;
}
