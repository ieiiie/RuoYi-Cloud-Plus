package com.ym.iot.product.domain.vo;

import com.ym.iot.product.domain.IotProductProperty;

import io.github.linpeilie.annotations.AutoMapper;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 物模型属性视图对象 iot_product_property
 *
 * @author ym-cloud
 */
@Data
@AutoMapper(target = IotProductProperty.class)
public class IotProductPropertyVo implements Serializable {
    /** Optional core modifyTime version for optimistic updates across both UIs. */
    private Long version;


    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 属性主键
     */
    private Long propertyId;

    /**
     * 租户编号
     */
    private String tenantId;

    /**
     * 产品主键
     */
    private Long productId;

    /**
     * 属性标识
     */
    private String identifier;

    /**
     * 属性名称
     */
    private String name;

    /**
     * 数据类型
     */
    private String dataType;

    /**
     * 单位
     */
    private String unit;

    /**
     * 读写标志
     */
    private String rwFlag;

    /**
     * 步长
     */
    private BigDecimal step;

    /**
     * 排序号
     */
    private Integer sortOrder;

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
