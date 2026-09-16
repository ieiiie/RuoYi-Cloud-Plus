package com.ym.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import com.ym.common.core.validate.AddGroup;
import com.ym.common.core.validate.EditGroup;
import com.ym.system.domain.SysTenantDictData;

/** 当前租户字典值请求对象。 */
@Data
@AutoMapper(target = SysTenantDictData.class, reverseConvertGenerate = false)
public class SysTenantDictDataBo {
    @NotNull(groups = EditGroup.class, message = "字典编码不能为空")
    private Long dictCode;

    private Integer dictSort;

    @NotBlank(groups = {AddGroup.class, EditGroup.class}, message = "字典标签不能为空")
    @Size(max = 100, groups = {AddGroup.class, EditGroup.class}, message = "字典标签长度不能超过100个字符")
    private String dictLabel;

    private String dictValue;

    @NotBlank(groups = {AddGroup.class, EditGroup.class}, message = "字典类型不能为空")
    @Size(max = 100, groups = {AddGroup.class, EditGroup.class}, message = "字典类型长度不能超过100个字符")
    private String dictType;

    @Size(max = 100, groups = {AddGroup.class, EditGroup.class}, message = "样式属性长度不能超过100个字符")
    private String cssClass;

    @Size(max = 100, groups = {AddGroup.class, EditGroup.class}, message = "列表样式长度不能超过100个字符")
    private String listClass;

    private String isDefault;

    @Size(max = 500, groups = {AddGroup.class, EditGroup.class}, message = "备注长度不能超过500个字符")
    private String remark;
}
