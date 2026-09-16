package com.ym.agriculture.farming.market.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farming.market.model.bo.SfMarketIngestQueryBo;
import com.ym.agriculture.farming.market.model.entity.SfMarketQuoteIngestStaging;
import com.ym.agriculture.farming.market.model.vo.SfMarketIngestListVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/** 农业行情采集暂存 Mapper。 */
@InterceptorIgnore(tenantLine = "true")
public interface SfMarketQuoteIngestMapper
    extends BaseMapperPlus<SfMarketQuoteIngestStaging, SfMarketQuoteIngestStaging> {

    /** 管理端分页查询采集记录。 */
    @Select({
        "<script>",
        "SELECT i.id, i.request_id, i.crawl_run_id, i.source_code, s.source_name, i.product_name,",
        "i.specification, i.price, i.unit, i.quote_date, i.ingest_status, i.result_action,",
        "i.error_code, i.error_message, i.warnings_json, i.warning_acknowledged, i.received_at, i.processed_at",
        "FROM sf_market_quote_ingest_staging i LEFT JOIN sf_market_source s ON s.source_code = i.source_code",
        "WHERE 1=1",
        "<if test='bo.sourceCode != null and bo.sourceCode != \"\"'> AND i.source_code = #{bo.sourceCode}</if>",
        "<if test='bo.ingestStatus == null or bo.ingestStatus == \"\"'> AND i.ingest_status IN ('REJECTED','FAILED')</if>",
        "<if test='bo.ingestStatus != null and bo.ingestStatus != \"\" and bo.ingestStatus != \"ALL\"'>",
        "AND i.ingest_status = #{bo.ingestStatus}</if>",
        "<if test='bo.crawlRunId != null and bo.crawlRunId != \"\"'> AND i.crawl_run_id = #{bo.crawlRunId}</if>",
        "<if test='bo.requestId != null and bo.requestId != \"\"'> AND i.request_id = #{bo.requestId}</if>",
        "<if test='bo.keyword != null and bo.keyword != \"\"'> AND (i.product_name LIKE CONCAT('%',#{bo.keyword},'%')",
        "OR i.specification LIKE CONCAT('%',#{bo.keyword},'%'))</if>",
        "<if test='bo.quoteDate != null'> AND i.quote_date = #{bo.quoteDate}</if>",
        "ORDER BY i.received_at DESC, i.id DESC",
        "</script>"
    })
    Page<SfMarketIngestListVo> selectAdminPage(Page<SfMarketIngestListVo> page,
                                                @Param("bo") SfMarketIngestQueryBo bo);

    /** 查询可领取记录，包含超时的处理中记录。 */
    default List<SfMarketQuoteIngestStaging> selectPending(int limit) {
        Date staleBefore = new Date(System.currentTimeMillis() - 10 * 60 * 1000L);
        return selectList(Wrappers.<SfMarketQuoteIngestStaging>lambdaQuery()
            .and(w -> w.eq(SfMarketQuoteIngestStaging::getIngestStatus, "PENDING")
                .or(stale -> stale.eq(SfMarketQuoteIngestStaging::getIngestStatus, "PROCESSING")
                    .lt(SfMarketQuoteIngestStaging::getProcessingStartedAt, staleBefore)))
            .orderByAsc(SfMarketQuoteIngestStaging::getReceivedAt)
            .last("limit " + Math.max(1, Math.min(limit, 200))));
    }

    /** 条件领取一条记录，防止多实例重复处理。 */
    default boolean claim(Long id, Date now) {
        Date staleBefore = new Date(now.getTime() - 10 * 60 * 1000L);
        return update(null, Wrappers.<SfMarketQuoteIngestStaging>lambdaUpdate()
            .eq(SfMarketQuoteIngestStaging::getId, id)
            .and(w -> w.eq(SfMarketQuoteIngestStaging::getIngestStatus, "PENDING")
                .or(stale -> stale.eq(SfMarketQuoteIngestStaging::getIngestStatus, "PROCESSING")
                    .lt(SfMarketQuoteIngestStaging::getProcessingStartedAt, staleBefore)))
            .set(SfMarketQuoteIngestStaging::getIngestStatus, "PROCESSING")
            .set(SfMarketQuoteIngestStaging::getProcessingStartedAt, now)
            .setSql("process_attempts = process_attempts + 1")) > 0;
    }
}
