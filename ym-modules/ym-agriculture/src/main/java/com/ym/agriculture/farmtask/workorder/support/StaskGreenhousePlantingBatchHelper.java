package com.ym.agriculture.farmtask.workorder.support;

import cn.hutool.core.collection.CollUtil;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchVo;
import com.ym.agriculture.farming.batch.service.ISfPlantingBatchService;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskAllTaskItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskGreenhouseBriefVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskHomeTaskCardVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskLeaderWorkbenchItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskManagerWorkbenchItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskPackageItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskTechnicianWorkbenchItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkOrderDetailVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkOrderVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkerAllTaskItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkerTaskItemVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * stask 任务列表/详情：按大棚批量填充进行中种植批次。
 */
@Component
@RequiredArgsConstructor
public class StaskGreenhousePlantingBatchHelper {

    private static final List<SfPlantingBatchVo> EMPTY_BATCHES = List.of();

    private final ISfPlantingBatchService plantingBatchService;

    /**
     * 批量加载大棚 ID → 进行中种植批次。
     */
    public Map<Long, List<SfPlantingBatchVo>> loadActiveBatchMap(Collection<Long> greenhouseIds) {
        if (CollUtil.isEmpty(greenhouseIds)) {
            return Map.of();
        }
        return plantingBatchService.mapActiveVoByFieldIds(greenhouseIds);
    }

    /**
     * 填充工单列表/详情基类字段。
     */
    public void enrichWorkOrderVos(List<SfStaskWorkOrderVo> vos) {
        if (CollUtil.isEmpty(vos)) {
            return;
        }
        Map<Long, List<SfPlantingBatchVo>> batchMap = loadActiveBatchMap(collectFromWorkOrderVos(vos));
        for (SfStaskWorkOrderVo vo : vos) {
            fillSplitPlantingBatches(vo.getGreenhouseId(), vo::setPlantingBatches, batchMap);
            fillGreenhouseBriefs(vo.getGreenhouses(), batchMap);
        }
    }

    /**
     * 填充工单详情（含任务包农事项内大棚）。
     */
    public void enrichWorkOrderDetailVo(SfStaskWorkOrderDetailVo vo) {
        if (vo == null) {
            return;
        }
        Set<Long> greenhouseIds = collectFromWorkOrderVos(List.of(vo));
        if (CollUtil.isNotEmpty(vo.getPackageItems())) {
            for (SfStaskPackageItemVo item : vo.getPackageItems()) {
                collectGreenhouseIds(greenhouseIds, item.getGreenhouses());
            }
        }
        Map<Long, List<SfPlantingBatchVo>> batchMap = loadActiveBatchMap(greenhouseIds);
        fillSplitPlantingBatches(vo.getGreenhouseId(), vo::setPlantingBatches, batchMap);
        fillGreenhouseBriefs(vo.getGreenhouses(), batchMap);
        if (CollUtil.isNotEmpty(vo.getPackageItems())) {
            for (SfStaskPackageItemVo item : vo.getPackageItems()) {
                fillGreenhouseBriefs(item.getGreenhouses(), batchMap);
            }
        }
    }

    /**
     * 填充任务包农事项内大棚批次。
     */
    public void enrichPackageItems(List<SfStaskPackageItemVo> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        Set<Long> greenhouseIds = new HashSet<>();
        for (SfStaskPackageItemVo item : items) {
            collectGreenhouseIds(greenhouseIds, item.getGreenhouses());
        }
        Map<Long, List<SfPlantingBatchVo>> batchMap = loadActiveBatchMap(greenhouseIds);
        for (SfStaskPackageItemVo item : items) {
            fillGreenhouseBriefs(item.getGreenhouses(), batchMap);
        }
    }

    /**
     * 填充全部任务列表项。
     */
    public void enrichAllTaskItems(List<SfStaskAllTaskItemVo> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        Map<Long, List<SfPlantingBatchVo>> batchMap = loadActiveBatchMap(collectFromAllTaskItems(items));
        for (SfStaskAllTaskItemVo item : items) {
            fillSplitPlantingBatches(item.getGreenhouseId(), item::setPlantingBatches, batchMap);
            fillGreenhouseBriefs(item.getGreenhouses(), batchMap);
        }
    }

    /**
     * 填充生产管理员工作台列表项。
     */
    public void enrichManagerWorkbenchItems(List<SfStaskManagerWorkbenchItemVo> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        Map<Long, List<SfPlantingBatchVo>> batchMap = loadActiveBatchMap(collectFromManagerWorkbenchItems(items));
        for (SfStaskManagerWorkbenchItemVo item : items) {
            fillSplitPlantingBatches(item.getGreenhouseId(), item::setPlantingBatches, batchMap);
            fillGreenhouseBriefs(item.getGreenhouses(), batchMap);
        }
    }

    /**
     * 填充组长工作台列表项。
     */
    public void enrichLeaderWorkbenchItems(List<SfStaskLeaderWorkbenchItemVo> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        Set<Long> greenhouseIds = new HashSet<>();
        for (SfStaskLeaderWorkbenchItemVo item : items) {
            if (item.getGreenhouseId() != null) {
                greenhouseIds.add(item.getGreenhouseId());
            }
        }
        Map<Long, List<SfPlantingBatchVo>> batchMap = loadActiveBatchMap(greenhouseIds);
        for (SfStaskLeaderWorkbenchItemVo item : items) {
            fillSplitPlantingBatches(item.getGreenhouseId(), item::setPlantingBatches, batchMap);
        }
    }

    /**
     * 填充技术员工作台列表项。
     */
    public void enrichTechnicianWorkbenchItems(List<SfStaskTechnicianWorkbenchItemVo> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        Set<Long> greenhouseIds = new HashSet<>();
        for (SfStaskTechnicianWorkbenchItemVo item : items) {
            if (item.getGreenhouseId() != null) {
                greenhouseIds.add(item.getGreenhouseId());
            }
            collectGreenhouseIds(greenhouseIds, item.getGreenhouses());
        }
        Map<Long, List<SfPlantingBatchVo>> batchMap = loadActiveBatchMap(greenhouseIds);
        for (SfStaskTechnicianWorkbenchItemVo item : items) {
            fillSplitPlantingBatches(item.getGreenhouseId(), item::setPlantingBatches, batchMap);
            fillGreenhouseBriefs(item.getGreenhouses(), batchMap);
        }
    }

    /**
     * 填充主页任务包卡片。
     */
    public void enrichHomeTaskCards(List<SfStaskHomeTaskCardVo> cards) {
        if (CollUtil.isEmpty(cards)) {
            return;
        }
        Set<Long> greenhouseIds = new HashSet<>();
        for (SfStaskHomeTaskCardVo card : cards) {
            collectGreenhouseIds(greenhouseIds, card.getGreenhouses());
        }
        Map<Long, List<SfPlantingBatchVo>> batchMap = loadActiveBatchMap(greenhouseIds);
        for (SfStaskHomeTaskCardVo card : cards) {
            fillGreenhouseBriefs(card.getGreenhouses(), batchMap);
        }
    }

    /**
     * 填充工人首页任务卡片。
     */
    public void enrichWorkerTaskItems(List<SfStaskWorkerTaskItemVo> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        Set<Long> greenhouseIds = new HashSet<>();
        for (SfStaskWorkerTaskItemVo item : items) {
            if (item.getGreenhouseId() != null) {
                greenhouseIds.add(item.getGreenhouseId());
            }
        }
        Map<Long, List<SfPlantingBatchVo>> batchMap = loadActiveBatchMap(greenhouseIds);
        for (SfStaskWorkerTaskItemVo item : items) {
            fillSplitPlantingBatches(item.getGreenhouseId(), item::setPlantingBatches, batchMap);
        }
    }

    /**
     * 填充工人全部任务列表项。
     */
    public void enrichWorkerAllTaskItems(List<SfStaskWorkerAllTaskItemVo> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        Set<Long> greenhouseIds = new HashSet<>();
        for (SfStaskWorkerAllTaskItemVo item : items) {
            if (item.getGreenhouseId() != null) {
                greenhouseIds.add(item.getGreenhouseId());
            }
        }
        Map<Long, List<SfPlantingBatchVo>> batchMap = loadActiveBatchMap(greenhouseIds);
        for (SfStaskWorkerAllTaskItemVo item : items) {
            fillSplitPlantingBatches(item.getGreenhouseId(), item::setPlantingBatches, batchMap);
        }
    }

    private static Set<Long> collectFromWorkOrderVos(List<SfStaskWorkOrderVo> vos) {
        Set<Long> greenhouseIds = new HashSet<>();
        for (SfStaskWorkOrderVo vo : vos) {
            if (vo.getGreenhouseId() != null) {
                greenhouseIds.add(vo.getGreenhouseId());
            }
            collectGreenhouseIds(greenhouseIds, vo.getGreenhouses());
        }
        return greenhouseIds;
    }

    private static Set<Long> collectFromAllTaskItems(List<SfStaskAllTaskItemVo> items) {
        Set<Long> greenhouseIds = new HashSet<>();
        for (SfStaskAllTaskItemVo item : items) {
            if (item.getGreenhouseId() != null) {
                greenhouseIds.add(item.getGreenhouseId());
            }
            collectGreenhouseIds(greenhouseIds, item.getGreenhouses());
        }
        return greenhouseIds;
    }

    private static Set<Long> collectFromManagerWorkbenchItems(List<SfStaskManagerWorkbenchItemVo> items) {
        Set<Long> greenhouseIds = new HashSet<>();
        for (SfStaskManagerWorkbenchItemVo item : items) {
            if (item.getGreenhouseId() != null) {
                greenhouseIds.add(item.getGreenhouseId());
            }
            collectGreenhouseIds(greenhouseIds, item.getGreenhouses());
        }
        return greenhouseIds;
    }

    private static void collectGreenhouseIds(Set<Long> target, List<SfStaskGreenhouseBriefVo> greenhouses) {
        if (CollUtil.isEmpty(greenhouses)) {
            return;
        }
        for (SfStaskGreenhouseBriefVo greenhouse : greenhouses) {
            if (greenhouse != null && greenhouse.getGreenhouseId() != null) {
                target.add(greenhouse.getGreenhouseId());
            }
        }
    }

    private static void fillGreenhouseBriefs(List<SfStaskGreenhouseBriefVo> greenhouses,
        Map<Long, List<SfPlantingBatchVo>> batchMap) {
        if (CollUtil.isEmpty(greenhouses)) {
            return;
        }
        for (SfStaskGreenhouseBriefVo greenhouse : greenhouses) {
            if (greenhouse == null || greenhouse.getGreenhouseId() == null) {
                continue;
            }
            List<SfPlantingBatchVo> batches = resolveBatches(batchMap, greenhouse.getGreenhouseId());
            greenhouse.setPlantingBatches(batches);
            greenhouse.setCropName(resolveCropName(batches));
        }
    }

    private static String resolveCropName(List<SfPlantingBatchVo> batches) {
        if (CollUtil.isEmpty(batches)) {
            return null;
        }
        String cropName = batches.stream()
            .map(batch -> {
                if (batch == null) {
                    return null;
                }
                String speciesName = batch.getSpeciesName();
                return speciesName == null || speciesName.isBlank() ? batch.getVarietyName() : speciesName;
            })
            .filter(name -> name != null && !name.isBlank())
            .map(String::trim)
            .distinct()
            .collect(Collectors.joining("、"));
        return cropName.isBlank() ? null : cropName;
    }

    private static void fillSplitPlantingBatches(Long greenhouseId,
        java.util.function.Consumer<List<SfPlantingBatchVo>> setter,
        Map<Long, List<SfPlantingBatchVo>> batchMap) {
        if (setter == null) {
            return;
        }
        if (greenhouseId == null) {
            setter.accept(EMPTY_BATCHES);
            return;
        }
        setter.accept(resolveBatches(batchMap, greenhouseId));
    }

    private static List<SfPlantingBatchVo> resolveBatches(Map<Long, List<SfPlantingBatchVo>> batchMap,
        Long greenhouseId) {
        List<SfPlantingBatchVo> batches = batchMap.get(greenhouseId);
        if (CollUtil.isEmpty(batches)) {
            return EMPTY_BATCHES;
        }
        return new ArrayList<>(batches);
    }
}
