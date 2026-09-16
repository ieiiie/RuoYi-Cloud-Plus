package com.ym.agriculture.farming.market.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.agriculture.farming.market.dao.SfMarketProductMapper;
import com.ym.agriculture.farming.market.dao.SfMarketProductMappingMapper;
import com.ym.agriculture.farming.market.dao.SfMarketQuoteIngestMapper;
import com.ym.agriculture.farming.market.dao.SfMarketQuoteMapper;
import com.ym.agriculture.farming.market.dao.SfMarketSourceMapper;
import com.ym.agriculture.farming.market.model.bo.SfMarketCorrectionBo;
import com.ym.agriculture.farming.market.model.bo.SfMarketHandleBo;
import com.ym.agriculture.farming.market.model.bo.SfMarketIngestQueryBo;
import com.ym.agriculture.farming.market.model.bo.SfMarketQuoteQueryBo;
import com.ym.agriculture.farming.market.model.constants.SfMarketConstants;
import com.ym.agriculture.farming.market.model.entity.SfMarketProduct;
import com.ym.agriculture.farming.market.model.entity.SfMarketProductMapping;
import com.ym.agriculture.farming.market.model.entity.SfMarketQuote;
import com.ym.agriculture.farming.market.model.entity.SfMarketQuoteIngestStaging;
import com.ym.agriculture.farming.market.model.entity.SfMarketSource;
import com.ym.agriculture.farming.market.model.vo.SfMarketCategoryVo;
import com.ym.agriculture.farming.market.model.vo.SfMarketIngestDetailVo;
import com.ym.agriculture.farming.market.model.vo.SfMarketIngestListVo;
import com.ym.agriculture.farming.market.model.vo.SfMarketIngestProcessVo;
import com.ym.agriculture.farming.market.model.vo.SfMarketQuoteVo;
import com.ym.agriculture.farming.market.service.ISfMarketService;
import com.ym.agriculture.farming.market.support.SfMarketNormalizationSupport;
import com.ym.agriculture.farming.market.support.SfMarketPayloadHashSupport;
import com.ym.agriculture.farming.market.support.SfMarketPriceSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 农业行情采集、归一、异常处置和移动查询实现。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SfMarketServiceImpl implements ISfMarketService {

    private final SfMarketSourceMapper sourceMapper;
    private final SfMarketQuoteIngestMapper ingestMapper;
    private final SfMarketProductMapper productMapper;
    private final SfMarketProductMappingMapper mappingMapper;
    private final SfMarketQuoteMapper quoteMapper;
    private final SfMarketNormalizationSupport normalizationSupport;
    private final SfMarketPayloadHashSupport payloadHashSupport;
    private final SfMarketPriceSupport priceSupport;
    private final TransactionTemplate transactionTemplate;

    @Override
    public PageResult<SfMarketQuoteVo> queryAdminQuotePage(SfMarketQuoteQueryBo bo, PageQuery pageQuery) {
        validateQuoteQuery(bo);
        Page<SfMarketQuoteVo> page = quoteMapper.selectAdminPage(pageQuery.build(), bo);
        page.getRecords().forEach(this::enrichQuote);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    @Override
    public PageResult<SfMarketIngestListVo> queryIngestPage(SfMarketIngestQueryBo bo, PageQuery pageQuery) {
        Page<SfMarketIngestListVo> page = ingestMapper.selectAdminPage(pageQuery.build(), bo);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    @Override
    public SfMarketIngestDetailVo getIngestDetail(Long id) {
        SfMarketQuoteIngestStaging row = requireIngest(id);
        SfMarketIngestDetailVo vo = new SfMarketIngestDetailVo();
        BeanUtil.copyProperties(row, vo);
        SfMarketSource source = sourceMapper.selectById(row.getSourceCode());
        vo.setSourceName(source == null ? row.getSourceCode() : source.getSourceName());
        return vo;
    }

    @Override
    public SfMarketIngestProcessVo processPending(int limit) {
        List<SfMarketQuoteIngestStaging> pending = ingestMapper.selectPending(Math.max(1, Math.min(limit, 200)));
        ProcessCounters counters = new ProcessCounters();
        for (SfMarketQuoteIngestStaging row : pending) {
            ProcessOutcome outcome = processOne(row);
            counters.accept(outcome);
        }
        return counters.toVo();
    }

    @Override
    public void correctAndReprocess(Long id, SfMarketCorrectionBo bo) {
        if (StrUtil.isNotBlank(bo.getCategory()) && !SfMarketConstants.CATEGORIES.contains(bo.getCategory())) {
            throw new ServiceException("不支持的商品品类");
        }
        SfMarketQuoteIngestStaging row = requireIngest(id);
        if (!List.of(SfMarketConstants.INGEST_REJECTED, SfMarketConstants.INGEST_FAILED,
            SfMarketConstants.INGEST_IGNORED).contains(row.getIngestStatus())) {
            throw new ServiceException("只有已拒绝、处理失败或已忽略记录可以修正");
        }
        Map<String, Object> correction = new LinkedHashMap<>();
        putPresent(correction, "productName", bo.getProductName());
        putPresent(correction, "specification", bo.getSpecification());
        putPresent(correction, "category", bo.getCategory());
        putPresent(correction, "quoteType", bo.getQuoteType());
        putPresent(correction, "price", bo.getPrice());
        putPresent(correction, "unit", bo.getUnit());
        putPresent(correction, "origin", bo.getOrigin());
        putPresent(correction, "marketName", bo.getMarketName());
        putPresent(correction, "quoteDate", bo.getQuoteDate());
        resetForReprocess(id, JSON.toJSONString(correction), bo.getComment());
        processOne(requireIngest(id));
    }

    @Override
    public void retry(Long id) {
        SfMarketQuoteIngestStaging row = requireIngest(id);
        if (!SfMarketConstants.INGEST_FAILED.equals(row.getIngestStatus())) {
            throw new ServiceException("只有平台处理失败记录可以直接重试");
        }
        resetForReprocess(id, row.getCorrectionJson(), "人工重试");
        processOne(requireIngest(id));
    }

    @Override
    public void ignore(Long id, SfMarketHandleBo bo) {
        SfMarketQuoteIngestStaging row = requireIngest(id);
        if (!List.of(SfMarketConstants.INGEST_REJECTED, SfMarketConstants.INGEST_FAILED)
            .contains(row.getIngestStatus())) {
            throw new ServiceException("只有已拒绝或处理失败记录可以忽略");
        }
        Date now = new Date();
        boolean updated = ingestMapper.update(null, Wrappers.<SfMarketQuoteIngestStaging>lambdaUpdate()
            .eq(SfMarketQuoteIngestStaging::getId, id)
            .in(SfMarketQuoteIngestStaging::getIngestStatus,
                SfMarketConstants.INGEST_REJECTED, SfMarketConstants.INGEST_FAILED)
            .set(SfMarketQuoteIngestStaging::getIngestStatus, SfMarketConstants.INGEST_IGNORED)
            .set(SfMarketQuoteIngestStaging::getResultAction, "IGNORED")
            .set(SfMarketQuoteIngestStaging::getHandledBy, LoginHelper.getUserId())
            .set(SfMarketQuoteIngestStaging::getHandlerName, LoginHelper.getUsername())
            .set(SfMarketQuoteIngestStaging::getHandleComment, StrUtil.trim(bo.getComment()))
            .set(SfMarketQuoteIngestStaging::getHandledAt, now)
            .set(SfMarketQuoteIngestStaging::getProcessedAt, now)) > 0;
        if (!updated) {
            throw new ServiceException("记录状态已变化，请刷新后重试");
        }
    }

    @Override
    public void acknowledgeWarning(Long id, SfMarketHandleBo bo) {
        SfMarketQuoteIngestStaging row = requireIngest(id);
        if (!List.of(SfMarketConstants.INGEST_ACCEPTED, SfMarketConstants.INGEST_UNCHANGED)
            .contains(row.getIngestStatus()) || StrUtil.isBlank(row.getWarningsJson())) {
            throw new ServiceException("当前记录没有可确认的已发布告警");
        }
        boolean updated = ingestMapper.update(null, Wrappers.<SfMarketQuoteIngestStaging>lambdaUpdate()
            .eq(SfMarketQuoteIngestStaging::getId, id)
            .in(SfMarketQuoteIngestStaging::getIngestStatus,
                SfMarketConstants.INGEST_ACCEPTED, SfMarketConstants.INGEST_UNCHANGED)
            .set(SfMarketQuoteIngestStaging::getWarningAcknowledged, true)
            .set(SfMarketQuoteIngestStaging::getHandledBy, LoginHelper.getUserId())
            .set(SfMarketQuoteIngestStaging::getHandlerName, LoginHelper.getUsername())
            .set(SfMarketQuoteIngestStaging::getHandleComment, StrUtil.trim(bo.getComment()))
            .set(SfMarketQuoteIngestStaging::getHandledAt, new Date())) > 0;
        if (!updated) {
            throw new ServiceException("记录状态已变化，请刷新后重试");
        }
    }

    @Override
    public PageResult<SfMarketQuoteVo> queryMobilePage(SfMarketQuoteQueryBo bo, PageQuery pageQuery) {
        validateQuoteQuery(bo);
        Page<SfMarketQuoteVo> page = quoteMapper.selectMobilePage(pageQuery.build(), bo);
        page.getRecords().forEach(this::enrichQuote);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    @Override
    public List<SfMarketCategoryVo> categories() {
        List<SfMarketCategoryVo> result = new ArrayList<>();
        for (int index = 0; index < SfMarketConstants.CATEGORY_ORDER.size(); index++) {
            String category = SfMarketConstants.CATEGORY_ORDER.get(index);
            result.add(new SfMarketCategoryVo(category, SfMarketConstants.CATEGORY_NAMES.get(category), index + 1));
        }
        return result;
    }

    private ProcessOutcome processOne(SfMarketQuoteIngestStaging row) {
        if (!ingestMapper.claim(row.getId(), new Date())) {
            return ProcessOutcome.notClaimed();
        }
        try {
            return transactionTemplate.execute(status -> processClaimed(row));
        } catch (SfMarketPayloadHashSupport.PayloadHashException e) {
            markError(row.getId(), SfMarketConstants.INGEST_REJECTED, e.getErrorCode(), e.getMessage());
            return ProcessOutcome.rejected();
        } catch (IllegalArgumentException e) {
            markError(row.getId(), SfMarketConstants.INGEST_REJECTED, "VALIDATION_ERROR", e.getMessage());
            return ProcessOutcome.rejected();
        } catch (Exception e) {
            log.error("农业行情暂存处理失败 requestId={}", row.getRequestId(), e);
            markError(row.getId(), SfMarketConstants.INGEST_FAILED, "INTERNAL_ERROR", "平台处理失败，请联系管理员");
            return ProcessOutcome.failed();
        }
    }

    private ProcessOutcome processClaimed(SfMarketQuoteIngestStaging row) {
        SfMarketSource source = requireSource(row);
        EffectiveQuote effective = effectiveQuote(row);
        validateEffective(row, source, effective);
        SfMarketProduct product = resolveProduct(row, effective);
        SfMarketNormalizationSupport.NormalizedPrice normalized =
            normalizationSupport.normalizePrice(effective.price(), effective.unit());
        String seriesKey = normalizationSupport.seriesKey(row.getSourceCode(), row.getExternalProductKey(),
            effective.specification(), effective.quoteType(), effective.origin(), effective.marketName(), normalized.unit());
        SfMarketQuote existing = quoteMapper.selectBySeriesDate(seriesKey, effective.quoteDate());
        SfMarketPriceSupport.Calculation calculation = calculate(seriesKey, effective.quoteDate(), normalized.price());
        String warningsJson = priceSupport.warningJson(calculation.changePercent());

        if (isUnchanged(existing, product, effective, normalized, warningsJson, row.getSourceUrl())) {
            markSuccess(row.getId(), SfMarketConstants.INGEST_UNCHANGED, "UNCHANGED",
                product.getProductId(), existing.getQuoteId(), warningsJson);
            return ProcessOutcome.unchanged(StrUtil.isNotBlank(warningsJson));
        }

        SfMarketQuote quote = existing == null ? new SfMarketQuote() : existing;
        if (existing == null) {
            quote.setQuoteId(IdWorker.getId());
            quote.setCreateTime(new Date());
        }
        quote.setLatestIngestId(row.getId());
        quote.setProductId(product.getProductId());
        quote.setSourceCode(row.getSourceCode());
        quote.setExternalProductKey(row.getExternalProductKey());
        quote.setSeriesKeySha256(seriesKey);
        quote.setSpecification(effective.specification());
        quote.setQuoteType(effective.quoteType());
        quote.setSourcePrice(effective.price().setScale(6, RoundingMode.HALF_UP));
        quote.setSourceUnit(effective.unit());
        quote.setPrice(normalized.price());
        quote.setUnit(normalized.unit());
        quote.setOrigin(effective.origin());
        quote.setMarketName(effective.marketName());
        quote.setQuoteDate(effective.quoteDate());
        quote.setCollectedAt(row.getFetchedAtUtc());
        quote.setSourceUrl(row.getSourceUrl());
        quote.setPreviousPrice(calculation.previousPrice());
        quote.setChangeAmount(calculation.changeAmount());
        quote.setChangePercent(calculation.changePercent());
        quote.setTrend(calculation.trend());
        quote.setWarningsJson(warningsJson);
        quote.setPublishStatus(SfMarketConstants.PUBLISHED);
        quote.setUpdateTime(new Date());

        String action;
        if (existing == null) {
            try {
                quoteMapper.insert(quote);
                action = "CREATED";
            } catch (DuplicateKeyException e) {
                // 不同暂存记录可能同时命中同一序列和业务日期；唯一索引负责仲裁，
                // 竞争失败方改为更新胜出记录，避免把合法的并发行情标记为系统失败。
                SfMarketQuote concurrent = quoteMapper.selectBySeriesDate(seriesKey, effective.quoteDate());
                if (concurrent == null) {
                    throw e;
                }
                quote.setQuoteId(concurrent.getQuoteId());
                quote.setCreateTime(concurrent.getCreateTime());
                quoteMapper.updateById(quote);
                action = "UPDATED";
            }
        } else {
            quoteMapper.updateById(quote);
            action = "UPDATED";
        }
        recalculateNext(seriesKey, effective.quoteDate());
        markSuccess(row.getId(), SfMarketConstants.INGEST_ACCEPTED, action,
            product.getProductId(), quote.getQuoteId(), warningsJson);
        return new ProcessOutcome(true, action, StrUtil.isNotBlank(warningsJson));
    }

    private SfMarketSource requireSource(SfMarketQuoteIngestStaging row) {
        SfMarketSource source = sourceMapper.selectById(row.getSourceCode());
        if (source == null || !Boolean.TRUE.equals(source.getEnabledFlag())) {
            throw new IllegalArgumentException("来源编码未启用");
        }
        return source;
    }

    private void validateEffective(SfMarketQuoteIngestStaging row, SfMarketSource source, EffectiveQuote effective) {
        if (StrUtil.isBlank(row.getExternalProductKey())) {
            throw new IllegalArgumentException("来源商品标识不能为空");
        }
        if (!List.of(SfMarketConstants.QUOTE_MARKET, SfMarketConstants.QUOTE_OFFICIAL_AVERAGE)
            .contains(effective.quoteType())) {
            throw new IllegalArgumentException("不支持的报价类型");
        }
        if (SfMarketConstants.QUOTE_MARKET.equals(effective.quoteType()) && StrUtil.isBlank(effective.marketName())) {
            throw new IllegalArgumentException("市场报价必须提供市场名称");
        }
        if (effective.quoteDate() == null || effective.quoteDate().isAfter(LocalDate.now().plusDays(1))) {
            throw new IllegalArgumentException("行情业务日期无效");
        }
        payloadHashSupport.validate(row.getRawPayloadJson(), row.getPayloadSha256());
        List<String> domains = JSON.parseArray(source.getAllowedDomainsJson(), String.class);
        normalizationSupport.validateSourceUrl(row.getSourceUrl(), domains);
        normalizationSupport.normalizeProductName(effective.productName());
        normalizationSupport.normalizePrice(effective.price(), effective.unit());
    }

    private SfMarketProduct resolveProduct(SfMarketQuoteIngestStaging row, EffectiveQuote effective) {
        SfMarketProductMapping mapping = mappingMapper.selectBySourceKey(row.getSourceCode(), row.getExternalProductKey());
        if (mapping != null) {
            SfMarketProduct product = productMapper.selectById(mapping.getProductId());
            if (product == null || !Boolean.TRUE.equals(product.getEnabledFlag())) {
                throw new IllegalArgumentException("来源商品映射的平台商品不存在或已停用");
            }
            mappingMapper.update(null, Wrappers.<SfMarketProductMapping>lambdaUpdate()
                .eq(SfMarketProductMapping::getMappingId, mapping.getMappingId())
                .set(SfMarketProductMapping::getLastSourceName, effective.productName())
                .set(SfMarketProductMapping::getUpdateTime, new Date()));
            return product;
        }

        if (!SfMarketConstants.CATEGORIES.contains(effective.category())) {
            throw new IllegalArgumentException("首次归一商品时必须提供可识别的商品品类");
        }
        String normalizedName = normalizationSupport.normalizeProductName(effective.productName());
        SfMarketProduct product = productMapper.selectByCategoryAndName(effective.category(), normalizedName);
        if (product == null) {
            product = new SfMarketProduct();
            product.setProductId(IdWorker.getId());
            product.setProductName(StrUtil.trim(effective.productName()));
            product.setNormalizedName(normalizedName);
            product.setCategory(effective.category());
            product.setSortOrder(0);
            product.setEnabledFlag(true);
            product.setCreateTime(new Date());
            product.setUpdateTime(new Date());
            try {
                productMapper.insert(product);
            } catch (DuplicateKeyException e) {
                product = productMapper.selectByCategoryAndName(effective.category(), normalizedName);
            }
        }
        SfMarketProductMapping created = new SfMarketProductMapping();
        created.setMappingId(IdWorker.getId());
        created.setSourceCode(row.getSourceCode());
        created.setExternalProductKey(row.getExternalProductKey());
        created.setProductId(Objects.requireNonNull(product).getProductId());
        created.setLastSourceName(effective.productName());
        created.setCreateTime(new Date());
        created.setUpdateTime(new Date());
        try {
            mappingMapper.insert(created);
        } catch (DuplicateKeyException e) {
            SfMarketProductMapping concurrent = mappingMapper.selectBySourceKey(row.getSourceCode(), row.getExternalProductKey());
            return productMapper.selectById(Objects.requireNonNull(concurrent).getProductId());
        }
        return product;
    }

    private EffectiveQuote effectiveQuote(SfMarketQuoteIngestStaging row) {
        JSONObject correction = StrUtil.isBlank(row.getCorrectionJson())
            ? new JSONObject() : JSON.parseObject(row.getCorrectionJson());
        String productName = correctedString(correction, "productName", row.getProductName());
        String specification = correctedNullable(correction, "specification", row.getSpecification());
        String category = correction.containsKey("category")
            ? correction.getString("category")
            : StrUtil.isBlank(row.getSourceCategory()) ? null
            : normalizationSupport.normalizeCategory(row.getSourceCategory());
        String quoteType = correctedString(correction, "quoteType", row.getQuoteType());
        BigDecimal price = correction.containsKey("price") ? correction.getBigDecimal("price") : row.getPrice();
        String unit = correctedString(correction, "unit", row.getUnit());
        String origin = correctedNullable(correction, "origin", row.getOrigin());
        String marketName = correctedNullable(correction, "marketName", row.getMarketName());
        LocalDate quoteDate = correction.containsKey("quoteDate")
            ? LocalDate.parse(correction.getString("quoteDate")) : row.getQuoteDate();
        return new EffectiveQuote(productName, specification, category, quoteType, price, unit, origin, marketName, quoteDate);
    }

    private SfMarketPriceSupport.Calculation calculate(String seriesKey, LocalDate quoteDate, BigDecimal price) {
        SfMarketQuote previous = quoteMapper.selectPrevious(seriesKey, quoteDate);
        return priceSupport.calculate(previous == null ? null : previous.getPrice(), price);
    }

    private void recalculateNext(String seriesKey, LocalDate quoteDate) {
        SfMarketQuote next = quoteMapper.selectNext(seriesKey, quoteDate);
        if (next == null) {
            return;
        }
        SfMarketPriceSupport.Calculation calculation = calculate(seriesKey, next.getQuoteDate(), next.getPrice());
        String warnings = priceSupport.warningJson(calculation.changePercent());
        next.setPreviousPrice(calculation.previousPrice());
        next.setChangeAmount(calculation.changeAmount());
        next.setChangePercent(calculation.changePercent());
        next.setTrend(calculation.trend());
        next.setWarningsJson(warnings);
        next.setUpdateTime(new Date());
        quoteMapper.updateById(next);
        ingestMapper.update(null, Wrappers.<SfMarketQuoteIngestStaging>lambdaUpdate()
            .eq(SfMarketQuoteIngestStaging::getId, next.getLatestIngestId())
            .set(SfMarketQuoteIngestStaging::getWarningsJson, warnings)
            .set(SfMarketQuoteIngestStaging::getWarningAcknowledged, false));
    }

    private boolean isUnchanged(SfMarketQuote existing, SfMarketProduct product, EffectiveQuote effective,
                                SfMarketNormalizationSupport.NormalizedPrice normalized, String warningsJson,
                                String sourceUrl) {
        if (existing == null) {
            return false;
        }
        BigDecimal sourcePrice = effective.price().setScale(6, RoundingMode.HALF_UP);
        return Objects.equals(existing.getProductId(), product.getProductId())
            && Objects.equals(existing.getSourcePrice(), sourcePrice)
            && Objects.equals(existing.getSourceUnit(), effective.unit())
            && Objects.equals(existing.getPrice(), normalized.price())
            && Objects.equals(existing.getUnit(), normalized.unit())
            && Objects.equals(existing.getSourceUrl(), sourceUrl)
            && Objects.equals(existing.getWarningsJson(), warningsJson);
    }

    private void markSuccess(Long id, String status, String action, Long productId, Long quoteId, String warningsJson) {
        ingestMapper.update(null, Wrappers.<SfMarketQuoteIngestStaging>lambdaUpdate()
            .eq(SfMarketQuoteIngestStaging::getId, id)
            .eq(SfMarketQuoteIngestStaging::getIngestStatus, SfMarketConstants.INGEST_PROCESSING)
            .set(SfMarketQuoteIngestStaging::getIngestStatus, status)
            .set(SfMarketQuoteIngestStaging::getResultAction, action)
            .set(SfMarketQuoteIngestStaging::getProductId, productId)
            .set(SfMarketQuoteIngestStaging::getQuoteId, quoteId)
            .set(SfMarketQuoteIngestStaging::getWarningsJson, warningsJson)
            .set(SfMarketQuoteIngestStaging::getErrorCode, null)
            .set(SfMarketQuoteIngestStaging::getErrorMessage, null)
            .set(SfMarketQuoteIngestStaging::getProcessedAt, new Date()));
    }

    private void markError(Long id, String status, String errorCode, String errorMessage) {
        ingestMapper.update(null, Wrappers.<SfMarketQuoteIngestStaging>lambdaUpdate()
            .eq(SfMarketQuoteIngestStaging::getId, id)
            .eq(SfMarketQuoteIngestStaging::getIngestStatus, SfMarketConstants.INGEST_PROCESSING)
            .set(SfMarketQuoteIngestStaging::getIngestStatus, status)
            .set(SfMarketQuoteIngestStaging::getErrorCode, errorCode)
            .set(SfMarketQuoteIngestStaging::getErrorMessage, StrUtil.maxLength(errorMessage, 1000))
            .set(SfMarketQuoteIngestStaging::getProcessedAt, new Date()));
    }

    private void resetForReprocess(Long id, String correctionJson, String comment) {
        ingestMapper.update(null, Wrappers.<SfMarketQuoteIngestStaging>lambdaUpdate()
            .eq(SfMarketQuoteIngestStaging::getId, id)
            .set(SfMarketQuoteIngestStaging::getCorrectionJson, correctionJson)
            .set(SfMarketQuoteIngestStaging::getIngestStatus, SfMarketConstants.INGEST_PENDING)
            .set(SfMarketQuoteIngestStaging::getResultAction, null)
            .set(SfMarketQuoteIngestStaging::getErrorCode, null)
            .set(SfMarketQuoteIngestStaging::getErrorMessage, null)
            .set(SfMarketQuoteIngestStaging::getWarningAcknowledged, false)
            .set(SfMarketQuoteIngestStaging::getHandledBy, LoginHelper.getUserId())
            .set(SfMarketQuoteIngestStaging::getHandlerName, LoginHelper.getUsername())
            .set(SfMarketQuoteIngestStaging::getHandleComment, StrUtil.trim(comment))
            .set(SfMarketQuoteIngestStaging::getHandledAt, new Date())
            .set(SfMarketQuoteIngestStaging::getProcessingStartedAt, null)
            .set(SfMarketQuoteIngestStaging::getProcessedAt, null));
    }

    private SfMarketQuoteIngestStaging requireIngest(Long id) {
        SfMarketQuoteIngestStaging row = ingestMapper.selectById(id);
        if (row == null) {
            throw new ServiceException("行情采集记录不存在");
        }
        return row;
    }

    private void validateQuoteQuery(SfMarketQuoteQueryBo bo) {
        if (StrUtil.isNotBlank(bo.getCategory()) && !SfMarketConstants.CATEGORIES.contains(bo.getCategory())) {
            throw new ServiceException("不支持的行情品类");
        }
        if (StrUtil.isNotBlank(bo.getQuoteType())
            && !List.of(SfMarketConstants.QUOTE_MARKET, SfMarketConstants.QUOTE_OFFICIAL_AVERAGE)
            .contains(bo.getQuoteType())) {
            throw new ServiceException("不支持的报价类型");
        }
    }

    private void enrichQuote(SfMarketQuoteVo vo) {
        vo.setDisplayName(StrUtil.isBlank(vo.getSpecification())
            ? vo.getProductName() : vo.getProductName() + "（" + vo.getSpecification() + "）");
        vo.setCategoryName(SfMarketConstants.CATEGORY_NAMES.get(vo.getCategory()));
    }

    private String correctedString(JSONObject correction, String key, String original) {
        return StrUtil.trim(correction.containsKey(key) ? correction.getString(key) : original);
    }

    private String correctedNullable(JSONObject correction, String key, String original) {
        String value = correctedString(correction, key, original);
        return StrUtil.isBlank(value) ? null : value;
    }

    private void putPresent(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private record EffectiveQuote(String productName, String specification, String category, String quoteType,
                                  BigDecimal price, String unit, String origin, String marketName,
                                  LocalDate quoteDate) {
    }

    private record ProcessOutcome(boolean claimed, String action, boolean warning) {
        private static ProcessOutcome notClaimed() { return new ProcessOutcome(false, null, false); }
        private static ProcessOutcome rejected() { return new ProcessOutcome(true, "REJECTED", false); }
        private static ProcessOutcome failed() { return new ProcessOutcome(true, "FAILED", false); }
        private static ProcessOutcome unchanged(boolean warning) { return new ProcessOutcome(true, "UNCHANGED", warning); }
    }

    private static final class ProcessCounters {
        private int claimed;
        private int created;
        private int updated;
        private int unchanged;
        private int rejected;
        private int failed;
        private int warnings;

        private void accept(ProcessOutcome outcome) {
            if (!outcome.claimed()) return;
            claimed++;
            if (outcome.warning()) warnings++;
            switch (StrUtil.nullToEmpty(outcome.action())) {
                case "CREATED" -> created++;
                case "UPDATED" -> updated++;
                case "UNCHANGED" -> unchanged++;
                case "REJECTED" -> rejected++;
                case "FAILED" -> failed++;
                default -> { }
            }
        }

        private SfMarketIngestProcessVo toVo() {
            return new SfMarketIngestProcessVo(claimed, created, updated, unchanged, rejected, failed, warnings);
        }
    }
}
