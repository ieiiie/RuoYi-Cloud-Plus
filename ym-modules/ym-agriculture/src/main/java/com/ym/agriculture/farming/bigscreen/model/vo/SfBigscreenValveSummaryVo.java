package com.ym.agriculture.farming.bigscreen.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 大屏阀门底栏汇总。
 */
@Data
public class SfBigscreenValveSummaryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 阀门总数 */
    private Integer totalCount;

    /** 在线数 */
    private Integer onlineCount;

    private Integer offlineCount;

    private Integer unknownCount;

    /** 当前在线且按设备有效阀口判定为开启的设备数（离线设备不计入） */
    private Integer openCount;

    /** 是否灌溉中：任一阀门在线且瞬时流量大于 0 */
    private Boolean irrigating;

    /** 当前有效开启会话最大持续秒数（离线及已被更新遥测否定的 OPEN 会话不计入） */
    private Integer maxOpenDurationSeconds;

    /** 阀门列表 */
    private List<SfBigscreenValveItemVo> valves = new ArrayList<>();

    /** 最近聚合灌溉记录 */
    private List<SfBigscreenValveHistoryVo> recentHistory = new ArrayList<>();
}
