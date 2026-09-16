package com.ym.agriculture.farmtask.workorder.support.workbench;

import cn.hutool.core.collection.CollUtil;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderGreenhouseMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderItemMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskFlowLogMapper;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskFlowLog;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskTaskPackage;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrderGreenhouse;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrderItem;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskGreenhouseBriefVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskHomeItemSummaryVo;
import com.ym.agriculture.farmtask.workorder.support.SfStaskWorkOrderAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 批量读取任务包工作台卡片所需的作业项与棚室范围。
 */
@Component
@RequiredArgsConstructor
public class SfStaskPackageWorkbenchBatchReader {

    private static final int ITEM_PREVIEW_LIMIT = 2;

    private final SfStaskWorkOrderItemMapper itemMapper;
    private final SfStaskWorkOrderGreenhouseMapper greenhouseMapper;
    private final SfStaskFlowLogMapper flowLogMapper;

    /**
     * 一次加载给定任务包的全部工作台关联数据。
     *
     * @param tenantId 当前租户
     * @param packages 任务包集合
     * @return 任务包批量数据
     */
    public PackageData load(String tenantId, Collection<SfStaskTaskPackage> packages) {
        List<Long> packageIds = (packages == null ? List.<SfStaskTaskPackage>of() : packages).stream()
            .map(SfStaskTaskPackage::getPackageId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (packageIds.isEmpty()) {
            return PackageData.empty();
        }
        List<SfStaskWorkOrderItem> items = CollUtil.emptyIfNull(itemMapper.selectByPackageIds(tenantId, packageIds));
        List<SfStaskWorkOrderGreenhouse> greenhouses =
            CollUtil.emptyIfNull(greenhouseMapper.selectByPackageIds(tenantId, packageIds));
        List<SfStaskFlowLog> flowLogs =
            CollUtil.emptyIfNull(flowLogMapper.selectByOrderIds(tenantId, packageIds));
        return new PackageData(
            buildGreenhouseIndex(packageIds, greenhouses),
            buildItemSummaryIndex(packageIds, items, greenhouses),
            SfStaskWorkOrderAssembler.indexLatestFlowLogs(flowLogs));
    }

    private static Map<Long, List<SfStaskGreenhouseBriefVo>> buildGreenhouseIndex(
        Collection<Long> packageIds, List<SfStaskWorkOrderGreenhouse> rows) {
        Map<Long, Map<Long, SfStaskGreenhouseBriefVo>> grouped = new LinkedHashMap<>();
        for (Long packageId : packageIds) {
            grouped.put(packageId, new LinkedHashMap<>());
        }
        for (SfStaskWorkOrderGreenhouse row : rows) {
            if (row.getPackageId() == null || row.getGreenhouseId() == null) {
                continue;
            }
            grouped.computeIfAbsent(row.getPackageId(), key -> new LinkedHashMap<>())
                .putIfAbsent(row.getGreenhouseId(), SfStaskWorkOrderAssembler.toGreenhouseBrief(row));
        }
        Map<Long, List<SfStaskGreenhouseBriefVo>> result = new HashMap<>();
        grouped.forEach((packageId, index) -> result.put(packageId, new ArrayList<>(index.values())));
        return result;
    }

    private static Map<Long, PackageItemSummary> buildItemSummaryIndex(Collection<Long> packageIds,
        List<SfStaskWorkOrderItem> items, List<SfStaskWorkOrderGreenhouse> greenhouses) {
        Map<Long, Long> greenhouseCountByItem = greenhouses.stream()
            .filter(row -> row.getItemId() != null && row.getGreenhouseId() != null)
            .collect(Collectors.groupingBy(SfStaskWorkOrderGreenhouse::getItemId,
                Collectors.mapping(SfStaskWorkOrderGreenhouse::getGreenhouseId,
                    Collectors.collectingAndThen(Collectors.toSet(), values -> (long) values.size()))));
        Map<Long, List<SfStaskWorkOrderItem>> itemGroups = items.stream()
            .filter(item -> item.getPackageId() != null)
            .collect(Collectors.groupingBy(SfStaskWorkOrderItem::getPackageId,
                LinkedHashMap::new, Collectors.toList()));
        Map<Long, PackageItemSummary> result = new HashMap<>();
        for (Long packageId : packageIds) {
            List<SfStaskWorkOrderItem> packageItems = itemGroups.getOrDefault(packageId, List.of());
            List<SfStaskHomeItemSummaryVo> previews = packageItems.stream()
                .limit(ITEM_PREVIEW_LIMIT)
                .map(item -> toItemSummary(item, greenhouseCountByItem))
                .toList();
            result.put(packageId, new PackageItemSummary(packageItems.size(), previews));
        }
        return result;
    }

    private static SfStaskHomeItemSummaryVo toItemSummary(
        SfStaskWorkOrderItem item, Map<Long, Long> greenhouseCountByItem) {
        SfStaskHomeItemSummaryVo summary = new SfStaskHomeItemSummaryVo();
        summary.setItemId(item.getItemId());
        summary.setWorkItemId(item.getWorkItemId());
        summary.setWorkItemName(item.getWorkItemNameSnapshot());
        summary.setGreenhouseCount(greenhouseCountByItem.getOrDefault(item.getItemId(), 0L).intValue());
        return summary;
    }

    /** 任务包工作台所需的批量关联数据。 */
    public record PackageData(
        Map<Long, List<SfStaskGreenhouseBriefVo>> greenhouses,
        Map<Long, PackageItemSummary> itemSummaries,
        Map<String, SfStaskFlowLog> flowLogs) {

        /** 返回不包含关联数据的空批次。 */
        public static PackageData empty() {
            return new PackageData(Map.of(), Map.of(), Map.of());
        }
    }

    /** 单个任务包的作业项总数和首页预览。 */
    public record PackageItemSummary(int total, List<SfStaskHomeItemSummaryVo> previews) {

        /** 返回空摘要。 */
        public static PackageItemSummary empty() {
            return new PackageItemSummary(0, List.of());
        }
    }
}
