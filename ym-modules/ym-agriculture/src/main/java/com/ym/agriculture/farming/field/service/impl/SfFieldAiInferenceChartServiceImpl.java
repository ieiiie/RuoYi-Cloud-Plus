package com.ym.agriculture.farming.field.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farming.algback.dao.SfAiInferenceLogMapper;
import com.ym.agriculture.farming.field.model.dto.FieldInferenceCountAggDto;
import com.ym.agriculture.farming.field.model.dto.FieldInferenceLabelCountDto;
import com.ym.agriculture.farming.field.model.dto.FieldInferenceLabelStatRowDto;
import com.ym.agriculture.farming.field.model.dto.FieldInferenceScoreDistributionAggDto;
import com.ym.agriculture.farming.field.model.dto.FieldInferenceTimeseriesRowDto;
import com.ym.agriculture.farming.field.model.vo.SfFieldAiInferenceLabelStatVo;
import com.ym.agriculture.farming.field.model.vo.SfFieldAiInferenceScoreBucketVo;
import com.ym.agriculture.farming.field.model.vo.SfFieldAiInferencePageVo;
import com.ym.agriculture.farming.field.model.vo.SfFieldAiInferenceSummaryVo;
import com.ym.agriculture.farming.field.model.vo.SfFieldAiInferenceTimeseriesPointVo;
import com.ym.agriculture.farming.field.model.vo.SfFieldVo;
import com.ym.agriculture.farming.field.service.ISfFieldAiInferenceChartService;
import com.ym.agriculture.farming.field.service.ISfFieldService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 地块推理图表：先 {@link ISfFieldService#queryById(Long)} 做权限与存在性校验，再按地块租户查 JOIN 流水。
 */
@RequiredArgsConstructor
@Service
public class SfFieldAiInferenceChartServiceImpl implements ISfFieldAiInferenceChartService {

    private static final ZoneId ZONE = ZoneId.systemDefault();

    private final ISfFieldService fieldService;
    private final SfAiInferenceLogMapper inferenceLogMapper;

    @Override
    public SfFieldAiInferenceSummaryVo querySummary(Long fieldId, Long plantingBatchId, Date startTime, Date endTime) {
        FieldInferenceScope scope = resolveScope(fieldId, startTime, endTime);

        SfFieldAiInferenceSummaryVo vo = new SfFieldAiInferenceSummaryVo();
        Long total = inferenceLogMapper.countFieldInferenceByField(scope.fieldId(), scope.tenantId(), plantingBatchId);
        vo.setTotalCount(longOrZero(total));

        FieldInferenceCountAggDto agg = inferenceLogMapper.selectFieldInferenceRangeAgg(
            scope.fieldId(), scope.tenantId(), plantingBatchId, scope.startMs(), scope.endMs());
        if (agg != null) {
            vo.setRangeCount(longOrZero(agg.getCnt()));
            vo.setDistinctTaskNoCount(longOrZero(agg.getDistinctTaskNo()));
            vo.setDistinctUavJobIdCount(longOrZero(agg.getDistinctUavJobId()));
            vo.setScoreParsedCount(longOrZero(agg.getScoreParsedCount()));
            vo.setRangeAvgScore(agg.getRangeAvgScore());
            vo.setRangeMaxScore(agg.getRangeMaxScore());
            vo.setRangeMinScore(agg.getRangeMinScore());
        } else {
            vo.setRangeCount(0L);
            vo.setDistinctTaskNoCount(0L);
            vo.setDistinctUavJobIdCount(0L);
            vo.setScoreParsedCount(0L);
            vo.setRangeAvgScore(null);
            vo.setRangeMaxScore(null);
            vo.setRangeMinScore(null);
        }

        List<FieldInferenceLabelCountDto> rows = inferenceLogMapper.selectFieldInferenceLabelCountsForSummary(
            scope.fieldId(), scope.tenantId(), plantingBatchId, scope.startMs(), scope.endMs());
        Map<String, Long> labelCounts = new LinkedHashMap<>();
        if (rows != null) {
            for (FieldInferenceLabelCountDto r : rows) {
                if (r == null || StringUtils.isBlank(r.getLabel())) {
                    continue;
                }
                labelCounts.put(r.getLabel(), longOrZero(r.getCnt()));
            }
        }
        vo.setLabelCounts(labelCounts);
        return vo;
    }

    @Override
    public List<SfFieldAiInferenceTimeseriesPointVo> queryTimeseries(
        Long fieldId, Long plantingBatchId, String granularity, Date startTime, Date endTime) {
        FieldInferenceScope scope = resolveScope(fieldId, startTime, endTime);
        String g = normalizeGranularity(granularity);

        List<FieldInferenceTimeseriesRowDto> rows = inferenceLogMapper.selectFieldInferenceTimeseries(
            scope.fieldId(), scope.tenantId(), plantingBatchId, scope.startMs(), scope.endMs(), g);
        List<SfFieldAiInferenceTimeseriesPointVo> list = new ArrayList<>();
        if (rows == null) {
            return list;
        }
        for (FieldInferenceTimeseriesRowDto r : rows) {
            if (r == null) {
                continue;
            }
            SfFieldAiInferenceTimeseriesPointVo p = new SfFieldAiInferenceTimeseriesPointVo();
            p.setBucketStart(r.getBucketStart());
            p.setCount(longOrZero(r.getCnt()));
            p.setAvgScore(r.getAvgScore());
            list.add(p);
        }
        return list;
    }

    @Override
    public List<SfFieldAiInferenceLabelStatVo> queryByLabel(Long fieldId, Long plantingBatchId, Date startTime, Date endTime) {
        FieldInferenceScope scope = resolveScope(fieldId, startTime, endTime);

        List<FieldInferenceLabelStatRowDto> rows = inferenceLogMapper.selectFieldInferenceLabelStats(
            scope.fieldId(), scope.tenantId(), plantingBatchId, scope.startMs(), scope.endMs());
        List<SfFieldAiInferenceLabelStatVo> list = new ArrayList<>();
        if (rows == null) {
            return list;
        }
        for (FieldInferenceLabelStatRowDto r : rows) {
            if (r == null) {
                continue;
            }
            SfFieldAiInferenceLabelStatVo v = new SfFieldAiInferenceLabelStatVo();
            v.setLabel(r.getLabel());
            v.setCount(longOrZero(r.getCnt()));
            v.setAvgScore(r.getAvgScore());
            v.setMaxScore(r.getMaxScore());
            list.add(v);
        }
        return list;
    }

    @Override
    public List<SfFieldAiInferenceScoreBucketVo> queryScoreDistribution(
        Long fieldId, Long plantingBatchId, Date startTime, Date endTime) {
        FieldInferenceScope scope = resolveScope(fieldId, startTime, endTime);
        FieldInferenceScoreDistributionAggDto row = inferenceLogMapper.selectFieldInferenceScoreDistribution(
            scope.fieldId(), scope.tenantId(), plantingBatchId, scope.startMs(), scope.endMs());
        return toScoreBucketList(row);
    }

    @Override
    public PageResult<SfFieldAiInferencePageVo> queryPage(
        Long fieldId,
        Long plantingBatchId,
        Date startTime,
        Date endTime,
        String taskNo,
        String modelNo,
        String labelKeyword,
        PageQuery pageQuery) {
        FieldInferenceScope scope = resolveScope(fieldId, startTime, endTime);

        Page<SfFieldAiInferencePageVo> page = pageQuery.build();
        Page<SfFieldAiInferencePageVo> result = inferenceLogMapper.selectFieldInferencePage(
            page,
            scope.fieldId(),
            scope.tenantId(),
            plantingBatchId,
            scope.startMs(),
            scope.endMs(),
            StringUtils.trimToNull(taskNo),
            StringUtils.trimToNull(modelNo),
            StringUtils.trimToNull(labelKeyword));
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(result);
    }

    /** 已通过地块校验后的查询上下文：地块 ID、租户、可选日粒度时间窗（毫秒，含起止日全天）。 */
    private record FieldInferenceScope(long fieldId, String tenantId, Long startMs, Long endMs) {}

    private FieldInferenceScope resolveScope(Long fieldId, Date startTime, Date endTime) {
        SfFieldVo field = requireAccessibleField(fieldId);
        String tenantId = trimTenantId(field);
        Long startMs = optionalDayStartMillis(startTime);
        Long endMs = optionalDayEndMillis(endTime);
        validateDayRange(startMs, endMs);
        return new FieldInferenceScope(fieldId, tenantId, startMs, endMs);
    }

    private static String normalizeGranularity(String granularity) {
        String g = StringUtils.isBlank(granularity) ? "day" : granularity.trim().toLowerCase();
        if (!"day".equals(g) && !"hour".equals(g)) {
            throw new ServiceException("granularity 仅支持 day 或 hour");
        }
        return g;
    }

    private static void validateDayRange(Long startMs, Long endMs) {
        if (startMs != null && endMs != null && startMs > endMs) {
            throw new ServiceException("startTime 不能晚于 endTime");
        }
    }

    private SfFieldVo requireAccessibleField(Long fieldId) {
        if (fieldId == null) {
            throw new ServiceException("地块ID不能为空");
        }
        SfFieldVo field = fieldService.queryById(fieldId);
        if (field == null) {
            throw new ServiceException("地块不存在");
        }
        return field;
    }

    private static String trimTenantId(SfFieldVo field) {
        String t = field.getTenantId();
        if (t == null || t.isBlank()) {
            throw new ServiceException("地块租户信息缺失，无法查询推理流水");
        }
        return t.trim();
    }

    /** 未传表示不按该端过滤，返回 null。 */
    private static Long optionalDayStartMillis(Date day) {
        if (day == null) {
            return null;
        }
        return day.toInstant().atZone(ZONE).toLocalDate().atStartOfDay(ZONE).toInstant().toEpochMilli();
    }

    /** 未传表示不按该端过滤，返回 null。 */
    private static Long optionalDayEndMillis(Date day) {
        if (day == null) {
            return null;
        }
        return day.toInstant().atZone(ZONE).toLocalDate().atTime(LocalTime.MAX).atZone(ZONE).toInstant().toEpochMilli();
    }

    private static long longOrZero(Long n) {
        return n == null ? 0L : n;
    }

    private static List<SfFieldAiInferenceScoreBucketVo> toScoreBucketList(FieldInferenceScoreDistributionAggDto row) {
        List<SfFieldAiInferenceScoreBucketVo> list = new ArrayList<>(6);
        if (row == null) {
            row = new FieldInferenceScoreDistributionAggDto();
        }
        list.add(new SfFieldAiInferenceScoreBucketVo(0, "[0.0, 0.2)", 0.0, 0.2, longOrZero(row.getB00())));
        list.add(new SfFieldAiInferenceScoreBucketVo(1, "[0.2, 0.4)", 0.2, 0.4, longOrZero(row.getB02())));
        list.add(new SfFieldAiInferenceScoreBucketVo(2, "[0.4, 0.6)", 0.4, 0.6, longOrZero(row.getB04())));
        list.add(new SfFieldAiInferenceScoreBucketVo(3, "[0.6, 0.8)", 0.6, 0.8, longOrZero(row.getB06())));
        list.add(new SfFieldAiInferenceScoreBucketVo(4, "[0.8, 1.0]", 0.8, 1.0, longOrZero(row.getB08())));
        list.add(new SfFieldAiInferenceScoreBucketVo(5, "其他（可解析且不在 [0,1]）", 0.0, 0.0, longOrZero(row.getBOther())));
        return list;
    }
}
