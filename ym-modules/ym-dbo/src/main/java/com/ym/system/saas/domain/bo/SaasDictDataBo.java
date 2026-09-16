package com.ym.system.saas.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;
import com.ym.common.core.validate.*;
import com.ym.system.saas.domain.SaasDictData;

import java.io.Serializable;

/**
 * SaaS 字典数据请求对象。
 */
@Data
@AutoMapper(target = SaasDictData.class, reverseConvertGenerate = false)
public class SaasDictDataBo implements Serializable {
    @NotNull(groups = EditGroup.class)
    private Long dictCode;
    private Integer dictSort;
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    private String dictLabel;
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    private String dictValue;
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    private String dictType;
    private String cssClass;
    private String listClass;
    private String isDefault;
    private String remark;
}
