package com.ym.agriculture.farming.trace.service;

import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farming.trace.model.bo.SfTraceBatchBo;
import com.ym.agriculture.farming.trace.model.bo.SfTraceCodeBo;
import com.ym.agriculture.farming.trace.model.bo.SfTraceCodeGenerateBo;
import com.ym.agriculture.farming.trace.model.bo.SfTraceCodeVoidBatchBo;
import com.ym.agriculture.farming.trace.model.vo.SfTraceBatchStatsVo;
import com.ym.agriculture.farming.trace.model.vo.SfTraceBatchVo;
import com.ym.agriculture.farming.trace.model.vo.SfTraceCodeVo;

import java.util.Collection;
import java.util.List;

/**
 * 溯源批次与溯源码管理服务。
 */
public interface ISfTraceBatchService {

    SfTraceBatchVo queryById(Long traceBatchId);

    PageResult<SfTraceBatchVo> queryPageList(SfTraceBatchBo bo, PageQuery pageQuery);

    Boolean insertByBo(SfTraceBatchBo bo);

    Boolean updateByBo(SfTraceBatchBo bo);

    Boolean deleteWithValidByIds(Collection<Long> traceBatchIds);

    Boolean publish(Long traceBatchId);

    Boolean disable(Long traceBatchId);

    int generateCodes(Long traceBatchId, SfTraceCodeGenerateBo bo);

    PageResult<SfTraceCodeVo> queryCodePageList(SfTraceCodeBo bo, PageQuery pageQuery);

    Boolean voidCode(Long traceCodeId);

    Boolean voidCodeBatch(SfTraceCodeVoidBatchBo bo);

    Boolean reprint(Long traceCodeId);

    SfTraceBatchStatsVo stats(Long traceBatchId);

    byte[] exportBatchLabelsPdf(Long traceBatchId);

    byte[] exportSingleLabelPdf(Long traceCodeId);
}
