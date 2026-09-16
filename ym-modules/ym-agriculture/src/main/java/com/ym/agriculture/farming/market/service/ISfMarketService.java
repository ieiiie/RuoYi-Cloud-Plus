package com.ym.agriculture.farming.market.service;

import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farming.market.model.bo.SfMarketCorrectionBo;
import com.ym.agriculture.farming.market.model.bo.SfMarketHandleBo;
import com.ym.agriculture.farming.market.model.bo.SfMarketIngestQueryBo;
import com.ym.agriculture.farming.market.model.bo.SfMarketQuoteQueryBo;
import com.ym.agriculture.farming.market.model.vo.SfMarketCategoryVo;
import com.ym.agriculture.farming.market.model.vo.SfMarketIngestDetailVo;
import com.ym.agriculture.farming.market.model.vo.SfMarketIngestListVo;
import com.ym.agriculture.farming.market.model.vo.SfMarketIngestProcessVo;
import com.ym.agriculture.farming.market.model.vo.SfMarketQuoteVo;

import java.util.List;

/** 农业行情采集、运营及移动查询服务。 */
public interface ISfMarketService {
    /** 查询管理端正式行情。 */
    PageResult<SfMarketQuoteVo> queryAdminQuotePage(SfMarketQuoteQueryBo bo, PageQuery pageQuery);
    /** 查询管理端采集记录。 */
    PageResult<SfMarketIngestListVo> queryIngestPage(SfMarketIngestQueryBo bo, PageQuery pageQuery);
    /** 查询采集记录详情。 */
    SfMarketIngestDetailVo getIngestDetail(Long id);
    /** 处理待处理采集记录。 */
    SfMarketIngestProcessVo processPending(int limit);
    /** 保存修正并重新处理。 */
    void correctAndReprocess(Long id, SfMarketCorrectionBo bo);
    /** 重试平台处理失败记录。 */
    void retry(Long id);
    /** 忽略无法使用的采集记录。 */
    void ignore(Long id, SfMarketHandleBo bo);
    /** 确认已发布报价的告警。 */
    void acknowledgeWarning(Long id, SfMarketHandleBo bo);
    /** 查询移动端最新或指定日期行情。 */
    PageResult<SfMarketQuoteVo> queryMobilePage(SfMarketQuoteQueryBo bo, PageQuery pageQuery);
    /** 查询固定商品品类。 */
    List<SfMarketCategoryVo> categories();
}
