package com.ym.system.saas.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import com.ym.common.core.validate.AddGroup;
import com.ym.common.core.validate.EditGroup;
import com.ym.system.saas.domain.SaasTenantDictType;

import java.io.Serializable;

/** 租户字典类型请求对象。 */
@Data
@AutoMapper(target = SaasTenantDictType.class, reverseConvertGenerate = false)
public class SaasTenantDictTypeBo implements Serializable {
    @NotNull(groups = EditGroup.class, message = "字典ID不能为空")
    private Long dictId;

    @NotBlank(groups = {AddGroup.class, EditGroup.class}, message = "字典名称不能为空")
    @Size(max = 100, groups = {AddGroup.class, EditGroup.class}, message = "字典名称长度不能超过100个字符")
    private String dictName;

    @NotBlank(groups = AddGroup.class, message = "字典类型不能为空")
    @Size(max = 100, groups = AddGroup.class, message = "字典类型长度不能超过100个字符")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$", groups = AddGroup.class,
        message = "字典类型必须以字母开头，只能包含字母、数字和下划线")
    private String dictType;

    @Size(max = 500, groups = {AddGroup.class, EditGroup.class}, message = "备注长度不能超过500个字符")
    private String remark;
}
