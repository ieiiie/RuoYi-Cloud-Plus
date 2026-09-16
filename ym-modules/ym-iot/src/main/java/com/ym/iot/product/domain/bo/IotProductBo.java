package com.ym.iot.product.domain.bo;

import com.ym.common.core.validate.AddGroup;
import com.ym.common.core.validate.EditGroup;
import com.ym.common.mybatis.core.domain.BaseEntity;
import com.ym.iot.product.domain.IotProduct;

import io.github.linpeilie.annotations.AutoMapper;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 物模型产品业务对象 iot_product
 *
 * @author ym-cloud
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = IotProduct.class, reverseConvertGenerate = false)
public class IotProductBo extends BaseEntity {
    /** Optional core modifyTime version for optimistic updates across both UIs. */
    private Long version;


    /**
     * 产品主键（修改必填）
     */
    @NotNull(message = "产品ID不能为空", groups = { EditGroup.class })
    private Long productId;

    /**
     * 产品标识 productKey（租户内唯一，新增必填）
     */
    @NotBlank(message = "产品标识不能为空", groups = { AddGroup.class })
    @Size(max = 64)
    private String productKey;

    /**
     * 产品名称（新增必填）
     */
    @NotBlank(message = "产品名称不能为空", groups = { AddGroup.class })
    @Size(max = 100)
    private String productName;

    /**
     * 节点类型
     */
    @Size(max = 16)
    private String nodeType;

    /**
     * 网络类型
     */
    @Size(max = 16)
    private String netType;

    /**
     * 接入协议
     */
    @Size(max = 32)
    private String protocol;

    /**
     * 数据格式
     */
    @Size(max = 16)
    private String dataFormat;

    /**
     * 设备大类
     */
    @Size(max = 64)
    private String deviceCategory;

    /**
     * 状态（0 开发中，1 已发布）
     */
    @Size(max = 1)
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 地图设备点位默认图标 URL（OSS/HTTPS，最长 512 字符）
     */
    @Size(max = 512)
    private String mapIconUrl;

    /**
     * 选中态地图图标 URL（可选；为空时前端可复用 mapIconUrl）
     */
    @Size(max = 512)
    private String mapSelectedIconUrl;
}
