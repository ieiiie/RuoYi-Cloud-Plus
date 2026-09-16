package com.ym.iot.device.domain.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ym.iot.device.domain.IotDevice;

import io.github.linpeilie.annotations.AutoMapper;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 物联网设备视图对象 iot_device
 *
 * @author ym-cloud
 */
@Data
@AutoMapper(target = IotDevice.class)
public class IotDeviceVo implements Serializable {
    /** Optional core modifyTime version for optimistic updates across both UIs. */
    private Long version;


    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 设备主键
     */
    private Long deviceId;

    /**
     * 租户编号
     */
    private String tenantId;

    /**
     * 所属产品 ID
     */
    private Long productId;

    /**
     * 产品名称（关联查询）
     */
    private String productName;

    /**
     * 产品密钥/设备类型标识（关联查询）
     */
    private String productKey;

    /**
     * 设备编号
     */
    private String deviceCode;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 排序值（升序）
     */
    private Integer sortOrder;

    /**
     * 设备密钥（服务端首查映射，仅用于生成 {@link #deviceSecretMask}，不落 JSON）
     */
    @JsonIgnore
    private String deviceSecret;

    /**
     * 设备密钥脱敏（仅后 4 位）
     */
    private String deviceSecretMask;

    /**
     * IMEI
     */
    private String imei;

    /**
     * MAC 地址
     */
    private String mac;

    /**
     * 设备大类
     */
    private String deviceCategory;

    /**
     * 经度
     */
    private BigDecimal lng;

    /**
     * 纬度
     */
    private BigDecimal lat;

    /**
     * 在线状态
     */
    private String onlineStatus;

    /**
     * 最后上报时间
     */
    private Date lastReportTime;

    /**
     * 最后一次远程拉取遥测成功时间（如 HFZK）
     */
    private Date lastRemoteFetchTime;

    /**
     * 最后离线时间
     */
    private Date lastOfflineTime;

    /**
     * IP 地址
     */
    private String ipAddress;

    /**
     * 接入协议
     */
    private String protocol;

    /**
     * 固件版本
     */
    private String firmwareVersion;

    /**
     * 档案状态（0 正常，1 停用）
     */
    private String status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 备注
     */
    private String remark;
}
