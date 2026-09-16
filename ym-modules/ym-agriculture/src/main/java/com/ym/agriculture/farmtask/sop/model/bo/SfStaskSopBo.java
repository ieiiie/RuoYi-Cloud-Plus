package com.ym.agriculture.farmtask.sop.model.bo;

import com.ym.common.core.validate.AddGroup;
import com.ym.common.core.validate.EditGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/** 农事 SOP 新增与编辑入参。 */
@Data
public class SfStaskSopBo {

    /** SOP 主键；详情回填使用，编辑以 URL 中的 sopId 为准。 */
    private Long sopId;

    /** 农事项目 ID，必须是启用叶子节点。 */
    @NotNull(message = "农事项目不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long workItemId;

    /** 作物范围：ALL/SPECIFIC。 */
    @NotBlank(message = "作物范围不能为空", groups = {AddGroup.class, EditGroup.class})
    private String cropScope;

    /** 指定物种时必填；全部作物时必须为空。 */
    private Long cropSpeciesId;

    /** 内容语言：zh-CN/ug-CN。 */
    @NotBlank(message = "语言不能为空", groups = {AddGroup.class, EditGroup.class})
    private String language;

    /** 有序 SOP 内容块。 */
    @NotEmpty(message = "SOP内容不能为空", groups = {AddGroup.class, EditGroup.class})
    private List<Map<String, Object>> contentBlocks;
}
