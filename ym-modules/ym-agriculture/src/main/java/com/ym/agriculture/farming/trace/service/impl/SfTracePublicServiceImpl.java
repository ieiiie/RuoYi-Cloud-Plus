package com.ym.agriculture.farming.trace.service.impl;

import cn.hutool.core.util.IdUtil;
import com.ym.common.core.utils.ServletUtils;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farming.batch.dao.SfPlantingBatchMapper;
import com.ym.agriculture.farming.batch.model.entity.SfPlantingBatch;
import com.ym.agriculture.farming.crop.dao.SfCropSpeciesMapper;
import com.ym.agriculture.farming.crop.dao.SfCropVarietyMapper;
import com.ym.agriculture.farming.crop.model.entity.SfCropSpecies;
import com.ym.agriculture.farming.crop.model.entity.SfCropVariety;
import com.ym.agriculture.farming.field.dao.SfFieldMapper;
import com.ym.agriculture.farming.field.model.entity.SfField;
import com.ym.agriculture.farming.trace.dao.SfTraceBatchMapper;
import com.ym.agriculture.farming.trace.dao.SfTraceCodeMapper;
import com.ym.agriculture.farming.trace.dao.SfTraceScanLogMapper;
import com.ym.agriculture.farming.trace.model.constants.TraceBatchStatus;
import com.ym.agriculture.farming.trace.model.constants.TraceCodeStatus;
import com.ym.agriculture.farming.trace.model.constants.TraceScanResult;
import com.ym.agriculture.farming.trace.model.entity.SfTraceBatch;
import com.ym.agriculture.farming.trace.model.entity.SfTraceCode;
import com.ym.agriculture.farming.trace.model.entity.SfTraceScanLog;
import com.ym.agriculture.farming.trace.model.vo.SfTracePublicVo;
import com.ym.agriculture.farming.trace.service.ISfTracePublicService;
import com.ym.agriculture.farming.trace.support.TraceCertificationSupport;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * H5 公开溯源查询：全局按溯源码反查租户，记录扫码日志。
 */
@RequiredArgsConstructor
@Service
public class SfTracePublicServiceImpl implements ISfTracePublicService {

    private final SfTraceCodeMapper codeMapper;
    private final SfTraceBatchMapper batchMapper;
    private final SfTraceScanLogMapper scanLogMapper;
    private final SfPlantingBatchMapper plantingBatchMapper;
    private final SfFieldMapper fieldMapper;
    private final SfCropVarietyMapper varietyMapper;
    private final SfCropSpeciesMapper speciesMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SfTracePublicVo queryPublic(String traceCode, HttpServletRequest request) {
        String codeText = StringUtils.trim(traceCode);
        if (StringUtils.isBlank(codeText)) {
            return notFoundVo(codeText, request, null, null);
        }
        return TenantHelper.ignore(() -> doQuery(codeText, request));
    }

    private SfTracePublicVo doQuery(String traceCode, HttpServletRequest request) {
        SfTraceCode code = codeMapper.selectByTraceCode(traceCode);
        if (code == null) {
            return notFoundVo(traceCode, request, null, null);
        }
        SfTraceBatch batch = batchMapper.selectById(code.getTraceBatchId());
        if (batch == null) {
            return notFoundVo(traceCode, request, code, null);
        }
        if (TraceCodeStatus.VOID.equals(code.getStatus())) {
            appendScanLog(traceCode, request, code, batch.getTenantId(), TraceScanResult.VOID, false);
            SfTracePublicVo vo = baseVo(code, batch);
            vo.setStatus(TraceCodeStatus.VOID);
            return vo;
        }
        if (TraceBatchStatus.DISABLED.equals(batch.getStatus())) {
            appendScanLog(traceCode, request, code, batch.getTenantId(), TraceScanResult.DISABLED, false);
            SfTracePublicVo vo = baseVo(code, batch);
            vo.setStatus(TraceBatchStatus.DISABLED);
            return vo;
        }
        if (!TraceBatchStatus.PUBLISHED.equals(batch.getStatus())) {
            appendScanLog(traceCode, request, code, batch.getTenantId(), TraceScanResult.UNPUBLISHED, false);
            SfTracePublicVo vo = baseVo(code, batch);
            vo.setStatus(TraceScanResult.UNPUBLISHED);
            return vo;
        }
        boolean repeat = code.getFirstScanTime() != null;
        Date now = new Date();
        if (!repeat) {
            code.setFirstScanTime(now);
            code.setScanCount(1);
        } else {
            code.setScanCount((code.getScanCount() == null ? 0 : code.getScanCount()) + 1);
        }
        code.setLastScanTime(now);
        codeMapper.updateById(code);
        appendScanLog(traceCode, request, code, batch.getTenantId(), TraceScanResult.OK, repeat);

        SfTracePublicVo vo = fillPublicDetail(code, batch);
        vo.setStatus(TraceCodeStatus.NORMAL);
        vo.setRepeatScan(repeat);
        vo.setFirstScanTime(code.getFirstScanTime());
        vo.setScanCount(code.getScanCount());
        return vo;
    }

    private SfTracePublicVo fillPublicDetail(SfTraceCode code, SfTraceBatch batch) {
        SfTracePublicVo vo = baseVo(code, batch);
        vo.setCertifications(TraceCertificationSupport.parse(batch.getCertificationJson()));
        if (batch.getFieldId() != null) {
            SfField field = fieldMapper.selectById(batch.getFieldId());
            if (field != null) {
                vo.setFieldName(field.getFieldName());
            }
        }
        if (batch.getVarietyId() != null) {
            SfCropVariety variety = varietyMapper.selectById(batch.getVarietyId());
            if (variety != null) {
                vo.setVarietyName(variety.getVarietyName());
                if (variety.getSpeciesId() != null) {
                    SfCropSpecies species = speciesMapper.selectById(variety.getSpeciesId());
                    if (species != null) {
                        vo.setSpeciesName(species.getSpeciesName());
                    }
                }
            }
        }
        if (batch.getPlantingBatchId() != null) {
            SfPlantingBatch planting = plantingBatchMapper.selectById(batch.getPlantingBatchId());
            if (planting != null) {
                vo.setBatchCode(planting.getBatchCode());
                vo.setSowingDate(planting.getSowingDate());
                vo.setExpectedHarvestDate(planting.getExpectedHarvestDate());
                vo.setActualHarvestDate(planting.getActualHarvestDate());
            }
        }
        return vo;
    }

    private SfTracePublicVo baseVo(SfTraceCode code, SfTraceBatch batch) {
        SfTracePublicVo vo = new SfTracePublicVo();
        vo.setTraceCode(code.getTraceCode());
        if (batch != null) {
            vo.setProductName(batch.getProductName());
            vo.setQualityGrade(batch.getQualityGrade());
            vo.setOriginText(batch.getOriginText());
            vo.setProducerName(batch.getProducerName());
        }
        return vo;
    }

    private SfTracePublicVo notFoundVo(String traceCode, HttpServletRequest request,
                                       SfTraceCode code, SfTraceBatch batch) {
        appendScanLog(traceCode, request, code,
            batch != null ? batch.getTenantId() : (code != null ? code.getTenantId() : null),
            TraceScanResult.NOT_FOUND, false);
        SfTracePublicVo vo = new SfTracePublicVo();
        vo.setTraceCode(traceCode);
        vo.setStatus(TraceScanResult.NOT_FOUND);
        vo.setRepeatScan(false);
        return vo;
    }

    private void appendScanLog(String traceCode, HttpServletRequest request, SfTraceCode code,
                               String tenantId, String scanResult, boolean repeat) {
        SfTraceScanLog log = new SfTraceScanLog();
        log.setScanLogId(IdUtil.getSnowflakeNextId());
        log.setTenantId(tenantId);
        log.setTraceCodeId(code != null ? code.getTraceCodeId() : null);
        log.setTraceCode(traceCode);
        log.setScanTime(new Date());
        log.setScanResult(scanResult);
        log.setIsRepeat(repeat ? 1 : 0);
        if (request != null) {
            log.setIp(ServletUtils.getClientIP(request));
            log.setUserAgent(request.getHeader("User-Agent"));
            log.setReferer(request.getHeader("Referer"));
        }
        scanLogMapper.insert(log);
    }
}
