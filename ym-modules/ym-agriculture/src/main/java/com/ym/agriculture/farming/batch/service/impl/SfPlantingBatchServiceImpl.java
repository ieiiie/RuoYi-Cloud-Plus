package com.ym.agriculture.farming.batch.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.agriculture.farming.batch.dao.SfPlantingBatchLogMapper;
import com.ym.agriculture.farming.batch.dao.SfPlantingBatchMapper;
import com.ym.agriculture.farming.batch.model.bo.SfPlantingBatchBo;
import com.ym.agriculture.farming.batch.model.bo.SfDetectorInfoBo;
import com.ym.agriculture.farming.batch.model.bo.SfPlantingBatchTransitionBo;
import com.ym.agriculture.farming.batch.model.constants.PlantingBatchStatus;
import com.ym.agriculture.farming.batch.model.entity.SfPlantingBatch;
import com.ym.agriculture.farming.batch.model.entity.SfPlantingBatchLog;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchCalendarDayVo;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchCalendarItemVo;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchDetailVo;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchDashboardStatsVo;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchExportVo;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchVo;
import com.ym.agriculture.farming.batch.model.vo.SfDetectorInfoVo;
import com.ym.agriculture.farming.batch.service.ISfPlantingBatchService;
import com.ym.agriculture.farming.crop.dao.SfCropSpeciesMapper;
import com.ym.agriculture.farming.crop.dao.SfCropVarietyMapper;
import com.ym.agriculture.farming.crop.model.entity.SfCropVariety;
import com.ym.agriculture.farming.crop.model.vo.SfCropSpeciesVo;
import com.ym.agriculture.farming.crop.model.vo.SfCropVarietyVo;
import com.ym.agriculture.farming.field.dao.SfFieldMapper;
import com.ym.agriculture.farming.field.dao.SfFieldIotMapper;
import com.ym.agriculture.farming.field.model.entity.SfField;
import com.ym.agriculture.farming.field.model.entity.SfFieldIot;
import com.ym.agriculture.farming.field.model.vo.SfFieldVo;
import com.ym.agriculture.farming.field.support.SfFieldIotDeviceAccessor;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.iot.api.domain.vo.RemoteDeviceSummaryVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 种植批次核心服务实现。
 */
@Service
@RequiredArgsConstructor
public class SfPlantingBatchServiceImpl implements ISfPlantingBatchService {

    private final SfPlantingBatchMapper batchMapper;
    private final SfPlantingBatchLogMapper logMapper;
    private final SfFieldMapper fieldMapper;
    private final SfFieldIotMapper fieldIotMapper;
    private final SfFieldIotDeviceAccessor fieldIotDeviceAccessor;
    private final SfCropVarietyMapper varietyMapper;
    private final SfCropSpeciesMapper speciesMapper;

    @Override
    public List<SfPlantingBatchVo> queryList(SfPlantingBatchBo bo) {
        QueryIds ids = resolveQueryIds(bo);
        return enrich(batchMapper.selectBatchList(bo, ids.fieldIds(), ids.varietyIds()));
    }

    @Override
    public PageResult<SfPlantingBatchVo> queryPageList(SfPlantingBatchBo bo, PageQuery pageQuery) {
        QueryIds ids = resolveQueryIds(bo);
        Page<SfPlantingBatchVo> page = batchMapper.selectBatchPage(pageQuery.build(), bo, ids.fieldIds(), ids.varietyIds());
        return PageResult.build(enrich(page.getRecords()), page.getTotal());
    }

    @Override
    public PageResult<SfDetectorInfoVo> queryDetectorInfoPage(SfDetectorInfoBo bo, PageQuery pageQuery) {
        List<SfFieldVo> fields = bo.getFieldId() == null
            ? fieldMapper.selectEnabledByIds(null)
            : fieldMapper.selectEnabledByIds(List.of(bo.getFieldId()));
        if (fields.isEmpty()) {
            return PageResult.build(List.of(), 0L);
        }

        Set<Long> fieldIds = fields.stream().map(SfFieldVo::getFieldId).collect(Collectors.toSet());
        List<SfFieldIot> bindings = fieldIotMapper.selectNormalListByFieldIds(fieldIds);
        Map<String, Long> snToFieldId = bindings.stream()
            .filter(binding -> StringUtils.isNotBlank(binding.getDeviceSn()))
            .collect(Collectors.toMap(
                binding -> binding.getDeviceSn().trim(),
                SfFieldIot::getFieldId,
                (first, ignored) -> first,
                LinkedHashMap::new));
        if (snToFieldId.isEmpty()) {
            return PageResult.build(List.of(), 0L);
        }

        Map<Long, SfFieldVo> fieldMap = fields.stream()
            .collect(Collectors.toMap(SfFieldVo::getFieldId, Function.identity()));
        Map<Long, SfPlantingBatchVo> activeBatchMap = mapActiveVoByFieldIds(fieldIds).entrySet().stream()
            .filter(entry -> !entry.getValue().isEmpty())
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getFirst()));

        List<SfDetectorInfoVo> rows = new ArrayList<>();
        for (RemoteDeviceSummaryVo device : fieldIotDeviceAccessor.listByCodes(snToFieldId.keySet())) {
            if (StringUtils.isBlank(device.getDeviceCode())) {
                continue;
            }
            Long fieldId = snToFieldId.get(device.getDeviceCode().trim());
            if (fieldId == null) {
                continue;
            }
            SfDetectorInfoVo row = new SfDetectorInfoVo();
            row.setDeviceId(device.getDeviceId());
            row.setDeviceCategory(device.getDeviceCategory());
            row.setDeviceCode(device.getDeviceCode());
            row.setDeviceName(device.getDeviceName());
            row.setFieldId(fieldId);
            SfFieldVo field = fieldMap.get(fieldId);
            row.setFieldName(field == null ? null : field.getFieldName());
            SfPlantingBatchVo batch = activeBatchMap.get(fieldId);
            if (batch != null) {
                row.setVarietyId(batch.getVarietyId());
                row.setVarietyName(batch.getVarietyName());
            }
            row.setLastDataDate(device.getLastReportTime());
            rows.add(row);
        }

        Date begin = startOfDay(bo.getLastDataDateBegin());
        Date end = endOfDay(bo.getLastDataDateEnd());
        rows = rows.stream()
            .filter(row -> StringUtils.isBlank(bo.getDeviceCategory())
                || Objects.equals(bo.getDeviceCategory(), row.getDeviceCategory()))
            .filter(row -> StringUtils.isBlank(bo.getDeviceName())
                || StringUtils.containsIgnoreCase(row.getDeviceName(), bo.getDeviceName()))
            .filter(row -> bo.getVarietyId() == null || Objects.equals(bo.getVarietyId(), row.getVarietyId()))
            .filter(row -> begin == null || row.getLastDataDate() != null && !row.getLastDataDate().before(begin))
            .filter(row -> end == null || row.getLastDataDate() != null && !row.getLastDataDate().after(end))
            .toList();

        Page<?> page = pageQuery.build();
        long offset = Math.max(0L, (page.getCurrent() - 1L) * page.getSize());
        int fromIndex = Math.toIntExact(Math.min(offset, rows.size()));
        int toIndex = Math.min(fromIndex + Math.toIntExact(page.getSize()), rows.size());
        return PageResult.build(rows.subList(fromIndex, toIndex), (long) rows.size());
    }

    @Override
    public SfPlantingBatchVo queryById(Long batchId) {
        SfPlantingBatchVo batch = batchMapper.selectVoById(batchId);
        if (batch == null) {
            throw new ServiceException("种植批次不存在");
        }
        return enrich(List.of(batch)).getFirst();
    }

    @Override
    public SfPlantingBatchDashboardStatsVo statsForDashboard() {
        SfPlantingBatchDashboardStatsVo stats = new SfPlantingBatchDashboardStatsVo();
        for (SfPlantingBatchVo row : queryList(new SfPlantingBatchBo())) {
            if (PlantingBatchStatus.PLANNING.equals(row.getBatchStatus())) {
                stats.setPlanningCount(stats.getPlanningCount() + 1);
            } else if (PlantingBatchStatus.PLANTING.equals(row.getBatchStatus())) {
                stats.setPlantingCount(stats.getPlantingCount() + 1);
            } else if (PlantingBatchStatus.GROWING.equals(row.getBatchStatus())) {
                stats.setGrowingCount(stats.getGrowingCount() + 1);
            } else if (PlantingBatchStatus.HARVESTING.equals(row.getBatchStatus())) {
                stats.setHarvestingCount(stats.getHarvestingCount() + 1);
            } else if (PlantingBatchStatus.FINISHED.equals(row.getBatchStatus())) {
                stats.setFinishedCount(stats.getFinishedCount() + 1);
            } else if (PlantingBatchStatus.FAILED.equals(row.getBatchStatus())) {
                stats.setFailedCount(stats.getFailedCount() + 1);
            }
        }
        return stats;
    }

    @Override
    public SfPlantingBatchDetailVo queryDetailById(Long batchId) {
        SfPlantingBatchVo batch = queryById(batchId);
        SfPlantingBatchDetailVo detail = new SfPlantingBatchDetailVo();
        org.springframework.beans.BeanUtils.copyProperties(batch, detail);
        detail.setLogs(logMapper.selectByBatchId(batchId, batch.getTenantId()));
        detail.setAnalysisHints(Collections.emptyList());
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(SfPlantingBatchBo bo) {
        normalize(bo);
        validateReferences(bo.getFieldId(), bo.getVarietyId());
        validateDates(bo.getSowingDate(), bo.getExpectedHarvestDate(), bo.getActualHarvestDate());
        if (batchMapper.existsByBatchCode(bo.getBatchCode(), null)) {
            throw new ServiceException("批次编号已存在");
        }
        if (batchMapper.existsActiveByFieldId(bo.getFieldId(), null)) {
            throw new ServiceException("该地块已有进行中的种植批次");
        }
        if (bo.getExpectedHarvestDate() == null) {
            bo.setExpectedHarvestDate(toDate(suggestHarvestDate(bo.getVarietyId(), toLocalDate(bo.getSowingDate()))));
        }
        SfPlantingBatch batch = MapstructUtils.convert(bo, SfPlantingBatch.class);
        batch.setBatchStatus(PlantingBatchStatus.PLANNING);
        batch.setStatusTime(new Date());
        batch.setDelFlag(SystemConstants.NORMAL);
        boolean inserted = batchMapper.insert(batch) > 0;
        if (inserted) {
            bo.setBatchId(batch.getBatchId());
            insertLog(batch, null, PlantingBatchStatus.PLANNING, "创建种植批次");
        }
        return inserted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(SfPlantingBatchBo bo) {
        SfPlantingBatch current = requireEntity(bo.getBatchId());
        if (!PlantingBatchStatus.isActive(current.getBatchStatus())) {
            throw new ServiceException("已结束或失败的种植批次不能修改");
        }
        mergeMissing(bo, current);
        normalize(bo);
        validateReferences(bo.getFieldId(), bo.getVarietyId());
        validateDates(bo.getSowingDate(), bo.getExpectedHarvestDate(), bo.getActualHarvestDate());
        if (batchMapper.existsByBatchCode(bo.getBatchCode(), bo.getBatchId())) {
            throw new ServiceException("批次编号已存在");
        }
        if (batchMapper.existsActiveByFieldId(bo.getFieldId(), bo.getBatchId())) {
            throw new ServiceException("该地块已有其他进行中的种植批次");
        }
        SfPlantingBatch update = MapstructUtils.convert(bo, SfPlantingBatch.class);
        update.setBatchStatus(current.getBatchStatus());
        update.setStatusTime(current.getStatusTime());
        return batchMapper.updateById(update) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean transition(Long batchId, SfPlantingBatchTransitionBo bo) {
        SfPlantingBatch current = requireEntity(batchId);
        String target = bo.getToStatus().trim().toUpperCase();
        if (!PlantingBatchStatus.canTransition(current.getBatchStatus(), target)) {
            throw new ServiceException("不允许从" + current.getBatchStatus() + "流转到" + target);
        }
        Date actualHarvestDate = bo.getActualHarvestDate();
        if (PlantingBatchStatus.FINISHED.equals(target) && actualHarvestDate == null) {
            actualHarvestDate = new Date();
        }
        validateDates(current.getSowingDate(), current.getExpectedHarvestDate(), actualHarvestDate);
        SfPlantingBatch update = new SfPlantingBatch();
        update.setBatchId(batchId);
        update.setBatchStatus(target);
        update.setStatusTime(new Date());
        update.setActualHarvestDate(actualHarvestDate);
        boolean changed = batchMapper.updateById(update) > 0;
        if (changed) {
            insertLog(current, current.getBatchStatus(), target, bo.getRemark());
        }
        return changed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> batchIds) {
        if (batchIds == null || batchIds.isEmpty()) {
            return false;
        }
        List<SfPlantingBatch> rows = batchMapper.selectBatchIds(batchIds);
        if (rows.size() != batchIds.stream().filter(Objects::nonNull).distinct().count()) {
            throw new ServiceException("部分种植批次不存在");
        }
        if (rows.stream().anyMatch(row -> !PlantingBatchStatus.PLANNING.equals(row.getBatchStatus()))) {
            throw new ServiceException("仅计划中的种植批次允许删除");
        }
        return batchMapper.deleteBatchIds(batchIds) > 0;
    }

    @Override
    public List<SfPlantingBatchCalendarDayVo> queryCalendar(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        Date begin = toDate(yearMonth.atDay(1));
        Date end = Date.from(yearMonth.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).minusNanos(1).toInstant());
        List<SfPlantingBatchVo> batches = enrich(batchMapper.selectCalendar(begin, end));
        Map<LocalDate, Map<Long, SfPlantingBatchCalendarItemVo>> days = new LinkedHashMap<>();
        for (SfPlantingBatchVo batch : batches) {
            addCalendarItem(days, yearMonth, batch.getSowingDate(), batch);
            addCalendarItem(days, yearMonth, batch.getExpectedHarvestDate(), batch);
        }
        return days.entrySet().stream().map(entry -> {
            SfPlantingBatchCalendarDayVo day = new SfPlantingBatchCalendarDayVo();
            day.setDate(entry.getKey().toString());
            day.setBatches(new ArrayList<>(entry.getValue().values()));
            return day;
        }).toList();
    }

    @Override
    public int suggestCroppingIndex(Long fieldId, LocalDate sowingDate) {
        requireField(fieldId);
        return Math.toIntExact(batchMapper.countByFieldAndYear(fieldId, sowingDate.getYear()) + 1);
    }

    @Override
    public LocalDate suggestHarvestDate(Long varietyId, LocalDate sowingDate) {
        SfCropVariety variety = requireVariety(varietyId);
        return variety.getGrowthCycleDays() == null ? null : sowingDate.plusDays(variety.getGrowthCycleDays());
    }

    @Override
    public List<SfPlantingBatchExportVo> queryExportList(SfPlantingBatchBo bo) {
        return queryList(bo).stream().map(batch -> {
            SfPlantingBatchExportVo row = new SfPlantingBatchExportVo();
            org.springframework.beans.BeanUtils.copyProperties(batch, row);
            return row;
        }).toList();
    }

    @Override
    public List<SfPlantingBatchVo> listActiveByFieldIds(Collection<Long> fieldIds) {
        Collection<Long> queryFieldIds = fieldIds == null || fieldIds.isEmpty() ? null : fieldIds;
        return enrich(batchMapper.selectActiveByFieldIds(queryFieldIds)).stream()
            .filter(batch -> batch.getFieldId() != null)
            .collect(Collectors.toMap(
                SfPlantingBatchVo::getFieldId,
                Function.identity(),
                (latest, ignored) -> latest,
                LinkedHashMap::new
            ))
            .values()
            .stream()
            .toList();
    }

    @Override
    public Map<Long, List<SfPlantingBatchVo>> mapActiveVoByFieldIds(Collection<Long> fieldIds) {
        if (fieldIds == null || fieldIds.isEmpty()) {
            return Map.of();
        }
        return enrich(batchMapper.selectActiveByFieldIds(fieldIds)).stream()
            .filter(batch -> batch.getFieldId() != null)
            .sorted(java.util.Comparator.comparing(SfPlantingBatchVo::getBatchStatus,
                PlantingBatchStatus.ACTIVE_PRIORITY).thenComparing(SfPlantingBatchVo::getBatchId,
                java.util.Comparator.reverseOrder()))
            .collect(Collectors.groupingBy(SfPlantingBatchVo::getFieldId, LinkedHashMap::new, Collectors.toList()));
    }

    private QueryIds resolveQueryIds(SfPlantingBatchBo bo) {
        Collection<Long> fieldIds = null;
        if (StringUtils.isNotBlank(bo.getFieldName())) {
            fieldIds = fieldMapper.selectList(Wrappers.<SfField>lambdaQuery()
                .like(SfField::getFieldName, bo.getFieldName())
                .eq(SfField::getDelFlag, SystemConstants.NORMAL)).stream().map(SfField::getFieldId).toList();
        }
        Collection<Long> varietyIds = null;
        if (bo.getSpeciesId() != null || StringUtils.isNotBlank(bo.getVarietyName())) {
            varietyIds = varietyMapper.selectList(Wrappers.<SfCropVariety>lambdaQuery()
                .eq(bo.getSpeciesId() != null, SfCropVariety::getSpeciesId, bo.getSpeciesId())
                .like(StringUtils.isNotBlank(bo.getVarietyName()), SfCropVariety::getVarietyName, bo.getVarietyName())
                .eq(SfCropVariety::getDelFlag, SystemConstants.NORMAL)).stream()
                .map(SfCropVariety::getVarietyId).toList();
        }
        return new QueryIds(fieldIds, varietyIds);
    }

    private List<SfPlantingBatchVo> enrich(List<SfPlantingBatchVo> batches) {
        if (batches.isEmpty()) {
            return batches;
        }
        Map<Long, SfFieldVo> fields = fieldMapper.selectVoByFieldIds(
                batches.stream().map(SfPlantingBatchVo::getFieldId).filter(Objects::nonNull).distinct().toList())
            .stream().collect(Collectors.toMap(SfFieldVo::getFieldId, Function.identity()));
        Map<Long, SfCropVarietyVo> varieties = varietyMapper.selectVoByIds(
                batches.stream().map(SfPlantingBatchVo::getVarietyId).filter(Objects::nonNull).distinct().toList())
            .stream().collect(Collectors.toMap(SfCropVarietyVo::getVarietyId, Function.identity()));
        Map<Long, SfCropSpeciesVo> species = speciesMapper.selectVoListByIds(varieties.values().stream()
                .map(SfCropVarietyVo::getSpeciesId).filter(Objects::nonNull).distinct().toList())
            .stream().collect(Collectors.toMap(SfCropSpeciesVo::getSpeciesId, Function.identity()));
        batches.forEach(batch -> {
            SfFieldVo field = fields.get(batch.getFieldId());
            SfCropVarietyVo variety = varieties.get(batch.getVarietyId());
            if (field != null) {
                batch.setFieldName(field.getFieldName());
            }
            if (variety != null) {
                batch.setVarietyName(variety.getVarietyName());
                batch.setSpeciesId(variety.getSpeciesId());
                SfCropSpeciesVo cropSpecies = species.get(variety.getSpeciesId());
                if (cropSpecies != null) {
                    batch.setSpeciesName(cropSpecies.getSpeciesName());
                    batch.setSpeciesImageUrl(cropSpecies.getMapIconUrl());
                }
            }
        });
        return batches;
    }

    private void validateReferences(Long fieldId, Long varietyId) {
        requireField(fieldId);
        SfCropVariety variety = requireVariety(varietyId);
        if (!SystemConstants.NORMAL.equals(variety.getStatus())) {
            throw new ServiceException("作物品种已停用");
        }
    }

    private SfField requireField(Long fieldId) {
        SfField field = fieldMapper.selectById(fieldId);
        if (field == null || !SystemConstants.NORMAL.equals(field.getDelFlag())) {
            throw new ServiceException("地块不存在");
        }
        if (!SystemConstants.NORMAL.equals(field.getStatus())) {
            throw new ServiceException("地块已停用");
        }
        return field;
    }

    private SfCropVariety requireVariety(Long varietyId) {
        SfCropVariety variety = varietyMapper.selectById(varietyId);
        if (variety == null || !SystemConstants.NORMAL.equals(variety.getDelFlag())) {
            throw new ServiceException("作物品种不存在");
        }
        return variety;
    }

    private SfPlantingBatch requireEntity(Long batchId) {
        SfPlantingBatch batch = batchMapper.selectById(batchId);
        if (batch == null || !SystemConstants.NORMAL.equals(batch.getDelFlag())) {
            throw new ServiceException("种植批次不存在");
        }
        return batch;
    }

    private void insertLog(SfPlantingBatch batch, String fromStatus, String toStatus, String remark) {
        SfPlantingBatchLog log = new SfPlantingBatchLog();
        log.setTenantId(StringUtils.defaultIfBlank(batch.getTenantId(), TenantHelper.getTenantId()));
        log.setBatchId(batch.getBatchId());
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperateBy(LoginHelper.getUserId());
        log.setOperateTime(new Date());
        log.setRemark(remark);
        logMapper.insert(log);
    }

    private static void normalize(SfPlantingBatchBo bo) {
        if (StringUtils.isNotBlank(bo.getBatchCode())) {
            bo.setBatchCode(bo.getBatchCode().trim());
        }
    }

    private static void validateDates(Date sowing, Date expected, Date actual) {
        if (sowing == null) {
            throw new ServiceException("播种日期不能为空");
        }
        if (expected != null && expected.before(sowing)) {
            throw new ServiceException("预计采收日期不能早于播种日期");
        }
        if (actual != null && actual.before(sowing)) {
            throw new ServiceException("实际采收日期不能早于播种日期");
        }
    }

    private static void mergeMissing(SfPlantingBatchBo bo, SfPlantingBatch current) {
        if (bo.getFieldId() == null) bo.setFieldId(current.getFieldId());
        if (bo.getVarietyId() == null) bo.setVarietyId(current.getVarietyId());
        if (StringUtils.isBlank(bo.getBatchCode())) bo.setBatchCode(current.getBatchCode());
        if (bo.getCroppingIndex() == null) bo.setCroppingIndex(current.getCroppingIndex());
        if (bo.getSowingDate() == null) bo.setSowingDate(current.getSowingDate());
        if (bo.getExpectedHarvestDate() == null) bo.setExpectedHarvestDate(current.getExpectedHarvestDate());
        if (bo.getActualHarvestDate() == null) bo.setActualHarvestDate(current.getActualHarvestDate());
    }

    private static void addCalendarItem(Map<LocalDate, Map<Long, SfPlantingBatchCalendarItemVo>> days,
                                        YearMonth yearMonth, Date date, SfPlantingBatchVo batch) {
        if (date == null) return;
        LocalDate localDate = toLocalDate(date);
        if (!YearMonth.from(localDate).equals(yearMonth)) return;
        SfPlantingBatchCalendarItemVo item = new SfPlantingBatchCalendarItemVo();
        org.springframework.beans.BeanUtils.copyProperties(batch, item);
        days.computeIfAbsent(localDate, ignored -> new LinkedHashMap<>()).put(batch.getBatchId(), item);
    }

    private static LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static Date toDate(LocalDate date) {
        return date == null ? null : Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static Date startOfDay(Date date) {
        return date == null ? null : toDate(toLocalDate(date));
    }

    private static Date endOfDay(Date date) {
        if (date == null) {
            return null;
        }
        return Date.from(toLocalDate(date).plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusNanos(1).toInstant());
    }

    private record QueryIds(Collection<Long> fieldIds, Collection<Long> varietyIds) {
    }
}
