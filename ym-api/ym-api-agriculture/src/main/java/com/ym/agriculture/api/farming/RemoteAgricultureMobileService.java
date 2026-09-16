package com.ym.agriculture.api.farming;

import com.ym.agriculture.api.farming.domain.bo.RemoteAgricultureMobileCommandBo;
import com.ym.agriculture.api.farming.domain.bo.RemoteAgricultureMobileQueryBo;
import com.ym.agriculture.api.farming.domain.vo.RemoteAgricultureViewVo;
import com.ym.common.core.domain.PageResult;

/** 农业小程序首页、天气、资讯与农事记录契约。 */
public interface RemoteAgricultureMobileService {

    RemoteAgricultureViewVo homeOverview(Long fieldId);

    RemoteAgricultureViewVo homeMap(Long fieldId);

    RemoteAgricultureViewVo homeWeather();

    RemoteAgricultureViewVo sensorSummary();

    RemoteAgricultureViewVo weatherAlertSummary();

    PageResult<RemoteAgricultureViewVo> pageWeatherAlerts(RemoteAgricultureMobileQueryBo query);

    RemoteAgricultureViewVo getWeatherAlert(String warningId);

    PageResult<RemoteAgricultureViewVo> pageNews(RemoteAgricultureMobileQueryBo query);

    RemoteAgricultureViewVo getNews(Long articleId);

    RemoteAgricultureViewVo newsDashboard();

    PageResult<RemoteAgricultureViewVo> pageFarmingRecords(RemoteAgricultureMobileQueryBo query);

    RemoteAgricultureViewVo getFarmingRecord(Long recordId);

    RemoteAgricultureViewVo latestFarmingImages(Long fieldId, Integer limit);

    RemoteAgricultureViewVo farmingSensorPreview(RemoteAgricultureMobileQueryBo query, boolean simple);

    RemoteAgricultureViewVo farmingWeatherPreview(RemoteAgricultureMobileQueryBo query);

    RemoteAgricultureViewVo farmingGrowthStagePreview(RemoteAgricultureMobileQueryBo query);

    RemoteAgricultureViewVo addFarmingDraft(RemoteAgricultureMobileCommandBo command);

    RemoteAgricultureViewVo updateFarmingDraft(Long recordId, RemoteAgricultureMobileCommandBo command);

    RemoteAgricultureViewVo submitFarmingDraft(Long recordId, RemoteAgricultureMobileCommandBo command);

    RemoteAgricultureViewVo updateSubmittedFarmingRecord(Long recordId, RemoteAgricultureMobileCommandBo command);

    RemoteAgricultureViewVo deleteFarmingRecord(Long recordId, boolean submitted,
                                                 RemoteAgricultureMobileCommandBo command);

    RemoteAgricultureViewVo patchFarmingMedia(Long recordId, Long mediaId,
                                              RemoteAgricultureMobileCommandBo command);
}
