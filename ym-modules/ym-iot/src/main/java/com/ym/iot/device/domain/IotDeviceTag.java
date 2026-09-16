package com.ym.iot.device.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 物联网设备标签实体，对应表 {@code iot_device_tag}。
 * <p>
 * deviceId+tagKey 唯一，用于扩展设备元数据（如 location、batch）。
 * 表结构无 del_flag，不继承 TenantEntity。
 * </p>
 *
 * @author ym-cloud
 */
@Data
@TableName("iot_device_tag")
public class IotDeviceTag implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "tag_id")
    private Long tagId;

    private String tenantId;

    private Long deviceId;

    private String tagKey;

    private String tagValue;

    private Date createTime;

    private Date updateTime;

    public IotDeviceTag() {
    }

    public IotDeviceTag(Long tagId) {
        this.tagId = tagId;
    }
}
