package com.ym.agriculture.farming.farmrecord.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;

/**
 * 农事类型主数据，表 {@code sf_farming_record_type}。
 * <p>租户、审计字段见 {@link TenantEntity}。</p>
 *
 * @author ym-cloud
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sf_farming_record_type")
public class SfFarmingRecordType extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 类型主键 */
    @TableId("type_id")
    private Long typeId;

    /** 租户内唯一编码（字母数字建议） */
    private String typeCode;

    /** 展示名称 */
    private String typeName;

    /** 列表/下拉图标 OSS URL */
    private String listIconUrl;

    /** 排序序号，越小越靠前 */
    private Integer sortOrder;

    /** {@code 0} 启用；{@code 1} 停用 */
    private String status;

    /** 逻辑删除标记 */
    @TableLogic
    private String delFlag;

    /** 内部备注说明 */
    private String remark;
}
