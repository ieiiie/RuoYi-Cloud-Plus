package com.ym.iot.product.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.tenant.core.TenantEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;

/**
 * 物联网物模型产品实体，对应表 {@code iot_product}。
 * <p>
 * 物模型产品定义设备类型（如传感器、网关），一个产品下有多个物模型属性。
 * 继承 {@link TenantEntity} 支持多租户。逻辑删除由 delFlag 控制。
 * </p>
 *
 * @author ym-cloud
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("iot_product")
public class IotProduct extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "product_id")
    private Long productId;

    /** 产品密钥/类型标识（租户内唯一） */
    private String productKey;

    /** 产品名称 */
    private String productName;

    /** 节点类型：DIRECT/GATEWAY/SUB */
    private String nodeType;

    /** 网络类型：WIFI/CELLULAR/ETHERNET/LORA/NBIOT */
    private String netType;

    /** 接入协议 */
    private String protocol;

    /** 数据格式：JSON/BINARY/PROTOBUF */
    private String dataFormat;

    /** 设备大类 */
    private String deviceCategory;

    /** 产品状态：0=开发中，1=已发布 */
    private String status;

    @TableLogic
    private String delFlag;

    private String remark;

    /**
     * 地图设备点位图标 URL。
     */
    private String mapIconUrl;

    /**
     * 选中态地图图标 URL。
     */
    private String mapSelectedIconUrl;

    public IotProduct(Long productId) {
        this.productId = productId;
    }
}
