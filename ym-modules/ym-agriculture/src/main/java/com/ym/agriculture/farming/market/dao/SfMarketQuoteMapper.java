package com.ym.agriculture.farming.market.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farming.market.model.bo.SfMarketQuoteQueryBo;
import com.ym.agriculture.farming.market.model.entity.SfMarketQuote;
import com.ym.agriculture.farming.market.model.vo.SfMarketQuoteVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

/** 农业行情正式报价 Mapper。 */
@InterceptorIgnore(tenantLine = "true")
public interface SfMarketQuoteMapper extends BaseMapperPlus<SfMarketQuote, SfMarketQuote> {

    /** 查询同序列同日期正式报价。 */
    default SfMarketQuote selectBySeriesDate(String seriesKey, LocalDate quoteDate) {
        return selectOne(Wrappers.<SfMarketQuote>lambdaQuery()
            .eq(SfMarketQuote::getSeriesKeySha256, seriesKey)
            .eq(SfMarketQuote::getQuoteDate, quoteDate)
            .last("limit 1"));
    }

    /** 查询指定日期之前最近一条有效报价。 */
    default SfMarketQuote selectPrevious(String seriesKey, LocalDate quoteDate) {
        return selectOne(Wrappers.<SfMarketQuote>lambdaQuery()
            .eq(SfMarketQuote::getSeriesKeySha256, seriesKey)
            .eq(SfMarketQuote::getPublishStatus, "PUBLISHED")
            .lt(SfMarketQuote::getQuoteDate, quoteDate)
            .orderByDesc(SfMarketQuote::getQuoteDate)
            .last("limit 1"));
    }

    /** 查询指定日期之后紧邻的一条有效报价。 */
    default SfMarketQuote selectNext(String seriesKey, LocalDate quoteDate) {
        return selectOne(Wrappers.<SfMarketQuote>lambdaQuery()
            .eq(SfMarketQuote::getSeriesKeySha256, seriesKey)
            .eq(SfMarketQuote::getPublishStatus, "PUBLISHED")
            .gt(SfMarketQuote::getQuoteDate, quoteDate)
            .orderByAsc(SfMarketQuote::getQuoteDate)
            .last("limit 1"));
    }

    /** 管理端查询全部正式报价。 */
    @Select({"<script>",
        "SELECT q.quote_id, q.product_id, p.product_name, q.specification, p.category, q.price, q.unit,",
        "q.previous_price, q.change_amount, q.change_percent, q.trend, q.quote_type, q.origin, q.market_name,",
        "q.quote_date, q.collected_at, q.source_code, s.source_name, q.source_url, q.warnings_json,",
        "COALESCE(i.warning_acknowledged,0) AS warning_acknowledged",
        "FROM sf_market_quote q JOIN sf_market_product p ON p.product_id = q.product_id",
        "JOIN sf_market_source s ON s.source_code = q.source_code",
        "LEFT JOIN sf_market_quote_ingest_staging i ON i.id = q.latest_ingest_id",
        "WHERE q.publish_status = 'PUBLISHED' AND p.enabled_flag = 1",
        "<if test='bo.category != null and bo.category != \"\"'> AND p.category = #{bo.category}</if>",
        "<if test='bo.sourceCode != null and bo.sourceCode != \"\"'> AND q.source_code = #{bo.sourceCode}</if>",
        "<if test='bo.quoteType != null and bo.quoteType != \"\"'> AND q.quote_type = #{bo.quoteType}</if>",
        "<if test='bo.marketName != null and bo.marketName != \"\"'> AND q.market_name LIKE CONCAT('%',#{bo.marketName},'%')</if>",
        "<if test='bo.quoteDate != null'> AND q.quote_date = #{bo.quoteDate}</if>",
        "<if test='bo.keyword != null and bo.keyword != \"\"'> AND (p.product_name LIKE CONCAT('%',#{bo.keyword},'%')",
        "OR q.specification LIKE CONCAT('%',#{bo.keyword},'%') OR q.origin LIKE CONCAT('%',#{bo.keyword},'%')",
        "OR q.market_name LIKE CONCAT('%',#{bo.keyword},'%'))</if>",
        "ORDER BY q.quote_date DESC, p.category, p.sort_order, p.product_name, q.market_name, q.quote_id",
        "</script>"})
    Page<SfMarketQuoteVo> selectAdminPage(Page<SfMarketQuoteVo> page, @Param("bo") SfMarketQuoteQueryBo bo);

    /** 移动端查询指定日期或每个价格序列的最新有效报价。 */
    @Select({"<script>",
        "SELECT q.quote_id, q.product_id, p.product_name, q.specification, p.category, q.price, q.unit,",
        "q.previous_price, q.change_amount, q.change_percent, q.trend, q.quote_type, q.origin, q.market_name,",
        "q.quote_date, q.collected_at, q.source_code, s.source_name, q.source_url, q.warnings_json",
        "FROM sf_market_quote q JOIN sf_market_product p ON p.product_id = q.product_id",
        "JOIN sf_market_source s ON s.source_code = q.source_code",
        "WHERE q.publish_status = 'PUBLISHED' AND p.enabled_flag = 1",
        "<choose><when test='bo.quoteDate != null'>AND q.quote_date = #{bo.quoteDate}</when>",
        "<otherwise>AND NOT EXISTS (SELECT 1 FROM sf_market_quote newer",
        "WHERE newer.series_key_sha256 = q.series_key_sha256 AND newer.publish_status = 'PUBLISHED'",
        "AND (newer.quote_date &gt; q.quote_date OR (newer.quote_date = q.quote_date AND newer.quote_id &gt; q.quote_id)))</otherwise></choose>",
        "<if test='bo.category != null and bo.category != \"\"'> AND p.category = #{bo.category}</if>",
        "<if test='bo.keyword != null and bo.keyword != \"\"'> AND (p.product_name LIKE CONCAT('%',#{bo.keyword},'%')",
        "OR q.specification LIKE CONCAT('%',#{bo.keyword},'%') OR q.origin LIKE CONCAT('%',#{bo.keyword},'%'))</if>",
        "ORDER BY FIELD(p.category,'grain','vegetable','fruit','livestock','aquatic'),",
        "p.sort_order, p.product_name, q.market_name, q.source_code, q.quote_id",
        "</script>"})
    Page<SfMarketQuoteVo> selectMobilePage(Page<SfMarketQuoteVo> page, @Param("bo") SfMarketQuoteQueryBo bo);
}
