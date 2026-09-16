package com.ym.agriculture.farming.solarterms.service;

import com.ym.agriculture.farming.solarterms.model.vo.CalendarDateInfoVo;
import com.ym.agriculture.farming.solarterms.model.vo.SfSolarTermDetailVo;

import java.util.Date;

/**
 * 节气查询与年份数据确保。
 * <p>本期仅返回通用知识内容，不提供农事建议。
 */
public interface ISfSolarTermService {

    /** 当前北京时间下的当前节气及下一节气摘要。 */
    SfSolarTermDetailVo getCurrent();

    /**
     * 指定年份节气详情；{@code year} 为空时取当前年。
     */
    SfSolarTermDetailVo getByTermCode(String termCode, Integer year);

    /**
     * 查询指定北京时间对应的农历日期与当前所处节气。
     */
    CalendarDateInfoVo getCalendarDateInfo(Date happenedAt);

    /** 确保指定年及相邻年份交节数据已落库。 */
    void ensureYears(int centerYear);
}
