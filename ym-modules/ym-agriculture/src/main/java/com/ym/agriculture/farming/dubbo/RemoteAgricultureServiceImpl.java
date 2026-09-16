package com.ym.agriculture.farming.dubbo;

import com.ym.agriculture.api.farming.RemoteAgricultureService;
import com.ym.agriculture.api.farming.domain.bo.RemoteMarketQuoteQueryBo;
import com.ym.agriculture.api.farming.domain.vo.RemoteCalendarDateInfoVo;
import com.ym.agriculture.api.farming.domain.vo.RemoteCropVarietySimpleVo;
import com.ym.agriculture.api.farming.domain.vo.RemoteFieldSummaryVo;
import com.ym.agriculture.api.farming.domain.vo.RemoteFarmWorkTreeVo;
import com.ym.agriculture.api.farming.domain.vo.RemoteFarmWorkVo;
import com.ym.agriculture.api.farming.domain.vo.RemotePlantingBatchSummaryVo;
import com.ym.agriculture.api.farming.domain.vo.RemoteSolarTermDetailVo;
import com.ym.agriculture.api.farming.domain.vo.RemoteSolarTermSummaryVo;
import com.ym.agriculture.api.farming.domain.vo.RemoteMarketCategoryVo;
import com.ym.agriculture.api.farming.domain.vo.RemoteMarketQuoteVo;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchVo;
import com.ym.agriculture.farming.batch.service.ISfPlantingBatchService;
import com.ym.agriculture.farming.crop.model.bo.SfCropVarietyBo;
import com.ym.agriculture.farming.crop.model.vo.SfCropVarietyVo;
import com.ym.agriculture.farming.crop.service.ISfCropVarietyService;
import com.ym.agriculture.farming.field.dao.SfFieldMapper;
import com.ym.agriculture.farming.field.model.vo.SfFieldVo;
import com.ym.agriculture.farming.farmwork.model.vo.SfFarmWorkDictTreeVo;
import com.ym.agriculture.farming.farmwork.model.vo.SfFarmWorkDictVo;
import com.ym.agriculture.farming.farmwork.service.ISfFarmWorkDictService;
import com.ym.agriculture.farming.market.model.bo.SfMarketQuoteQueryBo;
import com.ym.agriculture.farming.market.model.vo.SfMarketCategoryVo;
import com.ym.agriculture.farming.market.model.vo.SfMarketQuoteVo;
import com.ym.agriculture.farming.market.service.ISfMarketService;
import com.ym.agriculture.farming.solarterms.model.vo.CalendarDateInfoVo;
import com.ym.agriculture.farming.solarterms.model.vo.SfSolarTermDetailVo;
import com.ym.agriculture.farming.solarterms.model.vo.SfSolarTermSummaryVo;
import com.ym.agriculture.farming.solarterms.service.ISfSolarTermService;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 农业业务跨服务查询实现。
 *
 * @author ym-cloud
 */
@Service
@DubboService
@RequiredArgsConstructor
public class RemoteAgricultureServiceImpl implements RemoteAgricultureService {

    private final SfFieldMapper fieldMapper;
    private final ISfCropVarietyService cropVarietyService;
    private final ISfPlantingBatchService plantingBatchService;
    private final ISfFarmWorkDictService farmWorkDictService;
    private final ISfSolarTermService solarTermService;
    private final ISfMarketService marketService;

    @Override
    public RemoteFieldSummaryVo getField(Long fieldId) {
        SfFieldVo field = fieldMapper.selectVoById(fieldId);
        return toSummary(field);
    }

    @Override
    public List<RemoteFieldSummaryVo> listFields() {
        return fieldMapper.selectEnabledByIds(null).stream()
            .map(RemoteAgricultureServiceImpl::toSummary)
            .toList();
    }

    @Override
    public List<RemoteCropVarietySimpleVo> listEnabledCropVarieties() {
        SfCropVarietyBo query = new SfCropVarietyBo();
        query.setStatus(SystemConstants.NORMAL);
        return cropVarietyService.queryList(query).stream()
            .map(RemoteAgricultureServiceImpl::toCropVariety)
            .toList();
    }

    @Override
    public List<RemotePlantingBatchSummaryVo> listActivePlantingBatches(List<Long> fieldIds) {
        return plantingBatchService.listActiveByFieldIds(fieldIds).stream()
            .map(RemoteAgricultureServiceImpl::toPlantingBatch)
            .toList();
    }

    @Override
    public List<RemoteFarmWorkTreeVo> listEnabledFarmWorkTree() {
        return farmWorkDictService.miniappTree().stream()
            .map(RemoteAgricultureServiceImpl::toFarmWorkTree)
            .toList();
    }

    @Override
    public List<RemoteFarmWorkVo> listFarmWorkCategories() {
        return farmWorkDictService.mobileCategories().stream()
            .map(RemoteAgricultureServiceImpl::toFarmWork)
            .toList();
    }

    @Override
    public List<RemoteFarmWorkVo> listEnabledFarmWorkItems(Long categoryId) {
        return farmWorkDictService.mobileItems(categoryId).stream()
            .map(RemoteAgricultureServiceImpl::toFarmWork)
            .toList();
    }

    @Override
    public RemoteSolarTermDetailVo getCurrentSolarTerm() {
        return toSolarTerm(solarTermService.getCurrent());
    }

    @Override
    public RemoteSolarTermDetailVo getSolarTerm(String termCode, Integer year) {
        return toSolarTerm(solarTermService.getByTermCode(termCode, year));
    }

    @Override
    public RemoteCalendarDateInfoVo getCalendarDateInfo(Date happenedAt) {
        return toCalendarDateInfo(solarTermService.getCalendarDateInfo(happenedAt));
    }

    @Override
    public PageResult<RemoteMarketQuoteVo> pageMarketQuotes(RemoteMarketQuoteQueryBo query) {
        SfMarketQuoteQueryBo bo = new SfMarketQuoteQueryBo();
        bo.setKeyword(query.getKeyword());
        bo.setCategory(query.getCategory());
        bo.setQuoteDate(query.getQuoteDate());
        PageQuery pageQuery = new PageQuery(query.getPageSize(), query.getPageNum());
        PageResult<SfMarketQuoteVo> page = marketService.queryMobilePage(bo, pageQuery);
        return PageResult.build(page.getRows().stream()
            .map(RemoteAgricultureServiceImpl::toMarketQuote)
            .toList(), page.getTotal());
    }

    @Override
    public List<RemoteMarketCategoryVo> listMarketCategories() {
        return marketService.categories().stream()
            .map(RemoteAgricultureServiceImpl::toMarketCategory)
            .toList();
    }

    private static RemoteFieldSummaryVo toSummary(SfFieldVo field) {
        if (field == null) {
            return null;
        }
        RemoteFieldSummaryVo summary = new RemoteFieldSummaryVo();
        summary.setFieldId(field.getFieldId());
        summary.setFieldName(field.getFieldName());
        summary.setFieldCode(field.getFieldCode());
        summary.setStatus(field.getStatus());
        return summary;
    }

    private static RemoteCropVarietySimpleVo toCropVariety(SfCropVarietyVo variety) {
        RemoteCropVarietySimpleVo option = new RemoteCropVarietySimpleVo();
        option.setSpeciesId(variety.getSpeciesId());
        option.setSpeciesName(variety.getSpeciesName());
        option.setVarietyId(variety.getVarietyId());
        option.setVarietyName(variety.getVarietyName());
        return option;
    }

    private static RemotePlantingBatchSummaryVo toPlantingBatch(SfPlantingBatchVo source) {
        RemotePlantingBatchSummaryVo target = new RemotePlantingBatchSummaryVo();
        target.setBatchId(source.getBatchId());
        target.setFieldId(source.getFieldId());
        target.setFieldName(source.getFieldName());
        target.setVarietyId(source.getVarietyId());
        target.setVarietyName(source.getVarietyName());
        target.setSpeciesId(source.getSpeciesId());
        target.setSpeciesName(source.getSpeciesName());
        target.setBatchCode(source.getBatchCode());
        target.setCroppingIndex(source.getCroppingIndex());
        target.setSowingDate(source.getSowingDate());
        target.setExpectedHarvestDate(source.getExpectedHarvestDate());
        target.setActualHarvestDate(source.getActualHarvestDate());
        target.setBatchStatus(source.getBatchStatus());
        return target;
    }

    private static RemoteFarmWorkTreeVo toFarmWorkTree(SfFarmWorkDictTreeVo source) {
        RemoteFarmWorkTreeVo target = new RemoteFarmWorkTreeVo();
        fillFarmWork(target, source);
        target.setChildren(source.getChildren().stream()
            .map(RemoteAgricultureServiceImpl::toFarmWorkTree)
            .toList());
        return target;
    }

    private static RemoteFarmWorkVo toFarmWork(SfFarmWorkDictVo source) {
        RemoteFarmWorkVo target = new RemoteFarmWorkVo();
        fillFarmWork(target, source);
        return target;
    }

    @SuppressWarnings("unchecked")
    private static void fillFarmWork(RemoteFarmWorkVo target, SfFarmWorkDictVo source) {
        target.setDictId(source.getDictId());
        target.setParentId(source.getParentId());
        target.setNodeType(source.getNodeType());
        target.setDictName(source.getDictName());
        target.setDictCode(source.getDictCode());
        target.setMinWorkers(source.getMinWorkers());
        target.setMaxWorkers(source.getMaxWorkers());
        target.setRequiresMaterial(source.getRequiresMaterial());
        target.setStatus(source.getStatus());
        if (source.getCustomFormTemplate() instanceof Map<?, ?> template) {
            target.setCustomFormTemplate((Map<String, Object>) template);
        }
        target.setSortOrder(source.getSortOrder());
        target.setRemark(source.getRemark());
    }

    private static RemoteSolarTermDetailVo toSolarTerm(SfSolarTermDetailVo source) {
        if (source == null) {
            return null;
        }
        RemoteSolarTermDetailVo target = new RemoteSolarTermDetailVo();
        target.setTermCode(source.getTermCode());
        target.setTermName(source.getTermName());
        target.setTermOrder(source.getTermOrder());
        target.setTermYear(source.getTermYear());
        target.setOccurredAt(source.getOccurredAt());
        target.setGregorianDate(source.getGregorianDate());
        target.setLunarDateText(source.getLunarDateText());
        target.setIntro(source.getIntro());
        target.setSeasonalDescription(source.getSeasonalDescription());
        target.setCustoms(source.getCustoms());
        target.setNextTerm(toSolarTermSummary(source.getNextTerm()));
        return target;
    }

    private static RemoteSolarTermSummaryVo toSolarTermSummary(SfSolarTermSummaryVo source) {
        if (source == null) {
            return null;
        }
        RemoteSolarTermSummaryVo target = new RemoteSolarTermSummaryVo();
        target.setTermCode(source.getTermCode());
        target.setTermName(source.getTermName());
        target.setOccurredAt(source.getOccurredAt());
        target.setGregorianDate(source.getGregorianDate());
        return target;
    }

    private static RemoteCalendarDateInfoVo toCalendarDateInfo(CalendarDateInfoVo source) {
        RemoteCalendarDateInfoVo target = new RemoteCalendarDateInfoVo();
        target.setGregorianDateTime(source.getGregorianDateTime());
        target.setLunarDateText(source.getLunarDateText());
        target.setSolarTermCode(source.getSolarTermCode());
        target.setSolarTermName(source.getSolarTermName());
        target.setSolarTermOccurredAt(source.getSolarTermOccurredAt());
        target.setTimeZone(source.getTimeZone());
        target.setAlgorithmVersion(source.getAlgorithmVersion());
        return target;
    }

    private static RemoteMarketCategoryVo toMarketCategory(SfMarketCategoryVo source) {
        RemoteMarketCategoryVo target = new RemoteMarketCategoryVo();
        target.setCategory(source.getCategory());
        target.setCategoryName(source.getCategoryName());
        target.setSort(source.getSort());
        return target;
    }

    private static RemoteMarketQuoteVo toMarketQuote(SfMarketQuoteVo source) {
        RemoteMarketQuoteVo target = new RemoteMarketQuoteVo();
        target.setQuoteId(source.getQuoteId());
        target.setProductId(source.getProductId());
        target.setProductName(source.getProductName());
        target.setSpecification(source.getSpecification());
        target.setDisplayName(source.getDisplayName());
        target.setCategory(source.getCategory());
        target.setCategoryName(source.getCategoryName());
        target.setPrice(source.getPrice());
        target.setUnit(source.getUnit());
        target.setPreviousPrice(source.getPreviousPrice());
        target.setChangeAmount(source.getChangeAmount());
        target.setChangePercent(source.getChangePercent());
        target.setTrend(source.getTrend());
        target.setQuoteType(source.getQuoteType());
        target.setOrigin(source.getOrigin());
        target.setMarketName(source.getMarketName());
        target.setQuoteDate(source.getQuoteDate());
        target.setCollectedAt(source.getCollectedAt());
        target.setSourceCode(source.getSourceCode());
        target.setSourceName(source.getSourceName());
        target.setSourceUrl(source.getSourceUrl());
        target.setWarningsJson(source.getWarningsJson());
        target.setWarningAcknowledged(source.getWarningAcknowledged());
        return target;
    }
}
