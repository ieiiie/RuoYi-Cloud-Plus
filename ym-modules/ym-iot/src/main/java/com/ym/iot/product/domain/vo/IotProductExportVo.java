package com.ym.iot.product.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;

import lombok.Data;

import org.apache.fesod.sheet.annotation.ExcelIgnoreUnannotated;
import org.apache.fesod.sheet.annotation.ExcelProperty;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 物模型产品导出
 *
 * @author ym-cloud
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = IotProductVo.class)
public class IotProductExportVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 产品标识
     */
    @ExcelProperty(value = "产品标识")
    private String productKey;

    /**
     * 产品名称
     */
    @ExcelProperty(value = "产品名称")
    private String productName;

    /**
     * 节点类型
     */
    @ExcelProperty(value = "节点类型")
    private String nodeType;

    /**
     * 网络类型
     */
    @ExcelProperty(value = "网络类型")
    private String netType;

    /**
     * 协议
     */
    @ExcelProperty(value = "协议")
    private String protocol;

    /**
     * 设备大类
     */
    @ExcelProperty(value = "设备大类")
    private String deviceCategory;

    /**
     * 状态
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

    /**
     * 地图默认图标 URL
     */
    @ExcelProperty(value = "地图图标URL")
    private String mapIconUrl;

    /**
     * 选中态地图图标 URL
     */
    @ExcelProperty(value = "选中态图标URL")
    private String mapSelectedIconUrl;
}
