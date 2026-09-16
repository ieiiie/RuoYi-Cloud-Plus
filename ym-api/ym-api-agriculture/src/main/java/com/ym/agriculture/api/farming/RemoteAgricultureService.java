package com.ym.agriculture.api.farming;

import com.ym.agriculture.api.farming.domain.bo.RemoteMarketQuoteQueryBo;
import com.ym.agriculture.api.farming.domain.vo.RemoteCalendarDateInfoVo;
import com.ym.agriculture.api.farming.domain.vo.RemoteCropVarietySimpleVo;
import com.ym.agriculture.api.farming.domain.vo.RemoteFieldSummaryVo;
import com.ym.agriculture.api.farming.domain.vo.RemoteFarmWorkTreeVo;
import com.ym.agriculture.api.farming.domain.vo.RemoteFarmWorkVo;
import com.ym.agriculture.api.farming.domain.vo.RemotePlantingBatchSummaryVo;
import com.ym.agriculture.api.farming.domain.vo.RemoteSolarTermDetailVo;
import com.ym.agriculture.api.farming.domain.vo.RemoteMarketCategoryVo;
import com.ym.agriculture.api.farming.domain.vo.RemoteMarketQuoteVo;
import com.ym.common.core.domain.PageResult;

import java.util.Date;
import java.util.List;

/**
 * 农业业务只读契约，供小程序聚合服务调用。
 */
public interface RemoteAgricultureService {

    RemoteFieldSummaryVo getField(Long fieldId);

    List<RemoteFieldSummaryVo> listFields();

    List<RemoteCropVarietySimpleVo> listEnabledCropVarieties();

    /**
     * 查询地块当前有效的种植批次。
     *
     * @param fieldIds 地块ID，空集合表示当前租户全部地块
     * @return 每个地块最多一条当前批次
     */
    List<RemotePlantingBatchSummaryVo> listActivePlantingBatches(List<Long> fieldIds);

    List<RemoteFarmWorkTreeVo> listEnabledFarmWorkTree();

    List<RemoteFarmWorkVo> listFarmWorkCategories();

    List<RemoteFarmWorkVo> listEnabledFarmWorkItems(Long categoryId);

    RemoteSolarTermDetailVo getCurrentSolarTerm();

    RemoteSolarTermDetailVo getSolarTerm(String termCode, Integer year);

    RemoteCalendarDateInfoVo getCalendarDateInfo(Date happenedAt);

    PageResult<RemoteMarketQuoteVo> pageMarketQuotes(RemoteMarketQuoteQueryBo query);

    List<RemoteMarketCategoryVo> listMarketCategories();

}
