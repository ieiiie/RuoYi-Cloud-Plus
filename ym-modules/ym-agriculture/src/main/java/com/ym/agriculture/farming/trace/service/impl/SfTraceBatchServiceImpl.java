package com.ym.agriculture.farming.trace.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.agriculture.farming.batch.dao.SfPlantingBatchMapper;
import com.ym.agriculture.farming.batch.model.entity.SfPlantingBatch;
import com.ym.agriculture.farming.crop.dao.SfCropSpeciesMapper;
import com.ym.agriculture.farming.crop.dao.SfCropVarietyMapper;
import com.ym.agriculture.farming.crop.model.entity.SfCropSpecies;
import com.ym.agriculture.farming.crop.model.entity.SfCropVariety;
import com.ym.agriculture.farming.field.dao.SfFieldMapper;
import com.ym.agriculture.farming.field.model.entity.SfField;
import com.ym.agriculture.farming.trace.config.TraceProperties;
import com.ym.agriculture.farming.trace.dao.SfTraceBatchMapper;
import com.ym.agriculture.farming.trace.dao.SfTraceCodeMapper;
import com.ym.agriculture.farming.trace.model.bo.SfTraceBatchBo;
import com.ym.agriculture.farming.trace.model.bo.SfTraceCodeBo;
import com.ym.agriculture.farming.trace.model.bo.SfTraceCodeGenerateBo;
import com.ym.agriculture.farming.trace.model.bo.SfTraceCodeVoidBatchBo;
import com.ym.agriculture.farming.trace.model.constants.TraceBatchStatus;
import com.ym.agriculture.farming.trace.model.constants.TraceCodeStatus;
import com.ym.agriculture.farming.trace.model.constants.TraceLabelScope;
import com.ym.agriculture.farming.trace.model.entity.SfTraceBatch;
import com.ym.agriculture.farming.trace.model.entity.SfTraceCode;
import com.ym.agriculture.farming.trace.model.vo.SfTraceBatchStatsVo;
import com.ym.agriculture.farming.trace.model.vo.SfTraceBatchVo;
import com.ym.agriculture.farming.trace.model.vo.SfTraceCodeVo;
import com.ym.agriculture.farming.trace.service.ISfTraceBatchService;
import com.ym.agriculture.farming.trace.support.TraceCertificationSupport;
import com.ym.agriculture.farming.trace.support.TraceCodeGenerator;
import com.ym.agriculture.farming.trace.support.TraceLabelPdfRenderer;
import com.ym.agriculture.farming.trace.support.TraceLabelPdfRenderer.LabelItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 溯源批次与溯源码管理实现。
 */
@RequiredArgsConstructor
@Service
public class SfTraceBatchServiceImpl implements ISfTraceBatchService {

    private final SfTraceBatchMapper batchMapper;
    private final SfTraceCodeMapper codeMapper;
    private final SfPlantingBatchMapper plantingBatchMapper;
    private final SfFieldMapper fieldMapper;
    private final SfCropVarietyMapper varietyMapper;
    private final SfCropSpeciesMapper speciesMapper;
    private final TraceProperties traceProperties;

    @Override
    public SfTraceBatchVo queryById(Long traceBatchId) {
        SfTraceBatchVo vo = batchMapper.selectVoById(traceBatchId);
        if (vo == null) {
            throw new ServiceException("溯源批次不存在");
        }
        assertTenant(vo.getTenantId());
        enrichBatchVo(vo);
        return vo;
    }

    @Override
    public PageResult<SfTraceBatchVo> queryPageList(SfTraceBatchBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SfTraceBatch> lqw = batchMapper.buildQueryWrapper(bo);
        Page<SfTraceBatchVo> page = batchMapper.selectVoPage(pageQuery.build(), lqw);
        page.getRecords().forEach(this::enrichBatchVo);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(SfTraceBatchBo bo) {
        String tenantId = LoginHelper.getTenantId();
        if (batchMapper.selectByTenantAndNo(tenantId, bo.getTraceBatchNo()) != null) {
            throw new ServiceException("溯源批次号已存在");
        }
        resolveCreateFields(bo, tenantId);
        SfTraceBatch entity = MapstructUtils.convert(bo, SfTraceBatch.class);
        entity.setTraceBatchId(IdUtil.getSnowflakeNextId());
        entity.setTenantId(tenantId);
        validatePlannedQuantityForSave(entity.getPlannedQuantity(), 0);
        if (StringUtils.isBlank(entity.getLabelScope())) {
            entity.setLabelScope(TraceLabelScope.SINGLE_FRUIT);
        }
        if (entity.getPlannedQuantity() == null) {
            entity.setPlannedQuantity(0);
        }
        entity.setGeneratedQuantity(0);
        entity.setStatus(TraceBatchStatus.DRAFT);
        entity.setDelFlag(SystemConstants.NORMAL);
        return batchMapper.insert(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(SfTraceBatchBo bo) {
        SfTraceBatch existing = requireBatchOwned(bo.getTraceBatchId());
        if (TraceBatchStatus.DISABLED.equals(existing.getStatus())) {
            throw new ServiceException("已停用的溯源批次不可修改");
        }
        if (StringUtils.isNotBlank(bo.getTraceBatchNo())
                && !bo.getTraceBatchNo().equals(existing.getTraceBatchNo())) {
            SfTraceBatch dup = batchMapper.selectByTenantAndNo(existing.getTenantId(), bo.getTraceBatchNo());
            if (dup != null && !dup.getTraceBatchId().equals(existing.getTraceBatchId())) {
                throw new ServiceException("溯源批次号已存在");
            }
        }
        if (bo.getPlantingBatchId() != null) {
            fillFromPlantingBatch(bo, existing.getTenantId());
        } else if (bo.getFieldId() != null || bo.getVarietyId() != null) {
            validateFieldAndVariety(bo.getFieldId() != null ? bo.getFieldId() : existing.getFieldId(),
                    bo.getVarietyId() != null ? bo.getVarietyId() : existing.getVarietyId(),
                    existing.getTenantId());
        }
        if (bo.getPlannedQuantity() != null) {
            validatePlannedQuantityForSave(bo.getPlannedQuantity(),
                    existing.getGeneratedQuantity() == null ? 0 : existing.getGeneratedQuantity());
        }
        SfTraceBatch patch = MapstructUtils.convert(bo, SfTraceBatch.class);
        patch.setTraceBatchId(existing.getTraceBatchId());
        return batchMapper.updateById(patch) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> traceBatchIds) {
        for (Long id : traceBatchIds) {
            SfTraceBatch batch = requireBatchOwned(id);
            if (TraceBatchStatus.PUBLISHED.equals(batch.getStatus())) {
                throw new ServiceException("已发布的溯源批次不可删除，请先停用");
            }
            long codeCount = codeMapper.countByBatchAndStatus(id, null);
            if (codeCount > 0) {
                throw new ServiceException("批次已生成溯源码，不可删除");
            }
        }
        return batchMapper.deleteByIds(traceBatchIds) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean publish(Long traceBatchId) {
        SfTraceBatch batch = requireBatchOwned(traceBatchId);
        if (batch.getGeneratedQuantity() == null || batch.getGeneratedQuantity() <= 0) {
            throw new ServiceException("请先生成溯源码再发布");
        }
        if (TraceBatchStatus.DISABLED.equals(batch.getStatus())) {
            throw new ServiceException("已停用的批次不可发布");
        }
        batch.setStatus(TraceBatchStatus.PUBLISHED);
        return batchMapper.updateById(batch) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean disable(Long traceBatchId) {
        SfTraceBatch batch = requireBatchOwned(traceBatchId);
        batch.setStatus(TraceBatchStatus.DISABLED);
        return batchMapper.updateById(batch) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateCodes(Long traceBatchId, SfTraceCodeGenerateBo bo) {
        SfTraceBatch batch = requireBatchOwnedForUpdate(traceBatchId);
        if (!TraceBatchStatus.DRAFT.equals(batch.getStatus())
                && !TraceBatchStatus.GENERATED.equals(batch.getStatus())) {
            throw new ServiceException("当前批次状态不允许生成溯源码");
        }
        if (bo == null || bo.getQuantity() == null || bo.getQuantity() < 1 || bo.getQuantity() > 5000) {
            throw new ServiceException("本次生成数量需在 1-5000 之间");
        }
        int quantity = bo.getQuantity();
        int max = traceProperties.getMaxGeneratePerRequest();
        if (quantity > max) {
            throw new ServiceException("单次生成数量不能超过" + max);
        }
        int plannedQuantity = batch.getPlannedQuantity() == null ? 0 : batch.getPlannedQuantity();
        int generatedQuantity = batch.getGeneratedQuantity() == null ? 0 : batch.getGeneratedQuantity();
        if (plannedQuantity <= 0) {
            throw new ServiceException("计划生成数量需大于 0");
        }
        int remaining = plannedQuantity - generatedQuantity;
        if (remaining <= 0) {
            throw new ServiceException("已生成数量已达到计划数量，不能继续生成");
        }
        if (quantity > remaining) {
            throw new ServiceException("本次最多可生成 " + remaining + " 个溯源码");
        }
        int startSeq = codeMapper.maxSeqNo(traceBatchId) + 1;
        LocalDate today = LocalDate.now();
        List<String> codes = TraceCodeGenerator.generateBatch(traceBatchId, startSeq, quantity, today);
        List<SfTraceCode> rows = new ArrayList<>(quantity);
        LocalDateTime createTime = LocalDateTime.now();
        Long userId = LoginHelper.getUserId();
        for (int i = 0; i < quantity; i++) {
            SfTraceCode row = new SfTraceCode();
            row.setTraceCodeId(IdUtil.getSnowflakeNextId());
            row.setTenantId(batch.getTenantId());
            row.setTraceBatchId(traceBatchId);
            row.setTraceCode(codes.get(i));
            row.setSeqNo(startSeq + i);
            row.setStatus(TraceCodeStatus.NORMAL);
            row.setScanCount(0);
            row.setCreateBy(userId);
            row.setCreateTime(createTime);
            rows.add(row);
        }
        codeMapper.insertBatch(rows);
        int newGenerated = generatedQuantity + quantity;
        batch.setGeneratedQuantity(newGenerated);
        if (TraceBatchStatus.DRAFT.equals(batch.getStatus())) {
            batch.setStatus(TraceBatchStatus.GENERATED);
        }
        batchMapper.updateById(batch);
        return quantity;
    }

    @Override
    public PageResult<SfTraceCodeVo> queryCodePageList(SfTraceCodeBo bo, PageQuery pageQuery) {
        if (bo.getTraceBatchId() != null) {
            requireBatchOwned(bo.getTraceBatchId());
        }
        Page<SfTraceCodeVo> page = codeMapper.selectVoPage(pageQuery.build(), codeMapper.buildQueryWrapper(bo));
        enrichCodeVos(page.getRecords());
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean voidCode(Long traceCodeId) {
        SfTraceCode code = requireCodeOwned(traceCodeId);
        if (TraceCodeStatus.VOID.equals(code.getStatus())) {
            return true;
        }
        code.setStatus(TraceCodeStatus.VOID);
        return codeMapper.updateById(code) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean voidCodeBatch(SfTraceCodeVoidBatchBo bo) {
        for (Long id : bo.getTraceCodeIds()) {
            voidCode(id);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean reprint(Long traceCodeId) {
        SfTraceCode code = requireCodeOwned(traceCodeId);
        code.setLastPrintTime(new Date());
        return codeMapper.updateById(code) > 0;
    }

    @Override
    public SfTraceBatchStatsVo stats(Long traceBatchId) {
        requireBatchOwned(traceBatchId);
        SfTraceBatchStatsVo vo = new SfTraceBatchStatsVo();
        vo.setGeneratedCount(codeMapper.countByBatchAndStatus(traceBatchId, null));
        vo.setVoidCount(codeMapper.countByBatchAndStatus(traceBatchId, TraceCodeStatus.VOID));
        vo.setNormalCount(codeMapper.countByBatchAndStatus(traceBatchId, TraceCodeStatus.NORMAL));
        vo.setScanCount(codeMapper.sumScanCount(traceBatchId));
        vo.setFirstScanCount(codeMapper.countFirstScanned(traceBatchId));
        vo.setRepeatScanCount(codeMapper.countRepeatScanned(traceBatchId));
        return vo;
    }

    @Override
    public byte[] exportBatchLabelsPdf(Long traceBatchId) {
        SfTraceBatch batch = requireBatchOwned(traceBatchId);
        List<SfTraceCode> codes = codeMapper.selectNormalByBatch(traceBatchId);
        if (codes.isEmpty()) {
            throw new ServiceException("该批次没有可导出的正常溯源码");
        }
        String base = normalizeH5Base();
        List<LabelItem> items = codes.stream()
                .map(c -> new LabelItem(base + "/" + c.getTraceCode(), batch.getProductName(),
                        batch.getTraceBatchNo(), c.getTraceCode()))
                .toList();
        Date now = new Date();
        for (SfTraceCode c : codes) {
            c.setLastPrintTime(now);
            codeMapper.updateById(c);
        }
        return TraceLabelPdfRenderer.renderBatch(items);
    }

    @Override
    public byte[] exportSingleLabelPdf(Long traceCodeId) {
        SfTraceCode code = requireCodeOwned(traceCodeId);
        SfTraceBatch batch = requireBatchOwned(code.getTraceBatchId());
        String url = normalizeH5Base() + "/" + code.getTraceCode();
        code.setLastPrintTime(new Date());
        codeMapper.updateById(code);
        return TraceLabelPdfRenderer.renderSingle(
                new LabelItem(url, batch.getProductName(), batch.getTraceBatchNo(), code.getTraceCode()));
    }

    private void resolveCreateFields(SfTraceBatchBo bo, String tenantId) {
        if (StringUtils.isBlank(bo.getProductName()) || StringUtils.isBlank(bo.getOriginText())
                || StringUtils.isBlank(bo.getProducerName())) {
            throw new ServiceException("商品名称、产地与生产单位不能为空");
        }
        if (bo.getPlantingBatchId() != null) {
            fillFromPlantingBatch(bo, tenantId);
        } else {
            if (bo.getFieldId() == null || bo.getVarietyId() == null) {
                throw new ServiceException("未关联种植批次时，地块与品种不能为空");
            }
            validateFieldAndVariety(bo.getFieldId(), bo.getVarietyId(), tenantId);
        }
    }

    private void fillFromPlantingBatch(SfTraceBatchBo bo, String tenantId) {
        SfPlantingBatch planting = plantingBatchMapper.selectById(bo.getPlantingBatchId());
        if (planting == null || !SystemConstants.NORMAL.equals(planting.getDelFlag())) {
            throw new ServiceException("种植批次不存在");
        }
        if (!tenantId.equals(planting.getTenantId())) {
            throw new ServiceException("种植批次不属于当前租户");
        }
        bo.setFieldId(planting.getFieldId());
        bo.setVarietyId(planting.getVarietyId());
        validateFieldAndVariety(planting.getFieldId(), planting.getVarietyId(), tenantId);
    }

    private void validateFieldAndVariety(Long fieldId, Long varietyId, String tenantId) {
        SfField field = fieldMapper.selectById(fieldId);
        if (field == null || !SystemConstants.NORMAL.equals(field.getDelFlag())
                || !tenantId.equals(field.getTenantId())) {
            throw new ServiceException("地块不存在或不属于当前租户");
        }
        SfCropVariety variety = varietyMapper.selectById(varietyId);
        if (variety == null || !SystemConstants.NORMAL.equals(variety.getDelFlag())
                || !tenantId.equals(variety.getTenantId())) {
            throw new ServiceException("品种不存在或不属于当前租户");
        }
    }

    private SfTraceBatch requireBatchOwned(Long traceBatchId) {
        SfTraceBatch batch = batchMapper.selectById(traceBatchId);
        if (batch == null || !SystemConstants.NORMAL.equals(batch.getDelFlag())) {
            throw new ServiceException("溯源批次不存在");
        }
        assertTenant(batch.getTenantId());
        return batch;
    }

    private SfTraceBatch requireBatchOwnedForUpdate(Long traceBatchId) {
        String tenantId = LoginHelper.getTenantId();
        SfTraceBatch batch = batchMapper.selectByIdForUpdate(traceBatchId, tenantId);
        if (batch == null) {
            throw new ServiceException("溯源批次不存在");
        }
        return batch;
    }

    private SfTraceCode requireCodeOwned(Long traceCodeId) {
        SfTraceCode code = codeMapper.selectById(traceCodeId);
        if (code == null) {
            throw new ServiceException("溯源码不存在");
        }
        assertTenant(code.getTenantId());
        requireBatchOwned(code.getTraceBatchId());
        return code;
    }

    private void assertTenant(String tenantId) {
        if (!Objects.equals(LoginHelper.getTenantId(), tenantId)) {
            throw new ServiceException("无权访问该溯源数据");
        }
    }

    private void validatePlannedQuantityForSave(Integer plannedQuantity, int generatedQuantity) {
        if (plannedQuantity == null) {
            return;
        }
        if (plannedQuantity < 0) {
            throw new ServiceException("计划数量不能小于0");
        }
        if (plannedQuantity < generatedQuantity) {
            throw new ServiceException("计划数量不能小于已生成数量");
        }
    }

    private void enrichBatchVo(SfTraceBatchVo vo) {
        vo.setCertifications(TraceCertificationSupport.parse(vo.getCertificationJson()));
        if (vo.getFieldId() != null) {
            SfField field = fieldMapper.selectById(vo.getFieldId());
            if (field != null) {
                vo.setFieldName(field.getFieldName());
            }
        }
        if (vo.getVarietyId() != null) {
            SfCropVariety variety = varietyMapper.selectById(vo.getVarietyId());
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
        if (vo.getPlantingBatchId() != null) {
            SfPlantingBatch planting = plantingBatchMapper.selectById(vo.getPlantingBatchId());
            if (planting != null) {
                vo.setBatchCode(planting.getBatchCode());
                vo.setSowingDate(planting.getSowingDate());
                vo.setExpectedHarvestDate(planting.getExpectedHarvestDate());
                vo.setActualHarvestDate(planting.getActualHarvestDate());
            }
        }
    }

    private void enrichCodeVos(List<SfTraceCodeVo> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Set<Long> batchIds = rows.stream().map(SfTraceCodeVo::getTraceBatchId).collect(Collectors.toSet());
        Map<Long, SfTraceBatch> batchMap = batchMapper.selectByIds(batchIds).stream()
                .collect(Collectors.toMap(SfTraceBatch::getTraceBatchId, b -> b));
        for (SfTraceCodeVo vo : rows) {
            SfTraceBatch b = batchMap.get(vo.getTraceBatchId());
            if (b != null) {
                vo.setTraceBatchNo(b.getTraceBatchNo());
                vo.setProductName(b.getProductName());
            }
        }
    }

    private String normalizeH5Base() {
        String base = traceProperties.getH5BaseUrl();
        if (StringUtils.isBlank(base)) {
            throw new ServiceException("未配置 ym.trace.h5-base-url");
        }
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }
}
