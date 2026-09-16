package com.ym.agriculture.farming.bigscreen.support;

import cn.hutool.core.date.DateUtil;
import com.ym.iot.api.domain.vo.RemoteDeviceSummaryVo;
import com.ym.iot.api.domain.vo.RemoteLatestTelemetryVo;
import com.ym.iot.api.domain.vo.RemoteValveSessionVo;
import com.ym.agriculture.farming.dashboard.support.DashboardSensorOnline;
import com.ym.agriculture.farming.bigscreen.model.vo.SfBigscreenValveHistoryVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 大屏阀门实时态与灌溉记录组装。
 */
@Component
public class BigscreenValveAssembler {

    private static final String METRIC_FLOW_RATE = "flow_rate";

    private static final String METRIC_VALVE_POSITION_PREFIX = "valve_position_";

    private static final String SWITCH_OPEN = "开启";

    private static final String SWITCH_CLOSED = "关闭";

    /** 遥测角度未吸附到任一离散档位（如五通 90° 介于 60° 开与 120° 开之间） */
    private static final String SWITCH_INTERMEDIATE = "未定档";

    /** 单设备默认阀口号 */
    public static final int DEFAULT_VALVE_NO = 1;

    private static final int[] DEFAULT_VALVE_NOS = {DEFAULT_VALVE_NO};

    private static final int[] SEPARATE_VALVE_NOS = {1, 2};

    /**
     * 遥测角度与离散档位的吸附容差（度），与开阀会话、控阀实时确认容差一致。
     */
    private static final double POSITION_TOLERANCE_DEGREES = BigscreenValveType.DEFAULT_TOLERANCE;

    public BigDecimal resolveAngle(RemoteLatestTelemetryVo latest, int valveNo) {
        if (latest == null || latest.getMetrics() == null || valveNo < 1) {
            return null;
        }
        return latest.getMetrics().get(METRIC_VALVE_POSITION_PREFIX + valveNo);
    }

    public BigDecimal resolveFlowRate(RemoteLatestTelemetryVo latest) {
        if (latest == null || latest.getMetrics() == null) {
            return null;
        }
        return latest.getMetrics().get(METRIC_FLOW_RATE);
    }

    /**
     * 按阀型离散档位判断开阀（见 MQTT 协议第 4 节）。
     */
    public boolean isOpen(RemoteDeviceSummaryVo device, BigDecimal angleDeg, String channelTagValue) {
        if (angleDeg == null) {
            return false;
        }
        BigscreenValveType valveType = BigscreenValveTypeResolver.resolve(device, channelTagValue);
        return valveType.isOpen(angleDeg.doubleValue(), POSITION_TOLERANCE_DEGREES);
    }

    /**
     * 大屏汇总开阀判定：须在线且角度按阀型判定为开阀位，离线不计入避免陈旧测点。
     */
    public boolean isOpenForSummary(boolean online, RemoteDeviceSummaryVo device, BigDecimal angleDeg,
                                  String channelTagValue) {
        return online && isOpen(device, angleDeg, channelTagValue);
    }

    /**
     * 设备级开阀判定：分体阀聚合 1、2 号阀口，其余阀型沿用 1 号阀口。
     *
     * <p>有效 OPEN 会话仅作为遥测缺失或早于会话时的临时兜底。</p>
     */
    public boolean isDeviceOpen(boolean online, RemoteDeviceSummaryVo device, RemoteLatestTelemetryVo latest,
                                String channelTagValue, List<RemoteValveSessionVo> effectiveOpenSessions) {
        if (!online || device == null) {
            return false;
        }
        for (int valveNo : resolveSummaryValveNos(device)) {
            if (resolveTelemetryState(device, resolveAngle(latest, valveNo), channelTagValue)
                == ValveTelemetryState.OPEN) {
                return true;
            }
        }
        if (effectiveOpenSessions == null || effectiveOpenSessions.isEmpty()) {
            return false;
        }
        return effectiveOpenSessions.stream()
            .anyMatch(session -> device.getDeviceId() != null
                && device.getDeviceId().equals(session.getDeviceId()));
    }

    /**
     * 设备级开关文案：任一阀口开启为开启，无开启但存在未定档阀口为未定档。
     */
    public String resolveDeviceSwitchStatus(boolean online, RemoteDeviceSummaryVo device, RemoteLatestTelemetryVo latest,
                                            String channelTagValue,
                                            List<RemoteValveSessionVo> effectiveOpenSessions) {
        if (!online || device == null) {
            return SWITCH_CLOSED;
        }
        if (isDeviceOpen(true, device, latest, channelTagValue, effectiveOpenSessions)) {
            return SWITCH_OPEN;
        }
        for (int valveNo : resolveSummaryValveNos(device)) {
            if (resolveTelemetryState(device, resolveAngle(latest, valveNo), channelTagValue)
                == ValveTelemetryState.INTERMEDIATE) {
                return SWITCH_INTERMEDIATE;
            }
        }
        return SWITCH_CLOSED;
    }

    /**
     * 过滤大屏仍可视为有效的 OPEN 会话。
     *
     * <p>同阀口遥测缺失、无采集时间或早于会话开启时间时保留会话；更新遥测到达后，
     * 只有遥测仍处于开阀档位才保留。该方法只过滤展示数据，不更新会话表。</p>
     */
    public List<RemoteValveSessionVo> filterEffectiveOpenSessions(List<RemoteValveSessionVo> openSessions,
                                                            Map<Long, RemoteDeviceSummaryVo> deviceById,
                                                            Map<Long, RemoteLatestTelemetryVo> latestByDevice,
                                                            Map<Long, String> channelTagByDevice,
                                                            Date now) {
        if (openSessions == null || openSessions.isEmpty() || deviceById == null) {
            return List.of();
        }
        List<RemoteValveSessionVo> result = new ArrayList<>();
        for (RemoteValveSessionVo session : openSessions) {
            if (!isOnlineDeviceSession(session, deviceById, now)) {
                continue;
            }
            RemoteDeviceSummaryVo device = deviceById.get(session.getDeviceId());
            int valveNo = session.getValveNo() == null ? DEFAULT_VALVE_NO : session.getValveNo();
            if (!isSummaryValveNo(device, valveNo)) {
                continue;
            }
            RemoteLatestTelemetryVo latest = latestByDevice != null ? latestByDevice.get(session.getDeviceId()) : null;
            BigDecimal angleDeg = resolveAngle(latest, valveNo);
            Date collectTime = resolveCollectTime(latest, valveNo);
            Date openTime = session.getOpenTime();
            if (angleDeg == null || collectTime == null || openTime == null || collectTime.before(openTime)) {
                result.add(session);
                continue;
            }
            String channelTagValue = channelTagByDevice != null
                ? channelTagByDevice.get(session.getDeviceId()) : null;
            if (resolveTelemetryState(device, angleDeg, channelTagValue) == ValveTelemetryState.OPEN) {
                result.add(session);
            }
        }
        return result;
    }

    public boolean isIrrigating(boolean online, BigDecimal flowRateM3h) {
        return online && flowRateM3h != null && flowRateM3h.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 兼容仅知 open 布尔时的文案（无角度时视为关闭）。
     */
    public String resolveSwitchStatus(boolean open) {
        return open ? SWITCH_OPEN : SWITCH_CLOSED;
    }

    /**
     * 按遥测角度与阀型离散档位解析开/关/未定档文案。
     * <p>离线或无角度时返回「关闭」；中间角（如五通 90°）返回「未定档」，不等同于相邻档位。</p>
     */
    public String resolveSwitchStatus(RemoteDeviceSummaryVo device, BigDecimal angleDeg, String channelTagValue,
                                      boolean online) {
        if (!online || angleDeg == null) {
            return SWITCH_CLOSED;
        }
        BigscreenValveType valveType = BigscreenValveTypeResolver.resolve(device, channelTagValue);
        double angle = angleDeg.doubleValue();
        if (valveType.isOpen(angle, POSITION_TOLERANCE_DEGREES)) {
            return SWITCH_OPEN;
        }
        if (valveType.isClosed(angle, POSITION_TOLERANCE_DEGREES)) {
            return SWITCH_CLOSED;
        }
        return SWITCH_INTERMEDIATE;
    }

    private Date resolveCollectTime(RemoteLatestTelemetryVo latest, int valveNo) {
        if (latest == null || latest.getCollectTimes() == null || valveNo < 1) {
            return null;
        }
        return latest.getCollectTimes().get(METRIC_VALVE_POSITION_PREFIX + valveNo);
    }

    private ValveTelemetryState resolveTelemetryState(RemoteDeviceSummaryVo device, BigDecimal angleDeg,
                                                       String channelTagValue) {
        if (angleDeg == null) {
            return ValveTelemetryState.MISSING;
        }
        BigscreenValveType valveType = BigscreenValveTypeResolver.resolve(device, channelTagValue);
        double angle = angleDeg.doubleValue();
        if (valveType.isOpen(angle, POSITION_TOLERANCE_DEGREES)) {
            return ValveTelemetryState.OPEN;
        }
        if (valveType.isClosed(angle, POSITION_TOLERANCE_DEGREES)) {
            return ValveTelemetryState.CLOSED;
        }
        return ValveTelemetryState.INTERMEDIATE;
    }

    private int[] resolveSummaryValveNos(RemoteDeviceSummaryVo device) {
        return isSeparateValve(device) ? SEPARATE_VALVE_NOS : DEFAULT_VALVE_NOS;
    }

    private boolean isSummaryValveNo(RemoteDeviceSummaryVo device, int valveNo) {
        if (isSeparateValve(device)) {
            return valveNo == 1 || valveNo == 2;
        }
        return valveNo == DEFAULT_VALVE_NO;
    }

    private boolean isSeparateValve(RemoteDeviceSummaryVo device) {
        if (device == null) {
            return false;
        }
        if (BigscreenValveTypeResolver.PRODUCT_KEY_SEPARATE.equalsIgnoreCase(device.getProductKey())) {
            return true;
        }
        return StringUtils.contains(device.getDeviceName(), BigscreenValveTypeResolver.PRODUCT_KEY_SEPARATE_LABEL);
    }

    /**
     * 当前开启会话最大持续秒数：仅统计在线设备，离线设备的陈旧 OPEN 会话不计入。
     */
    public int resolveMaxOpenDurationSeconds(List<RemoteValveSessionVo> openSessions,
                                           Map<Long, RemoteDeviceSummaryVo> deviceById,
                                           Date now) {
        if (openSessions == null || openSessions.isEmpty()) {
            return 0;
        }
        return openSessions.stream()
            .filter(session -> isOnlineDeviceSession(session, deviceById, now))
            .map(RemoteValveSessionVo::getCurrentDurationSeconds)
            .filter(seconds -> seconds != null && seconds > 0)
            .max(Integer::compareTo)
            .orElse(0);
    }

    /**
     * 组装设备灌溉/开阀记录：瞬时流量&gt;0 且在线为「灌溉」，否则为「阀门开启」时长。
     */
    public List<SfBigscreenValveHistoryVo> buildRecentHistory(List<RemoteValveSessionVo> endedSessions,
                                                              List<RemoteValveSessionVo> openSessions,
                                                              Map<Long, RemoteDeviceSummaryVo> deviceById,
                                                              Map<Long, RemoteLatestTelemetryVo> latestByDevice,
                                                              Date now,
                                                              int limit) {
        if (limit <= 0) {
            return List.of();
        }
        List<SfBigscreenValveHistoryVo> records = new ArrayList<>();
        records.addAll(buildEndedDeviceHistory(endedSessions, deviceById, latestByDevice));
        records.addAll(buildOngoingDeviceHistory(openSessions, deviceById, latestByDevice, now));
        records.sort(Comparator.comparing(SfBigscreenValveHistoryVo::getOperatedAt,
            Comparator.nullsLast(Comparator.reverseOrder())));
        if (records.size() <= limit) {
            return records;
        }
        return new ArrayList<>(records.subList(0, limit));
    }

    private List<SfBigscreenValveHistoryVo> buildEndedDeviceHistory(List<RemoteValveSessionVo> endedSessions,
                                                                    Map<Long, RemoteDeviceSummaryVo> deviceById,
                                                                    Map<Long, RemoteLatestTelemetryVo> latestByDevice) {
        if (endedSessions == null || endedSessions.isEmpty()) {
            return List.of();
        }
        Map<String, DeviceIrrigationGroup> groups = new LinkedHashMap<>();
        List<RemoteValveSessionVo> sorted = endedSessions.stream()
            .filter(this::isAggregatableEndedSession)
            .sorted(Comparator.comparing(RemoteValveSessionVo::getCloseTime).reversed())
            .toList();
        for (RemoteValveSessionVo session : sorted) {
            if (session.getDeviceId() == null || session.getCloseTime() == null) {
                continue;
            }
            long minuteKey = DateUtil.beginOfMinute(session.getCloseTime()).getTime();
            String groupKey = session.getDeviceId() + ":" + minuteKey;
            groups.computeIfAbsent(groupKey, k -> new DeviceIrrigationGroup(session.getDeviceId(), false))
                .accumulate(session);
        }
        List<SfBigscreenValveHistoryVo> result = new ArrayList<>();
        for (DeviceIrrigationGroup group : groups.values()) {
            result.add(toDeviceHistoryVo(group, deviceById, latestByDevice));
        }
        return result;
    }

    private List<SfBigscreenValveHistoryVo> buildOngoingDeviceHistory(List<RemoteValveSessionVo> openSessions,
                                                                      Map<Long, RemoteDeviceSummaryVo> deviceById,
                                                                      Map<Long, RemoteLatestTelemetryVo> latestByDevice,
                                                                      Date now) {
        if (openSessions == null || openSessions.isEmpty()) {
            return List.of();
        }
        Map<Long, DeviceIrrigationGroup> groups = new LinkedHashMap<>();
        for (RemoteValveSessionVo session : openSessions) {
            if (!isAggregatableOpenSession(session) || session.getDeviceId() == null) {
                continue;
            }
            if (!isOnlineDeviceSession(session, deviceById, now)) {
                continue;
            }
            groups.computeIfAbsent(session.getDeviceId(), id -> new DeviceIrrigationGroup(id, true))
                .accumulate(session);
        }
        List<SfBigscreenValveHistoryVo> result = new ArrayList<>();
        for (DeviceIrrigationGroup group : groups.values()) {
            result.add(toDeviceHistoryVo(group, deviceById, latestByDevice));
        }
        return result;
    }

    private boolean isAggregatableEndedSession(RemoteValveSessionVo session) {
        if (session == null || session.getCloseTime() == null) {
            return false;
        }
        String status = session.getStatus();
        return ("CLOSED".equals(status)
            || "ABORTED".equals(status))
            && resolveSessionDurationSeconds(session, false) > 0;
    }

    private boolean isAggregatableOpenSession(RemoteValveSessionVo session) {
        return session != null
            && "OPEN".equals(session.getStatus())
            && resolveSessionDurationSeconds(session, true) > 0;
    }

    private int resolveSessionDurationSeconds(RemoteValveSessionVo session, boolean ongoing) {
        if (session == null) {
            return 0;
        }
        if (ongoing) {
            return session.getCurrentDurationSeconds() != null && session.getCurrentDurationSeconds() > 0
                ? session.getCurrentDurationSeconds() : 0;
        }
        if (session.getDurationSeconds() != null && session.getDurationSeconds() > 0) {
            return session.getDurationSeconds();
        }
        if (session.getOpenTime() == null || session.getCloseTime() == null) {
            return 0;
        }
        long seconds = Math.max(0L,
            (session.getCloseTime().getTime() - session.getOpenTime().getTime()) / 1000L);
        return seconds > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) seconds;
    }

    private SfBigscreenValveHistoryVo toDeviceHistoryVo(DeviceIrrigationGroup group,
                                                        Map<Long, RemoteDeviceSummaryVo> deviceById,
                                                        Map<Long, RemoteLatestTelemetryVo> latestByDevice) {
        RemoteDeviceSummaryVo device = deviceById != null ? deviceById.get(group.deviceId) : null;
        RemoteLatestTelemetryVo latest = latestByDevice != null ? latestByDevice.get(group.deviceId) : null;
        String deviceName = device != null ? device.getDeviceName() : null;
        String deviceCode = device != null ? device.getDeviceCode() : group.deviceCodeFallback;
        BigDecimal flowRate = resolveFlowRate(latest);
        boolean irrigating = resolveDeviceIrrigating(device, flowRate, group.ongoing);
        int durationSeconds = group.resolveDurationSeconds();
        int durationMinutes = toDurationMinutes(durationSeconds);

        SfBigscreenValveHistoryVo vo = new SfBigscreenValveHistoryVo();
        vo.setDeviceId(group.deviceId);
        vo.setDeviceCode(deviceCode);
        vo.setDeviceName(deviceName);
        vo.setOpenValveCount(group.valveCount);
        vo.setOpenTime(group.minOpenTime);
        vo.setCloseTime(group.ongoing ? null : group.maxCloseTime);
        vo.setOperatedAt(group.ongoing ? group.minOpenTime : group.maxCloseTime);
        vo.setDurationSeconds(durationSeconds);
        vo.setDurationMinutes(durationMinutes);
        vo.setFlowRateM3h(flowRate);
        vo.setIrrigating(irrigating);
        vo.setStatus(group.ongoing ? "OPEN" : group.dominantStatus);
        vo.setOngoing(group.ongoing);
        vo.setSummaryText(buildDeviceSummaryText(deviceName, deviceCode, durationMinutes, irrigating));
        return vo;
    }

    /**
     * 灌溉判定：进行中且在线且瞬时流量 &gt; 0；已结束会话仅展示阀门开启时长。
     */
    private boolean resolveDeviceIrrigating(RemoteDeviceSummaryVo device, BigDecimal flowRateM3h, boolean ongoing) {
        if (!ongoing || device == null) {
            return false;
        }
        return isIrrigating(DashboardSensorOnline.isOnline(device, new Date()), flowRateM3h);
    }

    private static boolean isOnlineDeviceSession(RemoteValveSessionVo session,
                                                 Map<Long, RemoteDeviceSummaryVo> deviceById,
                                                 Date now) {
        if (session == null || session.getDeviceId() == null || deviceById == null) {
            return false;
        }
        RemoteDeviceSummaryVo device = deviceById.get(session.getDeviceId());
        return device != null && DashboardSensorOnline.isOnline(device, now);
    }

    static String buildDeviceSummaryText(String deviceName, String deviceCode,
                                         int durationMinutes, boolean irrigating) {
        String name = StringUtils.defaultIfBlank(deviceName, deviceCode);
        String action = irrigating ? "灌溉" : "阀门开启";
        if (StringUtils.isBlank(name)) {
            return action + " " + durationMinutes + "分钟";
        }
        return name + " " + action + " " + durationMinutes + "分钟";
    }

    private static int toDurationMinutes(int durationSeconds) {
        return BigDecimal.valueOf(Math.max(0, durationSeconds))
            .divide(BigDecimal.valueOf(60), 0, RoundingMode.HALF_UP)
            .intValue();
    }

    private static final class DeviceIrrigationGroup {

        private final Long deviceId;
        private final boolean ongoing;
        private String deviceCodeFallback;
        private Date minOpenTime;
        private Date maxCloseTime;
        private int valveCount;
        private String dominantStatus;

        private DeviceIrrigationGroup(Long deviceId, boolean ongoing) {
            this.deviceId = deviceId;
            this.ongoing = ongoing;
        }

        private void accumulate(RemoteValveSessionVo session) {
            valveCount++;
            if (StringUtils.isBlank(deviceCodeFallback)) {
                deviceCodeFallback = session.getDeviceCode();
            }
            if (session.getOpenTime() != null
                && (minOpenTime == null || session.getOpenTime().before(minOpenTime))) {
                minOpenTime = session.getOpenTime();
            }
            if (!ongoing && session.getCloseTime() != null
                && (maxCloseTime == null || session.getCloseTime().after(maxCloseTime))) {
                maxCloseTime = session.getCloseTime();
            }
            if (!ongoing && session.getStatus() != null) {
                dominantStatus = session.getStatus();
            }
        }

        private int resolveDurationSeconds() {
            if (minOpenTime == null) {
                return 0;
            }
            Date end = ongoing ? new Date() : maxCloseTime;
            if (end == null) {
                return 0;
            }
            long seconds = Math.max(0L, (end.getTime() - minOpenTime.getTime()) / 1000L);
            return seconds > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) seconds;
        }
    }

    private enum ValveTelemetryState {
        OPEN,
        CLOSED,
        INTERMEDIATE,
        MISSING
    }
}
