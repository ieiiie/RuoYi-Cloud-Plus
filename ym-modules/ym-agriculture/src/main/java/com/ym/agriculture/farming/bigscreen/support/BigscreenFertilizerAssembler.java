package com.ym.agriculture.farming.bigscreen.support;

import com.ym.iot.api.domain.vo.RemoteLatestTelemetryVo;
import com.ym.iot.api.domain.vo.RemoteFertilizerStateVo;
import com.ym.iot.api.domain.vo.RemoteFertilizerRecordVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenFertilizerHistoryVo;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenFertilizerTankVo;
import cn.hutool.core.date.DateUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 施肥机实时态组装：三罐液位映射。
 */
@Component
public class BigscreenFertilizerAssembler {

    private static final String UNIT_M = "m";

    private static final String[] TANK_NAMES = {"磷肥", "钾肥", "氮肥"};

    /** 0x13 肥料类型位掩码：1=钾肥（2 号罐），2=磷肥（1 号罐），4=氮肥（3 号罐）。 */
    private static final int FERT_TYPE_POTASSIUM = 1;
    private static final int FERT_TYPE_PHOSPHORUS = 2;
    private static final int FERT_TYPE_NITROGEN = 4;

    /** 肥料种类展示名：磷肥、钾肥、氮肥 */
    private static final String[] FERTILIZER_LABELS = {"磷肥", "钾肥", "氮肥"};

    private static final String METRIC_LIQUID_LEVEL_1 = "liquid_level1";
    private static final String METRIC_LIQUID_LEVEL_2 = "liquid_level2";
    private static final String METRIC_LIQUID_LEVEL_3 = "liquid_level3";

    /**
     * 按天聚合施肥流水：同一天内各肥料施肥量累加，输出「日期 磷肥 xx，钾肥 xx，氮肥 xx」。
     *
     * @param records  原始流水，可为 null
     * @param dayLimit 返回最近天数上限
     */
    public List<SfBigscreenFertilizerHistoryVo> buildDailyRecords(List<RemoteFertilizerRecordVo> records, int dayLimit) {
        if (records == null || records.isEmpty() || dayLimit <= 0) {
            return List.of();
        }
        Map<String, DailyAggregate> byDay = new LinkedHashMap<>();
        for (SfBigscreenFertilizerHistoryVo batch : buildMomentBatches(records)) {
            String dateKey = batch.getFertilizeDate();
            if (!byDay.containsKey(dateKey) && byDay.size() >= dayLimit) {
                break;
            }
            byDay.computeIfAbsent(dateKey, DailyAggregate::new).merge(batch);
        }
        List<SfBigscreenFertilizerHistoryVo> result = new ArrayList<>();
        for (DailyAggregate daily : byDay.values()) {
            result.add(daily.toVo());
        }
        return result;
    }

    /**
     * 将原始流水按 {@code record_time}（秒级）合并为三罐一行（日内聚合的中间态）。
     */
    List<SfBigscreenFertilizerHistoryVo> buildMomentBatches(List<RemoteFertilizerRecordVo> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        Map<Long, FertilizeBatchGroup> groups = new LinkedHashMap<>();
        List<RemoteFertilizerRecordVo> sorted = records.stream()
            .filter(r -> r != null && r.getRecordTime() != null)
            .sorted(Comparator.comparing(RemoteFertilizerRecordVo::getRecordTime).reversed())
            .toList();
        for (RemoteFertilizerRecordVo record : sorted) {
            long secondKey = record.getRecordTime().getTime() / 1000L;
            groups.computeIfAbsent(secondKey, k -> new FertilizeBatchGroup(record.getRecordTime()))
                .accumulate(record, this);
        }
        List<SfBigscreenFertilizerHistoryVo> result = new ArrayList<>();
        for (FertilizeBatchGroup group : groups.values()) {
            result.add(group.toBatchVo());
        }
        return result;
    }

    static String formatFertKg(BigDecimal kg) {
        if (kg == null) {
            return "0kg";
        }
        BigDecimal normalized = kg.stripTrailingZeros();
        if (normalized.scale() <= 0) {
            return normalized.toPlainString() + "kg";
        }
        return normalized.toPlainString() + "kg";
    }

    static String buildDailySummaryText(String date, BigDecimal phosphorus, BigDecimal potassium, BigDecimal nitrogen) {
        String day = date == null ? "---- -- --" : date;
        return day + " 磷肥 " + formatFertKg(phosphorus)
            + "，钾肥 " + formatFertKg(potassium)
            + "，氮肥 " + formatFertKg(nitrogen);
    }

    static BigDecimal addKg(BigDecimal current, BigDecimal delta) {
        if (delta == null) {
            return current;
        }
        if (current == null) {
            return delta;
        }
        return current.add(delta);
    }

    private static final class DailyAggregate {

        private final String date;
        private Date lastFertilizedAt;
        private final BigDecimal[] fertKg = new BigDecimal[3];
        private int totalDurationMinutes;

        private DailyAggregate(String date) {
            this.date = date;
        }

        private void merge(SfBigscreenFertilizerHistoryVo batch) {
            if (batch.getFertilizedAt() != null
                && (lastFertilizedAt == null || batch.getFertilizedAt().after(lastFertilizedAt))) {
                lastFertilizedAt = batch.getFertilizedAt();
            }
            fertKg[0] = addKg(fertKg[0], batch.getPhosphorusKg());
            fertKg[1] = addKg(fertKg[1], batch.getPotassiumKg());
            fertKg[2] = addKg(fertKg[2], batch.getNitrogenKg());
            if (batch.getDurationMinutes() != null) {
                totalDurationMinutes += batch.getDurationMinutes();
            }
        }

        private SfBigscreenFertilizerHistoryVo toVo() {
            SfBigscreenFertilizerHistoryVo vo = new SfBigscreenFertilizerHistoryVo();
            vo.setFertilizeDate(date);
            vo.setFertilizedAt(lastFertilizedAt);
            vo.setPhosphorusKg(fertKg[0]);
            vo.setPotassiumKg(fertKg[1]);
            vo.setNitrogenKg(fertKg[2]);
            if (totalDurationMinutes > 0) {
                vo.setDurationMinutes(totalDurationMinutes);
            }
            vo.setSummaryText(buildDailySummaryText(date, fertKg[0], fertKg[1], fertKg[2]));
            return vo;
        }
    }

    private static final class FertilizeBatchGroup {

        private final Date recordTime;
        private final BigDecimal[] fertKg = new BigDecimal[3];
        private int maxDurationMinutes;

        private FertilizeBatchGroup(Date recordTime) {
            this.recordTime = recordTime;
        }

        private void accumulate(RemoteFertilizerRecordVo record, BigscreenFertilizerAssembler assembler) {
            Integer fertIdx = assembler.resolveFertilizerIndex(record.getFertilizationType());
            if (fertIdx != null) {
                fertKg[fertIdx] = record.getFertilizationQuantity();
            }
            if (record.getFertilizationSeconds() != null && record.getFertilizationSeconds() > maxDurationMinutes) {
                maxDurationMinutes = record.getFertilizationSeconds();
            }
        }

        private SfBigscreenFertilizerHistoryVo toBatchVo() {
            SfBigscreenFertilizerHistoryVo vo = new SfBigscreenFertilizerHistoryVo();
            vo.setFertilizeDate(DateUtil.format(recordTime, "yyyy-MM-dd"));
            vo.setFertilizedAt(recordTime);
            vo.setPhosphorusKg(fertKg[0]);
            vo.setPotassiumKg(fertKg[1]);
            vo.setNitrogenKg(fertKg[2]);
            if (maxDurationMinutes > 0) {
                vo.setDurationMinutes(maxDurationMinutes);
            }
            return vo;
        }
    }

    /**
     * 将状态快照映射为固定 3 罐液位列表。
     *
     * @param snap MQTT 状态快照，可为 null
     * @return 罐号 1→3 的液位列表，无数据时 liquidLevelM 为 null
     */
    public List<SfBigscreenFertilizerTankVo> buildTanks(RemoteFertilizerStateVo snap) {
        List<SfBigscreenFertilizerTankVo> tanks = new ArrayList<>(3);
        Float[] levels = snap == null
            ? new Float[] {null, null, null}
            : new Float[] {snap.getLiquidLevel1(), snap.getLiquidLevel2(), snap.getLiquidLevel3()};
        for (int i = 0; i < 3; i++) {
            SfBigscreenFertilizerTankVo tank = new SfBigscreenFertilizerTankVo();
            tank.setTankNo(i + 1);
            tank.setName(TANK_NAMES[i]);
            tank.setLiquidLevelM(toDecimal(levels[i]));
            tank.setUnit(UNIT_M);
            tanks.add(tank);
        }
        return tanks;
    }

    /**
     * 解析三罐液位数据源：内存 MQTT 快照优先，缺失时回退时序最新测点。
     *
     * @param memorySnapshot IoT 服务中的 MQTT 状态快照，可为 null
     * @param latestTelemetry 设备最新测点，可为 null
     * @return 用于 {@link #buildTanks(RemoteFertilizerStateVo)} 的快照，可能各液位仍为 null
     */
    public RemoteFertilizerStateVo resolveTankSnapshot(RemoteFertilizerStateVo memorySnapshot,
                                                       RemoteLatestTelemetryVo latestTelemetry) {
        if (memorySnapshot != null && hasAnyLiquidLevel(memorySnapshot)) {
            return memorySnapshot;
        }
        RemoteFertilizerStateVo fromTelemetry = snapshotFromLatest(latestTelemetry);
        if (fromTelemetry != null && hasAnyLiquidLevel(fromTelemetry)) {
            return fromTelemetry;
        }
        return memorySnapshot != null ? memorySnapshot : fromTelemetry;
    }

    /**
     * 从最新测点构造仅含三罐液位的快照。
     */
    public RemoteFertilizerStateVo snapshotFromLatest(RemoteLatestTelemetryVo latest) {
        if (latest == null || latest.getMetrics() == null || latest.getMetrics().isEmpty()) {
            return null;
        }
        Map<String, BigDecimal> metrics = latest.getMetrics();
        RemoteFertilizerStateVo snapshot = new RemoteFertilizerStateVo();
        snapshot.setDeviceId(latest.getDeviceId());
        snapshot.setLiquidLevel1(metricToFloat(metrics.get(METRIC_LIQUID_LEVEL_1)));
        snapshot.setLiquidLevel2(metricToFloat(metrics.get(METRIC_LIQUID_LEVEL_2)));
        snapshot.setLiquidLevel3(metricToFloat(metrics.get(METRIC_LIQUID_LEVEL_3)));
        return snapshot;
    }

    private static boolean hasAnyLiquidLevel(RemoteFertilizerStateVo snap) {
        return snap.getLiquidLevel1() != null
            || snap.getLiquidLevel2() != null
            || snap.getLiquidLevel3() != null;
    }

    private static Float metricToFloat(BigDecimal value) {
        return value == null ? null : value.floatValue();
    }

    private static BigDecimal toDecimal(Float value) {
        return value == null ? null : BigDecimal.valueOf(value.doubleValue());
    }

    /**
     * 将 0x13 帧 {@code fertilization_type}（位掩码 1/2/4）映射为肥料种类下标。
     *
     * @param fertilizationType 协议原始类型，可为 null
     * @return 0=磷肥、1=钾肥、2=氮肥；无法识别时返回 null
     */
    public Integer resolveFertilizerIndex(Integer fertilizationType) {
        if (fertilizationType == null || fertilizationType <= 0) {
            return null;
        }
        if (fertilizationType > 4 || (fertilizationType & (fertilizationType - 1)) != 0) {
            return null;
        }
        return switch (fertilizationType) {
            case FERT_TYPE_PHOSPHORUS -> 0;
            case FERT_TYPE_POTASSIUM -> 1;
            case FERT_TYPE_NITROGEN -> 2;
            default -> null;
        };
    }

    /**
     * 将 0x13 帧 {@code fertilization_type} 映射为物理罐号 1/2/3（液位区展示用）。
     */
    public Integer resolveRecordTankNo(Integer fertilizationType) {
        Integer fertIdx = resolveFertilizerIndex(fertilizationType);
        if (fertIdx == null) {
            return null;
        }
        return switch (fertIdx) {
            case 0 -> 1;
            case 1 -> 2;
            case 2 -> 3;
            default -> null;
        };
    }

    /**
     * 按物理罐号返回罐内肥料展示名：1 磷肥、2 钾肥、3 氮肥。
     */
    public String resolveRecordTankName(Integer tankNo) {
        if (tankNo == null || tankNo < 1 || tankNo > FERTILIZER_LABELS.length) {
            return null;
        }
        return FERTILIZER_LABELS[tankNo - 1];
    }
}
