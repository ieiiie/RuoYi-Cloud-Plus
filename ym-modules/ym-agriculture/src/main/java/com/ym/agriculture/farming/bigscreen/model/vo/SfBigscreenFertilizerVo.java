package com.ym.agriculture.farming.bigscreen.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 大屏水肥机底栏数据。
 */
@Data
public class SfBigscreenFertilizerVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 设备编号 */
    private String deviceCode;

    /** 设备名称 */
    private String deviceName;

    /** 产品标识 */
    private String productKey;

    /** 是否在线 */
    private Boolean online;

    /** ONLINE / OFFLINE / UNKNOWN；待确认不能视为离线。 */
    private String onlineStatus;

    /** 是否施肥中（MQTT 实时态，离线时为 null） */
    private Boolean fertilizing;

    /** 三罐液位（固定 3 项，罐号 1/2/3） */
    private List<SfBigscreenFertilizerTankVo> tanks = new ArrayList<>();

    /** 最近施肥记录（按天聚合，磷/钾/氮当日合计） */
    private List<SfBigscreenFertilizerHistoryVo> recentRecords = new ArrayList<>();
}
