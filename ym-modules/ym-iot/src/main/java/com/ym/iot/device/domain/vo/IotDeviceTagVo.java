package com.ym.iot.device.domain.vo;

import com.ym.iot.device.domain.IotDeviceTag;

import io.github.linpeilie.annotations.AutoMapper;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 设备标签视图对象 iot_device_tag
 *
 * @author ym-cloud
 */
@Data
@AutoMapper(target = IotDeviceTag.class)
public class IotDeviceTagVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 标签主键
     */
    private Long tagId;

    /**
     * 租户编号
     */
    private String tenantId;

    /**
     * 设备主键
     */
    private Long deviceId;

    /**
     * 标签键
     */
    private String tagKey;

    /**
     * 标签值
     */
    private String tagValue;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
