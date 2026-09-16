package com.ym.agriculture.farming.weatheralert.service;

import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farming.weatheralert.model.vo.SfWeatherAlertSummaryVo;
import com.ym.agriculture.farming.weatheralert.model.vo.SfWeatherAlertVo;

/**
 * 气象预警：同步缓存、摘要、列表与详情。
 * <p>本期不提供已读状态。
 */
public interface ISfWeatherAlertService {

    /** 按启用租户涉及省份同步公开预警。 */
    void syncEnabledProvinces();

    /** 当前登录租户地区预警摘要。 */
    SfWeatherAlertSummaryVo summaryForCurrentTenant();

    /**
     * 当前登录租户地区预警分页。
     *
     * @param activeOnly 默认 true
     */
    PageResult<SfWeatherAlertVo> pageForCurrentTenant(Boolean activeOnly, PageQuery pageQuery);

    /** 预警详情；{@code warningId} 按字符串处理。 */
    SfWeatherAlertVo detail(String warningId);
}
