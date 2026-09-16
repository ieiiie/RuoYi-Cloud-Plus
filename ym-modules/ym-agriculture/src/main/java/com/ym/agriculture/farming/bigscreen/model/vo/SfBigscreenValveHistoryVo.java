package com.ym.agriculture.farming.bigscreen.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 单设备灌溉/开阀记录（recentHistory 每条对应一台设备一次周期；多阀合并统计）。
 */
@Data
public class SfBigscreenValveHistoryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 设备主键 */
    private Long deviceId;

    /** 设备编号 */
    private String deviceCode;

    /** 设备名称 */
    private String deviceName;

    /** 本次参与打开的阀数 */
    private Integer openValveCount;

    /** 开阀时间（多阀取最早） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date openTime;

    /** 关阀时间（多阀取最晚；进行中为 null） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date closeTime;

    /** 列表排序/展示时间（已结束=关阀时间，进行中=开阀时间） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date operatedAt;

    /** 持续秒数（阀门开启跨度；灌溉时长在无流量时序时同此口径） */
    private Integer durationSeconds;

    /** 持续分钟数（展示用，四舍五入） */
    private Integer durationMinutes;

    /** 当前瞬时流量 m³/h（进行中时有值） */
    private BigDecimal flowRateM3h;

    /** 是否灌溉中：在线且瞬时流量大于 0 */
    private Boolean irrigating;

    /** 会话状态：OPEN=进行中；CLOSED/ABORTED=已结束 */
    private String status;

    /** 是否进行中（仍有阀未关） */
    private Boolean ongoing;

    /** 展示文案：灌溉中=「{设备} 灌溉 N分钟」；否则=「{设备} 阀门开启 N分钟」 */
    private String summaryText;
}
