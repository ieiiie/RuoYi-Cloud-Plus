package com.ym.iot.device.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;

import lombok.Data;

import org.apache.fesod.sheet.annotation.ExcelProperty;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 物联网设备导出
 *
 * @author ym-cloud
 */
@Data
@AutoMapper(target = IotDeviceVo.class)
public class IotDeviceExportVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 设备编号
     */
    @ExcelProperty(value = "设备编号")
    private String deviceCode;

    /**
     * 设备名称
     */
    @ExcelProperty(value = "设备名称")
    private String deviceName;

    /**
     * 产品名称
     */
    @ExcelProperty(value = "产品名称")
    private String productName;

    /**
     * 设备大类
     */
    @ExcelProperty(value = "设备大类")
    private String deviceCategory;

    /**
     * 在线状态
     */
    @ExcelProperty(value = "在线状态")
    private String onlineStatus;

    /**
     * 经度
     */
    @ExcelProperty(value = "经度")
    private BigDecimal lng;

    /**
     * 纬度
     */
    @ExcelProperty(value = "纬度")
    private BigDecimal lat;

    /**
     * 固件版本
     */
    @ExcelProperty(value = "固件版本")
    private String firmwareVersion;

    /**
     * 档案状态
     */
    @ExcelProperty(value = "状态")
    private String status;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private Date updateTime;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;
}
