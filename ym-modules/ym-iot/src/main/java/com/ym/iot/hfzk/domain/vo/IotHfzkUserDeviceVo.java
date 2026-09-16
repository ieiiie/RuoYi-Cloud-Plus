package com.ym.iot.hfzk.domain.vo;

import com.ym.iot.hfzk.domain.IotHfzkUserDevice;

import io.github.linpeilie.annotations.AutoMapper;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 中科合肥（HFZK）网关同步下来的用户设备档案视图对象。
 * <p>
 * 对应表 {@code iot_hfzk_user_device}，数据由定时任务
 * {@link com.ym.iot.hfzk.job.HfzkUserDeviceSyncJob} 调用网关接口拉取后写入。
 * 接口层按当前登录租户过滤，仅返回本租户下的记录。
 * </p>
 *
 * @author ym-cloud
 */
@Data
@AutoMapper(target = IotHfzkUserDevice.class)
public class IotHfzkUserDeviceVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 网关侧设备唯一标识（HFZK externalDeviceId，与平台 {@code iot_device} 对齐时用作关联键之一） */
    private String externalDeviceId;

    /** 设备序列号 SN（遥测拉取等常以此与平台 device_code 对齐） */
    private String deviceSn;

    /** 设备类型（网关返回的类型编码或描述） */
    private String deviceType;

    /** 设备名称（展示用） */
    private String deviceName;

    /** 经度（网关侧字符串形式） */
    private String longitude;

    /** 纬度（网关侧字符串形式） */
    private String latitude;

    /** 网关侧用户标识（HFZK 用户维度） */
    private String externalUserId;

    /** 备注 */
    private String remark;

    /** 最近一次从网关成功同步该设备档案的时间 */
    private Date lastSyncTime;

    /** 本地记录创建时间 */
    private Date createTime;

    /** 本地记录更新时间 */
    private Date updateTime;

    /** 租户编号，与平台多租户一致 */
    private String tenantId;
}
