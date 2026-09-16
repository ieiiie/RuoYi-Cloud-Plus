package com.ym.agriculture.farming.batch.service;

import com.ym.agriculture.farming.batch.model.bo.SfPlantingBatchBo;
import com.ym.agriculture.farming.batch.model.bo.SfDetectorInfoBo;
import com.ym.agriculture.farming.batch.model.bo.SfPlantingBatchTransitionBo;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchCalendarDayVo;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchDetailVo;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchExportVo;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchDashboardStatsVo;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchVo;
import com.ym.agriculture.farming.batch.model.vo.SfDetectorInfoVo;
import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 种植批次核心服务。
 */
public interface ISfPlantingBatchService {

    List<SfPlantingBatchVo> queryList(SfPlantingBatchBo bo);

    PageResult<SfPlantingBatchVo> queryPageList(SfPlantingBatchBo bo, PageQuery pageQuery);

    PageResult<SfDetectorInfoVo> queryDetectorInfoPage(SfDetectorInfoBo bo, PageQuery pageQuery);

    SfPlantingBatchVo queryById(Long batchId);

    SfPlantingBatchDetailVo queryDetailById(Long batchId);

    Boolean insertByBo(SfPlantingBatchBo bo);

    Boolean updateByBo(SfPlantingBatchBo bo);

    Boolean transition(Long batchId, SfPlantingBatchTransitionBo bo);

    Boolean deleteWithValidByIds(Collection<Long> batchIds);

    List<SfPlantingBatchCalendarDayVo> queryCalendar(int year, int month);

    int suggestCroppingIndex(Long fieldId, LocalDate sowingDate);

    LocalDate suggestHarvestDate(Long varietyId, LocalDate sowingDate);

    List<SfPlantingBatchExportVo> queryExportList(SfPlantingBatchBo bo);

    SfPlantingBatchDashboardStatsVo statsForDashboard();

    List<SfPlantingBatchVo> listActiveByFieldIds(Collection<Long> fieldIds);

    Map<Long, List<SfPlantingBatchVo>> mapActiveVoByFieldIds(Collection<Long> fieldIds);
}
