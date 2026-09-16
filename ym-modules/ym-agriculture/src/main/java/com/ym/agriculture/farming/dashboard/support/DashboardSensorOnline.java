package com.ym.agriculture.farming.dashboard.support;

import com.ym.iot.api.domain.vo.RemoteDeviceSummaryVo;
import java.util.Collection;
import java.util.Date;

/** JetLinks 是状态权威来源；遥测时间不能将待确认状态推断为在线或离线。 */
public final class DashboardSensorOnline {
    private DashboardSensorOnline() {}

    public static String status(RemoteDeviceSummaryVo device) {
        if (device == null) return "UNKNOWN";
        return normalize(device.getOnlineStatus());
    }

    public static String normalize(String status) {
        return "ONLINE".equals(status) || "OFFLINE".equals(status) ? status : "UNKNOWN";
    }

    /** 保留原布尔接口；false 只表示未确认在线，不能用来统计离线。 */
    public static boolean isOnline(RemoteDeviceSummaryVo device, Date now) {
        return "ONLINE".equals(status(device));
    }

    public static Counts count(Collection<RemoteDeviceSummaryVo> devices) {
        int online = 0, offline = 0, unknown = 0;
        for (RemoteDeviceSummaryVo device : devices) {
            switch (status(device)) {
                case "ONLINE" -> online++;
                case "OFFLINE" -> offline++;
                default -> unknown++;
            }
        }
        return new Counts(online, offline, unknown);
    }

    public record Counts(int online, int offline, int unknown) {
        public int total() { return online + offline + unknown; }
        public int confirmed() { return online + offline; }
        public int pct() { return confirmed() == 0 ? 0 : (int) Math.round(online * 100.0 / confirmed()); }
    }
}
