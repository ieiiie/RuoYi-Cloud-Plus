package com.ym.agriculture.farming.trace.controller;

import com.ym.common.core.domain.R;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.validate.AddGroup;
import com.ym.common.core.validate.EditGroup;
import com.ym.common.redis.annotation.RepeatSubmit;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.web.core.BaseController;
import com.ym.agriculture.farming.trace.model.bo.SfTraceBatchBo;
import com.ym.agriculture.farming.trace.model.bo.SfTraceCodeBo;
import com.ym.agriculture.farming.trace.model.bo.SfTraceCodeGenerateBo;
import com.ym.agriculture.farming.trace.model.bo.SfTraceCodeVoidBatchBo;
import com.ym.agriculture.farming.trace.model.vo.SfTraceBatchStatsVo;
import com.ym.agriculture.farming.trace.model.vo.SfTraceBatchVo;
import com.ym.agriculture.farming.trace.model.vo.SfTraceCodeVo;
import com.ym.agriculture.farming.trace.service.ISfTraceBatchService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 瓜果溯源管理端接口，前缀 {@code /smart-farming/trace}。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/smart-farming/trace")
public class SfTraceBatchController extends BaseController {

    private final ISfTraceBatchService traceBatchService;

    @GetMapping("/batches/page")
    public R<PageResult<SfTraceBatchVo>> batchPage(SfTraceBatchBo bo, PageQuery pageQuery) {
        return R.ok(traceBatchService.queryPageList(bo, pageQuery));
    }

    @GetMapping("/batches/{traceBatchId}")
    public R<SfTraceBatchVo> getBatch(@PathVariable Long traceBatchId) {
        return R.ok(traceBatchService.queryById(traceBatchId));
    }

    @Log(title = "溯源批次", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/batches")
    public R<Void> addBatch(@Validated(AddGroup.class) @RequestBody SfTraceBatchBo bo) {
        return toAjax(traceBatchService.insertByBo(bo));
    }

    @Log(title = "溯源批次", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping("/batches")
    public R<Void> editBatch(@Validated(EditGroup.class) @RequestBody SfTraceBatchBo bo) {
        return toAjax(traceBatchService.updateByBo(bo));
    }

    @Log(title = "溯源批次", businessType = BusinessType.DELETE)
    @DeleteMapping("/batches/{traceBatchIds}")
    public R<Void> removeBatch(@PathVariable Long[] traceBatchIds) {
        return toAjax(traceBatchService.deleteWithValidByIds(List.of(traceBatchIds)));
    }

    @Log(title = "溯源批次", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PostMapping("/batches/{traceBatchId}/publish")
    public R<Void> publish(@PathVariable Long traceBatchId) {
        return toAjax(traceBatchService.publish(traceBatchId));
    }

    @Log(title = "溯源批次", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PostMapping("/batches/{traceBatchId}/disable")
    public R<Void> disable(@PathVariable Long traceBatchId) {
        return toAjax(traceBatchService.disable(traceBatchId));
    }

    @Log(title = "溯源码", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/batches/{traceBatchId}/codes/generate")
    public R<Integer> generateCodes(@PathVariable Long traceBatchId,
                                    @Validated @RequestBody SfTraceCodeGenerateBo bo) {
        return R.ok(traceBatchService.generateCodes(traceBatchId, bo));
    }

    @GetMapping("/codes/page")
    public R<PageResult<SfTraceCodeVo>> codePage(SfTraceCodeBo bo, PageQuery pageQuery) {
        return R.ok(traceBatchService.queryCodePageList(bo, pageQuery));
    }

    @Log(title = "溯源码", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PostMapping("/codes/{traceCodeId}/void")
    public R<Void> voidCode(@PathVariable Long traceCodeId) {
        return toAjax(traceBatchService.voidCode(traceCodeId));
    }

    @Log(title = "溯源码", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PostMapping("/codes/void-batch")
    public R<Void> voidCodeBatch(@Validated @RequestBody SfTraceCodeVoidBatchBo bo) {
        return toAjax(traceBatchService.voidCodeBatch(bo));
    }

    @Log(title = "溯源码", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PostMapping("/codes/{traceCodeId}/reprint")
    public R<Void> reprint(@PathVariable Long traceCodeId) {
        return toAjax(traceBatchService.reprint(traceCodeId));
    }

    @GetMapping("/batches/{traceBatchId}/stats")
    public R<SfTraceBatchStatsVo> stats(@PathVariable Long traceBatchId) {
        return R.ok(traceBatchService.stats(traceBatchId));
    }

    @GetMapping("/batches/{traceBatchId}/labels.pdf")
    public void batchLabelsPdf(@PathVariable Long traceBatchId, HttpServletResponse response) throws IOException {
        writePdf(response, "trace-labels-" + traceBatchId + ".pdf", traceBatchService.exportBatchLabelsPdf(traceBatchId));
    }

    @GetMapping("/codes/{traceCodeId}/label.pdf")
    public void singleLabelPdf(@PathVariable Long traceCodeId, HttpServletResponse response) throws IOException {
        writePdf(response, "trace-label-" + traceCodeId + ".pdf", traceBatchService.exportSingleLabelPdf(traceCodeId));
    }

    private static void writePdf(HttpServletResponse response, String filename, byte[] pdf) throws IOException {
        response.setContentType(MediaType.APPLICATION_PDF_VALUE);
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded);
        response.getOutputStream().write(pdf);
        response.getOutputStream().flush();
    }
}
