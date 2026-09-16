package com.ym.iot.product.domain.vo;

import com.ym.iot.product.domain.IotProduct;

import io.github.linpeilie.annotations.AutoMapper;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 物模型产品视图对象 iot_product
 *
 * @author ym-cloud
 */
@Data
@AutoMapper(target = IotProduct.class)
public class IotProductVo implements Serializable {
    /** Optional core modifyTime version for optimistic updates across both UIs. */
    private Long version;


    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 产品主键
     */
    private Long productId;

    /**
     * 租户编号
     */
    private String tenantId;

    /**
     * 产品标识
     */
    private String productKey;

    /**
     * 产品名称
     */
    private String productName;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 网络类型
     */
    private String netType;

    /**
     * 接入协议
     */
    private String protocol;

    /**
     * 数据格式
     */
    private String dataFormat;

    /**
     * 设备大类
     */
    private String deviceCategory;

    /**
     * 状态（0 开发中，1 已发布）
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

    /**
     * 地图设备点位默认图标 URL
     */
    private String mapIconUrl;

    /**
     * 选中态地图图标 URL（可选）
     */
    private String mapSelectedIconUrl;
}
