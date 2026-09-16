package com.ym.agriculture.farmtask.yieldrecord.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.agriculture.farming.crop.dao.SfCropSpeciesMapper;
import com.ym.agriculture.farming.crop.dao.SfCropVarietyMapper;
import com.ym.agriculture.farming.crop.model.entity.SfCropSpecies;
import com.ym.agriculture.farming.crop.model.entity.SfCropVariety;
import com.ym.agriculture.farmtask.yieldrecord.dao.SfStaskYieldRecordMapper;
import com.ym.agriculture.farmtask.yieldrecord.model.bo.SfStaskYieldBatchCreateBo;
import com.ym.agriculture.farmtask.yieldrecord.model.bo.SfStaskYieldDetailBo;
import com.ym.agriculture.farmtask.yieldrecord.model.bo.SfStaskYieldQueryBo;
import com.ym.agriculture.farmtask.yieldrecord.model.bo.SfStaskYieldUpdateBo;
import com.ym.agriculture.farmtask.yieldrecord.model.entity.SfStaskYieldRecord;
import com.ym.agriculture.farmtask.yieldrecord.model.vo.SfStaskAnnualYieldRowVo;
import com.ym.agriculture.farmtask.yieldrecord.model.vo.SfStaskYieldRecordVo;
import com.ym.agriculture.farmtask.yieldrecord.service.ISfStaskYieldRecordService;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.tenant.helper.TenantHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 产量记录服务实现。
 */
@Service
@RequiredArgsConstructor
public class SfStaskYieldRecordServiceImpl implements ISfStaskYieldRecordService {

    private final SfStaskYieldRecordMapper yieldRecordMapper;
    private final SfCropSpeciesMapper speciesMapper;
    private final SfCropVarietyMapper varietyMapper;

    @Override
    public PageResult<SfStaskYieldRecordVo> page(SfStaskYieldQueryBo bo, PageQuery pageQuery) {
        if (bo.getHarvestDateStart() != null
            && bo.getHarvestDateEnd() != null
            && bo.getHarvestDateStart().isAfter(bo.getHarvestDateEnd())) {
            throw new ServiceException("收获日期开始时间不能晚于结束时间");
        }
        Page<SfStaskYieldRecord> source = yieldRecordMapper.selectPage(
            pageQuery.build(),
            Wrappers.<SfStaskYieldRecord>lambdaQuery()
                .eq(SfStaskYieldRecord::getTenantId, requireTenantId())
                .ge(bo.getHarvestDateStart() != null,
                    SfStaskYieldRecord::getHarvestDate, bo.getHarvestDateStart())
                .le(bo.getHarvestDateEnd() != null,
                    SfStaskYieldRecord::getHarvestDate, bo.getHarvestDateEnd())
                .eq(bo.getSpeciesId() != null,
                    SfStaskYieldRecord::getSpeciesId, bo.getSpeciesId())
                .eq(bo.getVarietyId() != null,
                    SfStaskYieldRecord::getVarietyId, bo.getVarietyId())
                .orderByDesc(SfStaskYieldRecord::getHarvestDate)
                .orderByDesc(SfStaskYieldRecord::getVarietyNameSnapshot)
                .orderByDesc(SfStaskYieldRecord::getYieldId));
        return PageResult.build(source.getRecords().stream().map(this::toVo).toList(), source.getTotal());
    }

    @Override
    public SfStaskYieldRecordVo getById(Long yieldId) {
        return toVo(requireRecord(yieldId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> batchCreate(SfStaskYieldBatchCreateBo bo) {
        String tenantId = requireTenantId();
        List<Long> ids = new ArrayList<>(bo.getDetails().size());
        for (int index = 0; index < bo.getDetails().size(); index++) {
            SfStaskYieldRecord record = buildRecord(
                tenantId, bo.getHarvestDate(), bo.getDetails().get(index), index + 1);
            yieldRecordMapper.insert(record);
            ids.add(record.getYieldId());
        }
        return ids;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long yieldId, SfStaskYieldUpdateBo bo) {
        SfStaskYieldRecord current = requireRecord(yieldId);
        SfStaskYieldRecord replacement = buildRecord(
            current.getTenantId(), bo.getHarvestDate(), bo.getDetail(), 1);
        replacement.setYieldId(current.getYieldId());
        yieldRecordMapper.updateById(replacement);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Collection<Long> yieldIds) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>(yieldIds == null ? List.of() : yieldIds);
        ids.remove(null);
        if (ids.isEmpty()) {
            throw new ServiceException("请选择要删除的产量记录");
        }
        long count = yieldRecordMapper.selectCount(
            Wrappers.<SfStaskYieldRecord>lambdaQuery()
                .eq(SfStaskYieldRecord::getTenantId, requireTenantId())
                .in(SfStaskYieldRecord::getYieldId, ids));
        if (count != ids.size()) {
            throw new ServiceException("部分产量记录不存在或无权操作");
        }
        yieldRecordMapper.deleteBatchIds(ids);
    }

    @Override
    public List<SfStaskAnnualYieldRowVo> annualYield(LocalDate startDate, LocalDate endDate) {
        return yieldRecordMapper.selectAnnualYield(requireTenantId(), startDate, endDate);
    }

    private SfStaskYieldRecord buildRecord(
        String tenantId,
        LocalDate harvestDate,
        SfStaskYieldDetailBo detail,
        int rowNumber) {
        SfCropSpecies species = speciesMapper.selectOne(
            Wrappers.<SfCropSpecies>lambdaQuery()
                .eq(SfCropSpecies::getTenantId, tenantId)
                .eq(SfCropSpecies::getSpeciesId, detail.getSpeciesId())
                .eq(SfCropSpecies::getStatus, SystemConstants.NORMAL)
                .last("LIMIT 1"));
        if (species == null) {
            throw rowError(rowNumber, "物种不存在、已停用或无权使用");
        }
        SfCropVariety variety = varietyMapper.selectOne(
            Wrappers.<SfCropVariety>lambdaQuery()
                .eq(SfCropVariety::getTenantId, tenantId)
                .eq(SfCropVariety::getVarietyId, detail.getVarietyId())
                .eq(SfCropVariety::getStatus, SystemConstants.NORMAL)
                .last("LIMIT 1"));
        if (variety == null) {
            throw rowError(rowNumber, "品种不存在、已停用或无权使用");
        }
        if (!species.getSpeciesId().equals(variety.getSpeciesId())) {
            throw rowError(rowNumber, "所选品种不属于所选物种");
        }
        SfStaskYieldRecord record = new SfStaskYieldRecord();
        record.setTenantId(tenantId);
        record.setHarvestDate(harvestDate);
        record.setSpeciesId(species.getSpeciesId());
        record.setVarietyId(variety.getVarietyId());
        record.setSpeciesNameSnapshot(species.getSpeciesName());
        record.setVarietyNameSnapshot(variety.getVarietyName());
        record.setYieldKg(detail.getYieldKg().setScale(2, RoundingMode.UNNECESSARY));
        record.setDelFlag(SystemConstants.NORMAL);
        return record;
    }

    private ServiceException rowError(int rowNumber, String message) {
        return new ServiceException("第" + rowNumber + "条明细：" + message);
    }

    private SfStaskYieldRecord requireRecord(Long yieldId) {
        SfStaskYieldRecord record = yieldRecordMapper.selectOne(
            Wrappers.<SfStaskYieldRecord>lambdaQuery()
                .eq(SfStaskYieldRecord::getTenantId, requireTenantId())
                .eq(SfStaskYieldRecord::getYieldId, yieldId)
                .last("LIMIT 1"));
        if (record == null) {
            throw new ServiceException("产量记录不存在或无权操作");
        }
        return record;
    }

    private SfStaskYieldRecordVo toVo(SfStaskYieldRecord record) {
        SfStaskYieldRecordVo vo = new SfStaskYieldRecordVo();
        vo.setYieldId(record.getYieldId());
        vo.setHarvestDate(record.getHarvestDate());
        vo.setSpeciesId(record.getSpeciesId());
        vo.setVarietyId(record.getVarietyId());
        vo.setSpeciesName(record.getSpeciesNameSnapshot());
        vo.setVarietyName(record.getVarietyNameSnapshot());
        vo.setYieldKg(record.getYieldKg());
        vo.setCreateTime(record.getCreateTime());
        vo.setUpdateTime(record.getUpdateTime());
        return vo;
    }

    private String requireTenantId() {
        String tenantId = TenantHelper.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new ServiceException("缺少租户上下文");
        }
        return tenantId;
    }
}
