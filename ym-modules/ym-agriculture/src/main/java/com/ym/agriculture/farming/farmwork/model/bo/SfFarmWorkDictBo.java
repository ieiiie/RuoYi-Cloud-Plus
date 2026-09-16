package com.ym.agriculture.farming.farmwork.model.bo;

import com.ym.common.core.validate.AddGroup;
import com.ym.common.core.validate.EditGroup;
import com.ym.common.mybatis.core.domain.BaseEntity;
import com.ym.agriculture.farming.farmwork.model.entity.SfFarmWorkDict;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * stask 农事字典新增/编辑入参。
 *
 * @author ym-cloud
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SfFarmWorkDict.class, reverseConvertGenerate = false)
public class SfFarmWorkDictBo extends BaseEntity {

    /**
     * 农事字典主键，编辑时由路径或请求体传入。
     */
    @NotNull(message = "农事字典ID不能为空", groups = {EditGroup.class})
    private Long dictId;

    /**
     * 父节点ID：分类传 0，项目传所属分类ID。
     */
    @NotNull(message = "父节点ID不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long parentId;

    /**
     * 节点类型：CATEGORY-分类，ITEM-项目。
     */
    @NotBlank(message = "节点类型不能为空", groups = {AddGroup.class, EditGroup.class})
    @Pattern(regexp = "^(CATEGORY|ITEM)$", message = "节点类型只能为CATEGORY或ITEM")
    private String nodeType;

    /**
     * 名称，分类名称或项目名称，最大 50 个字符。
     */
    @NotBlank(message = "名称不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 50, message = "名称长度不能超过{max}个字符")
    private String dictName;

    /**
     * 编码，租户内分类与项目共用唯一命名空间，最大 20 个字符。
     */
    @NotBlank(message = "编码不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 20, message = "编码长度不能超过{max}个字符")
    private String dictCode;

    /**
     * 最少工人数，仅项目有效；为空时项目默认 1。
     */
    @Min(value = 0, message = "最少工人数不能小于{value}")
    private Integer minWorkers;

    /**
     * 最多工人数，仅项目有效；为空时项目默认 30，无固定上限。
     */
    @Min(value = 0, message = "最多工人数不能小于{value}")
    private Integer maxWorkers;

    /**
     * 是否需要领料，仅 ITEM 节点有效；分类节点会被服务端归一为 false。
     */
    private Boolean requiresMaterial;

    /**
     * 状态：0-启用，1-停用；项目为空时默认启用。
     */
    @Pattern(regexp = "^[01]$", message = "状态只能为0或1")
    private String status;

    /**
     * 自定义表单模板，仅项目有效。
     */
    private Map<String, Object> customFormTemplate;

    /**
     * 排序序号，新增为空时自动排到同级末尾。
     */
    private Integer sortOrder;

    /**
     * 备注，可记录内部维护说明。
     */
    @Size(max = 500, message = "备注长度不能超过{max}个字符")
    private String remark;
}
