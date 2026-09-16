package com.ym.agriculture.farming.market.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ym.common.core.domain.R;
import com.ym.common.redis.annotation.RepeatSubmit;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farming.market.model.bo.SfMarketCorrectionBo;
import com.ym.agriculture.farming.market.model.bo.SfMarketHandleBo;
import com.ym.agriculture.farming.market.model.bo.SfMarketIngestProcessBo;
import com.ym.agriculture.farming.market.model.bo.SfMarketIngestQueryBo;
import com.ym.agriculture.farming.market.model.bo.SfMarketQuoteQueryBo;
import com.ym.agriculture.farming.market.model.vo.SfMarketIngestDetailVo;
import com.ym.agriculture.farming.market.model.vo.SfMarketIngestListVo;
import com.ym.agriculture.farming.market.model.vo.SfMarketIngestProcessVo;
import com.ym.agriculture.farming.market.model.vo.SfMarketQuoteVo;
import com.ym.agriculture.farming.market.service.ISfMarketService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理端农业行情查询、采集处理和异常处置接口。 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/smart-farming/market")
public class SfMarketController {

    private final ISfMarketService marketService;

    /** 查询已发布农业行情。 */
    @SaCheckPermission("smartfarming:market:list")
    @GetMapping("/quotes/page")
    public R<PageResult<SfMarketQuoteVo>> quotePage(SfMarketQuoteQueryBo bo, PageQuery pageQuery) {
        return R.ok(marketService.queryAdminQuotePage(bo, pageQuery));
    }

    /** 查询采集暂存及异常记录。 */
    @SaCheckPermission("smartfarming:market:list")
    @GetMapping("/ingest/page")
    public R<PageResult<SfMarketIngestListVo>> ingestPage(SfMarketIngestQueryBo bo, PageQuery pageQuery) {
        return R.ok(marketService.queryIngestPage(bo, pageQuery));
    }

    /** 查询一条采集记录的原始数据、修正数据和处理结果。 */
    @SaCheckPermission("smartfarming:market:query")
    @GetMapping("/ingest/{id}")
    public R<SfMarketIngestDetailVo> ingestDetail(@NotNull @PathVariable Long id) {
        return R.ok(marketService.getIngestDetail(id));
    }

    /** 手动处理一批行情采集队列。 */
    @SaCheckPermission("smartfarming:market:process")
    @Log(title = "农业行情采集处理", businessType = BusinessType.OTHER)
    @RepeatSubmit()
    @PostMapping("/ingest/process")
    public R<SfMarketIngestProcessVo> process(@Valid @RequestBody SfMarketIngestProcessBo bo) {
        return R.ok(marketService.processPending(bo.getLimit()));
    }

    /** 保存运营修正并立即重新处理记录。 */
    @SaCheckPermission("smartfarming:market:handle")
    @Log(title = "农业行情修正重处理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PostMapping("/ingest/{id}/correct-and-reprocess")
    public R<Void> correctAndReprocess(@NotNull @PathVariable Long id,
                                       @Valid @RequestBody SfMarketCorrectionBo bo) {
        marketService.correctAndReprocess(id, bo);
        return R.ok();
    }

    /** 对平台内部处理失败记录直接重试。 */
    @SaCheckPermission("smartfarming:market:handle")
    @Log(title = "农业行情采集重试", businessType = BusinessType.OTHER)
    @RepeatSubmit()
    @PostMapping("/ingest/{id}/retry")
    public R<Void> retry(@NotNull @PathVariable Long id) {
        marketService.retry(id);
        return R.ok();
    }

    /** 忽略无法使用的异常采集记录。 */
    @SaCheckPermission("smartfarming:market:handle")
    @Log(title = "农业行情异常忽略", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PostMapping("/ingest/{id}/ignore")
    public R<Void> ignore(@NotNull @PathVariable Long id, @Valid @RequestBody SfMarketHandleBo bo) {
        marketService.ignore(id, bo);
        return R.ok();
    }

    /** 确认已自动发布报价的价格跳变告警。 */
    @SaCheckPermission("smartfarming:market:handle")
    @Log(title = "农业行情告警确认", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PostMapping("/ingest/{id}/acknowledge-warning")
    public R<Void> acknowledgeWarning(@NotNull @PathVariable Long id,
                                      @Valid @RequestBody SfMarketHandleBo bo) {
        marketService.acknowledgeWarning(id, bo);
        return R.ok();
    }
}
