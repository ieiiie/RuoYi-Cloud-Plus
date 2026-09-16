package com.ym.agriculture.farming.batch.model.constants;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;

/**
 * 种植批次状态及合法流转。
 */
public final class PlantingBatchStatus {

    public static final String PLANNING = "PLANNING";
    public static final String PLANTING = "PLANTING";
    public static final String GROWING = "GROWING";
    public static final String HARVESTING = "HARVESTING";
    public static final String FINISHED = "FINISHED";
    public static final String FAILED = "FAILED";

    public static final List<String> ACTIVE_STATUSES = List.of(PLANNING, PLANTING, GROWING, HARVESTING);

    public static final Comparator<String> ACTIVE_PRIORITY = Comparator.comparingInt(status -> switch (status) {
        case HARVESTING -> 0;
        case GROWING -> 1;
        case PLANTING -> 2;
        case PLANNING -> 3;
        default -> 99;
    });

    private static final Map<String, Set<String>> NEXT = Map.of(
        PLANNING, Set.of(PLANTING, FAILED),
        PLANTING, Set.of(GROWING, FAILED),
        GROWING, Set.of(HARVESTING, FAILED),
        HARVESTING, Set.of(FINISHED, FAILED),
        FINISHED, Set.of(),
        FAILED, Set.of()
    );

    private PlantingBatchStatus() {
    }

    public static boolean isActive(String status) {
        return status != null && ACTIVE_STATUSES.contains(status);
    }

    public static boolean canTransition(String from, String to) {
        Set<String> allowed = NEXT.get(from);
        return allowed != null && allowed.contains(to);
    }
}
