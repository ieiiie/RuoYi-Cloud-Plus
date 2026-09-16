package com.ym.iot.product.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.tenant.core.TenantEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 物联网物模型属性实体，对应表 {@code iot_product_property}。
 * <p>
 * 定义物模型产品的属性（如温度、湿度等），identifier 在同产品内唯一。
 * 数据类型支持 INT/FLOAT/STRING/ENUM/BOOL/ARRAY/OBJECT，读写权限 R/W。
 * </p>
 *
 * @author ym-cloud
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("iot_product_property")
public class IotProductProperty extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "property_id")
    private Long propertyId;

    /** 所属产品主键 */
    private Long productId;

    /** 属性标识（英文键，同产品内唯一） */
    private String identifier;

    /** 属性显示名称 */
    private String name;

    /** 数据类型：INT/FLOAT/STRING/ENUM/BOOL/ARRAY/OBJECT */
    private String dataType;

    /** 物理单位 */
    private String unit;

    /** 读写权限：R只读/W可写 */
    private String rwFlag;

    /** 步长或精度 */
    private BigDecimal step;

    /** 排序号 */
    private Integer sortOrder;

    @TableLogic
    private String delFlag;

    private String remark;

    public IotProductProperty(Long propertyId) {
        this.propertyId = propertyId;
    }
}
