package com.ym.agriculture.farming.farmwork.model.vo;

import com.ym.agriculture.farming.farmwork.model.entity.SfFarmWorkDict;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * stask 农事字典节点出参。
 *
 * @author ym-cloud
 */
@Data
@AutoMapper(target = SfFarmWorkDict.class)
public class SfFarmWorkDictVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 农事字典主键。
     */
    private Long dictId;

    /**
     * 租户编号。
     */
    private String tenantId;

    /**
     * 父节点ID：0 表示分类；非 0 表示项目所属分类ID。
     */
    private Long parentId;

    /**
     * 节点类型：CATEGORY-分类，ITEM-项目。
     */
    private String nodeType;

    /**
     * 名称，分类名称或项目名称。
     */
    private String dictName;

    /**
     * 编码，租户内分类与项目共用唯一命名空间。
     */
    private String dictCode;

    /**
     * 最少工人数，仅项目有效。
     */
    private Integer minWorkers;

    /**
     * 最多工人数，仅项目有效；未配置时默认 30，无固定上限。
     */
    private Integer maxWorkers;

    /**
     * 是否需要领料，仅项目节点可能为 true。
     */
    private Boolean requiresMaterial;

    /**
     * 状态：0-启用，1-停用，仅项目有效.
     */
    private String status;

    /**
     * 自定义表单模板，仅项目有效。
     */
    private Object customFormTemplate;

    /**
     * 同级排序序号。
     */
    private Integer sortOrder;

    /**
     * 创建时间。
     */
    private Date createTime;

    /**
     * 更新时间。
     */
    private Date updateTime;

    /**
     * 备注。
     */
    private String remark;
}
