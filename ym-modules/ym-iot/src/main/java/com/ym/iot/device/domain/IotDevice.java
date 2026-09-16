package com.ym.iot.device.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.tenant.core.TenantEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 物联网设备实体，对应表 {@code iot_device}。
 * <p>
 * 归属于产品，deviceCode 租户内唯一。status 档案状态（0 正常，1 停用），
 * onlineStatus 在线状态（ONLINE/OFFLINE/FAULT）。支持经纬度、imei、mac 等扩展。
 * </p>
 *
 * @author ym-cloud
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("iot_device")
public class IotDevice extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "device_id")
    private Long deviceId;

    private Long productId;

    /** 设备编号（租户内唯一） */
    private String deviceCode;

    /** 设备名称 */
    private String deviceName;

    /** 排序值（升序） */
    private Integer sortOrder;

    /** 设备密钥 */
    private String deviceSecret;

    private String imei;

    private String mac;

    /** 设备大类 */
    private String deviceCategory;

    /** 经度 */
    private BigDecimal lng;

    /** 纬度 */
    private BigDecimal lat;

    /** 在线状态：ONLINE/OFFLINE/FAULT */
    private String onlineStatus;

    private Date lastReportTime;

    /** 平台侧最后一次成功从远程拉取到遥测列表的时间（如 HFZK），仅运维观测，不参与离线判定 */
    private Date lastRemoteFetchTime;

    private Date lastOfflineTime;

    private String ipAddress;

    private String protocol;

    private String firmwareVersion;

    /** 扩展配置JSON */
    private String configJson;

    /** 档案状态：0=正常，1=停用 */
    private String status;

    @TableLogic
    private String delFlag;

    private String remark;

    public IotDevice(Long deviceId) {
        this.deviceId = deviceId;
    }
}
