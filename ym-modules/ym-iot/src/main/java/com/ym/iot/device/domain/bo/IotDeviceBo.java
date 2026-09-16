package com.ym.iot.device.domain.bo;

import com.ym.common.core.validate.AddGroup;
import com.ym.common.core.validate.EditGroup;
import com.ym.common.mybatis.core.domain.BaseEntity;
import com.ym.iot.device.domain.IotDevice;

import io.github.linpeilie.annotations.AutoMapper;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 物联网设备业务对象 iot_device
 *
 * @author ym-cloud
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = IotDevice.class, reverseConvertGenerate = false)
public class IotDeviceBo extends BaseEntity {
    /** Optional core modifyTime version for optimistic updates across both UIs. */
    private Long version;


    /**
     * 设备主键（修改必填）
     */
    @NotNull(message = "设备ID不能为空", groups = { EditGroup.class })
    private Long deviceId;

    /**
     * 所属产品 ID（iot_product.product_id）
     */
    private Long productId;

    /**
     * 设备编号（租户内唯一，新增必填）
     */
    @NotBlank(message = "设备编号不能为空", groups = { AddGroup.class })
    @Size(max = 64)
    private String deviceCode;

    /**
     * 设备名称（可选；未传时新增默认取设备编号）
     */
    @Size(max = 100)
    private String deviceName;

    /**
     * 排序值（升序，默认 0）
     */
    @Min(value = 0, message = "排序值不能小于0")
    private Integer sortOrder;

    /**
     * 设备大类（可选；未传时优先继承产品大类，否则默认为 OTHER）
     */
    @Size(max = 64)
    private String deviceCategory;

    /**
     * IMEI
     */
    @Size(max = 50)
    private String imei;

    /**
     * MAC 地址
     */
    @Size(max = 50)
    private String mac;

    /**
     * 经度
     */
    private BigDecimal lng;

    /**
     * 纬度
     */
    private BigDecimal lat;

    /**
     * 接入协议
     */
    @Size(max = 32)
    private String protocol;

    /**
     * 固件版本
     */
    @Size(max = 32)
    private String firmwareVersion;

    /**
     * 扩展配置 JSON
     */
    private String configJson;

    /**
     * 档案状态（0 正常，1 停用）
     */
    @Size(max = 1)
    private String status;

    /**
     * 在线状态（查询条件：ONLINE/OFFLINE/FAULT）
     */
    @Size(max = 16)
    private String onlineStatus;

    /**
     * 备注
     */
    private String remark;

    /**
     * 设备编号精确列表（查询用；非空时 IN 查询，优先于 deviceCode 模糊）
     */
    private List<String> deviceCodeList;
}
