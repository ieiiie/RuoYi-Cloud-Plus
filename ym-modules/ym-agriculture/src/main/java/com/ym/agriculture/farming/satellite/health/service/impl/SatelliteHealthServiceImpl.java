package com.ym.agriculture.farming.satellite.health.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farming.batch.dao.SfPlantingBatchMapper;
import com.ym.agriculture.farming.field.dao.SfFieldMapper;
import com.ym.agriculture.farming.field.model.entity.SfField;
import com.ym.agriculture.farming.satellite.health.dao.SatelliteHealthMapper;
import com.ym.agriculture.farming.satellite.health.model.SatelliteHealthResultRow;
import com.ym.agriculture.farming.satellite.health.model.vo.SatelliteHealthVo;
import com.ym.agriculture.farming.satellite.health.service.ISatelliteHealthService;
import com.ym.agriculture.farming.satellite.health.support.SatelliteHealthAreaParser;
import com.ym.agriculture.farming.satellite.health.support.SatelliteHealthWeights;
import com.ym.agriculture.farming.satellite.service.impl.SatelliteTaskTypeDict;
import com.ym.agriculture.farming.satellite.support.SatelliteTaskTypeNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 卫星综合健康评分读模型实现，不影响既有遥感任务及大屏聚合链路。 */
@Service
@RequiredArgsConstructor
public class SatelliteHealthServiceImpl implements ISatelliteHealthService {

    private static final List<String> QUERY_TYPES = List.of(
        "growth", "chlorophyll", "nitrogen", "droughtlevel", "soilmoisture",
        SatelliteTaskTypeNormalizer.LEGACY_SOIL_MOISTURE, "health", "seedlinggrowth", "cloudcover");
    private static final List<String> DISPLAY_TYPES = List.of(
        "growth", "soilmoisture", "nitrogen", "seedlinggrowth", "health", "chlorophyll", "droughtlevel");
    private static final String SCORED = "SCORED";
    private static final String INSUFFICIENT_DATA = "INSUFFICIENT_DATA";

    private final SatelliteHealthMapper healthMapper;
    private final SfFieldMapper fieldMapper;
    private final SfPlantingBatchMapper batchMapper;
    private final SatelliteHealthAreaParser areaParser;
    private final SatelliteHealthWeights weights;
    private final SatelliteTaskTypeDict typeDict;

    @Override
    public PageResult<SatelliteHealthVo.Field> queryFields(String keyword, PageQuery pageQuery) {
        String normalizedKeyword = StrUtil.isBlank(keyword) ? null : keyword.trim();
        Page<SatelliteHealthVo.Field> page = healthMapper.selectFieldPage(pageQuery.build(), TenantHelper.getTenantId(), normalizedKeyword);
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(page);
    }

    @Override
    public long getFieldPage(Long fieldId, int pageSize) {
        requireEnabledField(fieldId);
        Long position = healthMapper.selectFieldPosition(TenantHelper.getTenantId(), fieldId);
        if (position == null) {
            throw new ServiceException("地块不存在、已停用或无权访问");
        }
        return (position - 1) / normalizePageSize(pageSize) + 1;
    }

    @Override
    public SatelliteHealthVo.Dates queryDates(Long fieldId, String beforeDate, int limit) {
        requireEnabledField(fieldId);
        SatelliteHealthVo.Dates response = new SatelliteHealthVo.Dates();
        Long batchId = batchMapper.selectActiveBatchIdByFieldId(fieldId);
        response.setActiveBatchId(batchId);
        response.setDates(List.of());
        if (batchId == null) {
            return response;
        }
        String cursor = validDate(beforeDate) ? beforeDate : null;
        int targetSize = Math.max(1, Math.min(limit, 24));
        List<SatelliteHealthVo.DateCard> cards = new ArrayList<>();
        String scanCursor = cursor;
        boolean exhausted = false;
        // 查询目标数量之外的第一个有效日期，用于准确判断是否还有更早历史。
        while (cards.size() <= targetSize && !exhausted) {
            List<String> candidates = healthMapper.selectCandidateDates(TenantHelper.getTenantId(), fieldId, batchId, scanCursor, 120);
            if (candidates.isEmpty()) {
                exhausted = true;
                break;
            }
            List<String> validDates = candidates.stream().filter(this::validDate).distinct().toList();
            if (!validDates.isEmpty()) {
                Map<String, Map<String, SatelliteHealthResultRow>> results = latestByDateAndType(fieldId, batchId, validDates);
                for (String date : validDates) {
                    Map<String, SatelliteHealthResultRow> byType = results.getOrDefault(date, Map.of());
                    if (countValidMetrics(byType) == 0) {
                        continue;
                    }
                    cards.add(toDateCard(date, byType));
                    if (cards.size() > targetSize) {
                        break;
                    }
                }
            }
            if (cards.size() > targetSize) {
                break;
            }
            // 只有整批候选都已处理完，才推进扫描游标；命中前瞻日期时游标由返回页最早日期决定。
            scanCursor = candidates.get(candidates.size() - 1);
            exhausted = candidates.size() < 120;
        }
        boolean hasMore = cards.size() > targetSize;
        if (hasMore) {
            cards = new ArrayList<>(cards.subList(0, targetSize));
        }
        cards.sort(Comparator.comparing(SatelliteHealthVo.DateCard::getImageDate));
        response.setDates(cards);
        response.setHasMore(hasMore);
        response.setNextBeforeDate(hasMore && !cards.isEmpty() ? cards.get(0).getImageDate() : null);
        return response;
    }

    @Override
    public SatelliteHealthVo.Detail queryDetail(Long fieldId, String imageDate) {
        SfField field = requireEnabledField(fieldId);
        if (!validDate(imageDate)) {
            throw new ServiceException("影像日期格式无效");
        }
        Long batchId = batchMapper.selectActiveBatchIdByFieldId(fieldId);
        SatelliteHealthVo.Detail detail = new SatelliteHealthVo.Detail();
        detail.setActiveBatchId(batchId);
        detail.setImageDate(imageDate);
        detail.setTotalAreaMu(validFieldArea(field.getAreaMu()) ? field.getAreaMu() : null);
        if (batchId == null) {
            return insufficient(detail, List.of());
        }
        Map<String, SatelliteHealthResultRow> rows = latestByDateAndType(fieldId, batchId, List.of(imageDate)).getOrDefault(imageDate, Map.of());
        List<SatelliteHealthVo.Metric> metrics = toMetrics(rows);
        if (metrics.isEmpty()) {
            return insufficient(detail, metrics);
        }
        detail.setMetrics(metrics);
        populateLowLevelAndFocus(detail, metrics);
        if (metrics.size() < 4) {
            return insufficient(detail, metrics);
        }
        calculateScore(detail, metrics);
        return detail;
    }

    private SfField requireEnabledField(Long fieldId) {
        if (fieldId == null) {
            throw new ServiceException("地块ID不能为空");
        }
        SatelliteHealthVo.Field field = healthMapper.selectEnabledField(TenantHelper.getTenantId(), fieldId);
        if (field == null) {
            throw new ServiceException("地块不存在、已停用或无权访问");
        }
        SfField entity = fieldMapper.selectById(fieldId);
        if (entity == null) {
            throw new ServiceException("地块不存在、已停用或无权访问");
        }
        return entity;
    }

    private Map<String, Map<String, SatelliteHealthResultRow>> latestByDateAndType(Long fieldId, Long batchId,
                                                                                      Collection<String> dates) {
        List<SatelliteHealthResultRow> rows = healthMapper.selectResultsByDates(TenantHelper.getTenantId(), fieldId, batchId,
            dates, QUERY_TYPES);
        Map<String, Map<String, SatelliteHealthResultRow>> result = new HashMap<>();
        for (SatelliteHealthResultRow row : rows) {
            if (!validDate(row.getImageDate())) {
                continue;
            }
            String taskType = SatelliteTaskTypeNormalizer.normalize(row.getTaskType());
            result.computeIfAbsent(row.getImageDate(), key -> new LinkedHashMap<>()).putIfAbsent(taskType, row);
        }
        return result;
    }

    private SatelliteHealthVo.DateCard toDateCard(String imageDate, Map<String, SatelliteHealthResultRow> byType) {
        SatelliteHealthVo.DateCard card = new SatelliteHealthVo.DateCard();
        card.setImageDate(imageDate);
        SatelliteHealthResultRow cloud = byType.get("cloudcover");
        BigDecimal cloudPercent = cloud == null ? null : areaParser.parseCloudCoverPercent(cloud.getArea());
        card.setCloudCoverPercent(scale(cloudPercent));
        card.setCloudWarning(cloudPercent != null && cloudPercent.compareTo(BigDecimal.valueOf(60)) >= 0);
        card.setThumbnailUrl(DISPLAY_TYPES.stream().map(byType::get).filter(Objects::nonNull)
            .map(SatelliteHealthResultRow::getOssUrl).filter(StrUtil::isNotBlank).findFirst().orElse(null));
        return card;
    }

    private int countValidMetrics(Map<String, SatelliteHealthResultRow> byType) {
        return (int) DISPLAY_TYPES.stream().map(byType::get).filter(Objects::nonNull)
            .filter(row -> !areaParser.parseValidArea(row.getArea()).isEmpty()).count();
    }

    private List<SatelliteHealthVo.Metric> toMetrics(Map<String, SatelliteHealthResultRow> rows) {
        return DISPLAY_TYPES.stream().map(type -> toMetric(type, rows.get(type))).filter(Objects::nonNull).toList();
    }

    private SatelliteHealthVo.Metric toMetric(String type, SatelliteHealthResultRow row) {
        if (row == null) {
            return null;
        }
        List<BigDecimal> areas = areaParser.parseValidArea(row.getArea());
        if (areas.isEmpty()) {
            return null;
        }
        BigDecimal good = areas.get(4).add(areas.get(3));
        BigDecimal medium = areas.get(2);
        BigDecimal bad = areas.get(1).add(areas.get(0));
        BigDecimal total = good.add(medium).add(bad);
        SatelliteHealthVo.Metric metric = new SatelliteHealthVo.Metric();
        metric.setType(type);
        metric.setName(typeDict.describeTaskType(type));
        metric.setImageUrl(StrUtil.isBlank(row.getOssUrl()) ? null : row.getOssUrl().trim());
        BigDecimal weighted = areas.get(1).add(areas.get(2).multiply(BigDecimal.valueOf(2)))
            .add(areas.get(3).multiply(BigDecimal.valueOf(3))).add(areas.get(4).multiply(BigDecimal.valueOf(4)));
        metric.setScore(weighted.divide(total.multiply(BigDecimal.valueOf(4)), 12, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)));
        metric.setLevels(List.of(level("GOOD", type.equals("droughtlevel") ? "润" : "好", good, total),
            level("MEDIUM", "中", medium, total), level("BAD", type.equals("droughtlevel") ? "旱" : "差", bad, total)));
        return metric;
    }

    private SatelliteHealthVo.Level level(String code, String label, BigDecimal area, BigDecimal total) {
        SatelliteHealthVo.Level level = new SatelliteHealthVo.Level();
        level.setCode(code);
        level.setLabel(label);
        level.setAreaMu(scale(area));
        level.setRatioPercent(scale(area.multiply(BigDecimal.valueOf(100)).divide(total, 12, RoundingMode.HALF_UP)));
        return level;
    }

    private void populateLowLevelAndFocus(SatelliteHealthVo.Detail detail, List<SatelliteHealthVo.Metric> metrics) {
        BigDecimal lowTotal = metrics.stream().map(metric -> metric.getLevels().get(2).getAreaMu())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal lowAverage = lowTotal.divide(BigDecimal.valueOf(metrics.size()), 12, RoundingMode.HALF_UP);
        detail.setLowLevelAreaMu(scale(lowAverage));
        if (detail.getTotalAreaMu() != null) {
            detail.setLowLevelRatioPercent(scale(lowAverage.multiply(BigDecimal.valueOf(100))
                .divide(detail.getTotalAreaMu(), 12, RoundingMode.HALF_UP)));
        }
        SatelliteHealthVo.Metric focus = metrics.stream().reduce((first, next) ->
            first.getLevels().get(2).getRatioPercent().compareTo(next.getLevels().get(2).getRatioPercent()) >= 0 ? first : next)
            .orElse(null);
        if (focus == null || focus.getLevels().get(2).getAreaMu().compareTo(BigDecimal.ZERO) == 0) {
            detail.setFocusItems(List.of("暂无"));
            return;
        }
        SatelliteHealthVo.Level low = focus.getLevels().get(2);
        detail.setFocusItems(List.of(focus.getName() + "（" + low.getAreaMu().toPlainString() + "亩 " + low.getLabel() + "）"));
    }

    private void calculateScore(SatelliteHealthVo.Detail detail, List<SatelliteHealthVo.Metric> metrics) {
        Map<String, BigDecimal> weightMap = weights.getWeights();
        BigDecimal totalWeight = metrics.stream().map(metric -> weightMap.getOrDefault(metric.getType(), BigDecimal.ZERO))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalWeight.compareTo(BigDecimal.ZERO) <= 0) {
            insufficient(detail, metrics);
            return;
        }
        BigDecimal weighted = metrics.stream().map(metric -> metric.getScore().multiply(weightMap.get(metric.getType())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal score = weighted.divide(totalWeight, 12, RoundingMode.HALF_UP).setScale(1, RoundingMode.HALF_UP);
        detail.setScoreStatus(SCORED);
        detail.setScore(score);
        detail.setGrade(score.compareTo(BigDecimal.valueOf(80)) >= 0 ? "EXCELLENT"
            : score.compareTo(BigDecimal.valueOf(60)) >= 0 ? "GOOD"
            : score.compareTo(BigDecimal.valueOf(40)) >= 0 ? "FAIR" : "POOR");
    }

    private SatelliteHealthVo.Detail insufficient(SatelliteHealthVo.Detail detail, List<SatelliteHealthVo.Metric> metrics) {
        detail.setScoreStatus(INSUFFICIENT_DATA);
        detail.setScore(null);
        detail.setGrade(null);
        detail.setMetrics(metrics);
        if (detail.getFocusItems() == null) {
            detail.setFocusItems(List.of("暂无"));
        }
        return detail;
    }

    private boolean validDate(String value) {
        if (StrUtil.isBlank(value)) {
            return false;
        }
        try {
            return !LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE).isAfter(LocalDate.now());
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private boolean validFieldArea(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private int normalizePageSize(int pageSize) {
        return Math.max(1, Math.min(pageSize, 100));
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(1, RoundingMode.HALF_UP);
    }
}
