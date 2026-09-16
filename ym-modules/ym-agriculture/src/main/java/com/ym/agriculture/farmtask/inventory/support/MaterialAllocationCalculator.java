package com.ym.agriculture.farmtask.inventory.support;

import com.ym.agriculture.farmtask.inventory.InventoryBusinessException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 每种物资按组长大棚数比例独立拆分。预览和最终保存必须复用该无状态组件。
 */
@Component
public class MaterialAllocationCalculator {

    public List<LeaderAllocation> allocate(List<MaterialTotal> materials, List<LeaderWeight> leaders) {
        if (materials == null || materials.isEmpty()) {
            return List.of();
        }
        Set<Long> materialIds = new HashSet<>();
        for (MaterialTotal material : materials) {
            if (material == null || material.inventoryMaterialId() == null
                || !materialIds.add(material.inventoryMaterialId())) {
                throw InventoryBusinessException.rule("物资不能为空且不能重复");
            }
        }
        List<LeaderWeight> sourceLeaders = leaders == null ? List.of() : leaders;
        Set<Long> leaderIds = new HashSet<>();
        Set<Integer> leaderOrders = new HashSet<>();
        for (LeaderWeight leader : sourceLeaders) {
            if (leader == null || leader.leaderEmployeeId() == null
                || !leaderIds.add(leader.leaderEmployeeId())) {
                throw InventoryBusinessException.rule("组长不能为空且不能重复");
            }
            if (leader.greenhouseCount() < 0) {
                throw InventoryBusinessException.rule("组长负责大棚数不能小于0");
            }
            if (!leaderOrders.add(leader.order())) {
                throw InventoryBusinessException.rule("组长名单顺序不能重复");
            }
        }
        List<LeaderWeight> normalizedLeaders = sourceLeaders.stream()
            .filter(item -> item.greenhouseCount() > 0)
            .sorted(Comparator.comparingInt(LeaderWeight::order))
            .toList();
        int totalGreenhouses = normalizedLeaders.stream().mapToInt(LeaderWeight::greenhouseCount).sum();
        if (totalGreenhouses <= 0) {
            throw InventoryBusinessException.rule("没有可用于物料拆分的组长大棚");
        }
        Map<Long, LinkedHashMap<Long, BigDecimal>> result = new LinkedHashMap<>();
        normalizedLeaders.forEach(leader -> result.put(leader.leaderEmployeeId(), new LinkedHashMap<>()));

        for (MaterialTotal material : materials) {
            BigDecimal total = InventoryQuantity.positive(material.quantity(), "物资数量");
            BigDecimal allocated = InventoryQuantity.ZERO;
            for (LeaderWeight leader : normalizedLeaders) {
                BigDecimal share = total.multiply(BigDecimal.valueOf(leader.greenhouseCount()))
                    .divide(BigDecimal.valueOf(totalGreenhouses), InventoryQuantity.ZERO.scale(), RoundingMode.DOWN);
                result.get(leader.leaderEmployeeId()).put(material.inventoryMaterialId(), share);
                allocated = allocated.add(share);
            }
            BigDecimal remainder = total.subtract(allocated).setScale(1, RoundingMode.UNNECESSARY);
            if (remainder.compareTo(InventoryQuantity.ZERO) > 0) {
                LeaderWeight tailLeader = normalizedLeaders.stream()
                    .max(Comparator.comparingInt(LeaderWeight::greenhouseCount)
                        .thenComparing(Comparator.comparingInt(LeaderWeight::order).reversed()))
                    .orElseThrow();
                result.get(tailLeader.leaderEmployeeId()).merge(material.inventoryMaterialId(), remainder, BigDecimal::add);
            }
        }

        List<LeaderAllocation> allocations = new ArrayList<>();
        for (LeaderWeight leader : normalizedLeaders) {
            List<MaterialAllocation> lines = result.get(leader.leaderEmployeeId()).entrySet().stream()
                .filter(entry -> entry.getValue().compareTo(InventoryQuantity.ZERO) > 0)
                .map(entry -> new MaterialAllocation(entry.getKey(), entry.getValue()))
                .toList();
            if (!lines.isEmpty()) {
                allocations.add(new LeaderAllocation(leader.leaderEmployeeId(), leader.leaderName(),
                    leader.greenhouseCount(), leader.order(), leader.greenhouseNames(), lines));
            }
        }
        return allocations;
    }

    public record MaterialTotal(Long inventoryMaterialId, BigDecimal quantity) {
    }

    public record LeaderWeight(Long leaderEmployeeId, String leaderName, int greenhouseCount,
                               int order, List<String> greenhouseNames) {
    }

    public record MaterialAllocation(Long inventoryMaterialId, BigDecimal quantity) {
    }

    public record LeaderAllocation(Long leaderEmployeeId, String leaderName, int greenhouseCount,
                                   int order, List<String> greenhouseNames, List<MaterialAllocation> materials) {
    }
}
