package com.ym.iot.product.domain.bo;

import com.ym.common.core.validate.AddGroup;
import com.ym.common.core.validate.EditGroup;
import com.ym.common.mybatis.core.domain.BaseEntity;
import com.ym.iot.product.domain.IotProductProperty;

import io.github.linpeilie.annotations.AutoMapper;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 物模型属性业务对象 iot_product_property
 *
 * @author ym-cloud
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = IotProductProperty.class, reverseConvertGenerate = false)
public class IotProductPropertyBo extends BaseEntity {
    /** Optional core modifyTime version for optimistic updates across both UIs. */
    private Long version;


    /**
     * 属性主键（修改必填）
     */
    @NotNull(message = "属性ID不能为空", groups = { EditGroup.class })
    private Long propertyId;

    /**
     * 所属产品 ID（新增必填）
     */
    @NotNull(message = "产品ID不能为空", groups = { AddGroup.class })
    private Long productId;

    /**
     * 属性标识 identifier（新增必填）
     */
    @NotBlank(message = "属性标识不能为空", groups = { AddGroup.class })
    @Size(max = 64)
    private String identifier;

    /**
     * 属性名称（新增必填）
     */
    @NotBlank(message = "属性名称不能为空", groups = { AddGroup.class })
    @Size(max = 100)
    private String name;

    /**
     * 数据类型（新增必填）
     */
    @NotBlank(message = "数据类型不能为空", groups = { AddGroup.class })
    @Size(max = 16)
    private String dataType;

    /**
     * 单位
     */
    @Size(max = 16)
    private String unit;

    /**
     * 读写标志
     */
    @Size(max = 1)
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
     * 备注
     */
    private String remark;
}
