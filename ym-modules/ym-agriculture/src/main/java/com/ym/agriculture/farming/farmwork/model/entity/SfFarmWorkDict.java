package com.ym.agriculture.farming.farmwork.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;

/**
 * stask 农事分类与项目字典，表 {@code sf_farm_work_dict}。
 *
 * @author ym-cloud
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sf_farm_work_dict")
public class SfFarmWorkDict extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 农事字典主键。
     */
    @TableId("dict_id")
    private Long dictId;

    /**
     * 父节点ID：0 表示分类；非 0 表示项目所属分类ID。
     */
    private Long parentId;

    /**
     * 节点类型：CATEGORY-分类，ITEM-项目。
     */
    private String nodeType;

    /**
     * 名称，分类名称或项目名称，最大 50 个字符。
     */
    private String dictName;

    /**
     * 编码，分类与项目在同一租户内共用唯一命名空间，最大 20 个字符。
     */
    private String dictCode;

    /**
     * 最少工人数，仅项目有效，允许 0。
     */
    private Integer minWorkers;

    /**
     * 最多工人数，仅项目有效；未配置时默认 30，无固定上限。
     */
    private Integer maxWorkers;

    /**
     * 是否需要领料，仅 ITEM 节点有效。
     */
    private Boolean requiresMaterial;

    /**
     * 状态：0-启用，1-停用，仅项目有效.
     */
    private String status;

    /**
     * 自定义表单模板 JSON，仅项目有效。
     */
    private String customFormTemplateJson;

    /**
     * 同级排序序号，越小越靠前。
     */
    private Integer sortOrder;

    /**
     * 逻辑删除标记：0-存在，1-删除。
     */
    @TableLogic
    private String delFlag;

    /**
     * 备注，可记录内部维护说明。
     */
    private String remark;
}
