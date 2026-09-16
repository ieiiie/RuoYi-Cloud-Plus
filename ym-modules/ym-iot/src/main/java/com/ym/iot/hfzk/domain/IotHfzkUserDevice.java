package com.ym.iot.hfzk.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
@TableName("iot_hfzk_user_device")
public class IotHfzkUserDevice implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String externalDeviceId;

    private String deviceSn;

    private String deviceType;

    private String deviceName;

    private String longitude;

    private String latitude;

    private String externalUserId;

    private String remark;

    /** 设备扩展信息 JSON（WVP_CAMERA 写入通道快照数组；schema 见 SQL 注释） */
    private String extraInfo;

    private Date lastSyncTime;

    private Date createTime;

    private Date updateTime;

    private String tenantId;
}
