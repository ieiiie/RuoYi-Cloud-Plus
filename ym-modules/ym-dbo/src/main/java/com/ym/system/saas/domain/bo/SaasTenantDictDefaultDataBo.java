package com.ym.system.saas.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import com.ym.common.core.validate.AddGroup;
import com.ym.common.core.validate.EditGroup;
import com.ym.system.saas.domain.SaasTenantDictDefaultData;

import java.io.Serializable;

/** 租户字典默认值请求对象。 */
@Data
@AutoMapper(target = SaasTenantDictDefaultData.class, reverseConvertGenerate = false)
public class SaasTenantDictDefaultDataBo implements Serializable {
    @NotNull(groups = EditGroup.class, message = "默认值ID不能为空")
    private Long dictCode;

    private Integer dictSort;

    @NotBlank(groups = {AddGroup.class, EditGroup.class}, message = "字典标签不能为空")
    @Size(max = 100, groups = {AddGroup.class, EditGroup.class}, message = "字典标签长度不能超过100个字符")
    private String dictLabel;

    @NotBlank(groups = {AddGroup.class, EditGroup.class}, message = "业务编码不能为空")
    @Size(max = 100, groups = {AddGroup.class, EditGroup.class}, message = "业务编码长度不能超过100个字符")
    private String dictValue;

    @NotBlank(groups = {AddGroup.class, EditGroup.class}, message = "字典类型不能为空")
    @Size(max = 100, groups = {AddGroup.class, EditGroup.class}, message = "字典类型长度不能超过100个字符")
    private String dictType;

    @Size(max = 100, groups = {AddGroup.class, EditGroup.class}, message = "样式属性长度不能超过100个字符")
    private String cssClass;

    @Size(max = 100, groups = {AddGroup.class, EditGroup.class}, message = "列表样式长度不能超过100个字符")
    private String listClass;

    @Pattern(regexp = "^[YN]$", groups = {AddGroup.class, EditGroup.class}, message = "是否默认只能为Y或N")
    private String isDefault;

    @Size(max = 500, groups = {AddGroup.class, EditGroup.class}, message = "备注长度不能超过500个字符")
    private String remark;
}
