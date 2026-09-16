package com.ym.system.saas.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;
import com.ym.common.core.validate.*;
import com.ym.system.saas.domain.SaasDictType;

import java.io.Serializable;

/**
 * SaaS 字典类型请求对象。
 */
@Data
@AutoMapper(target = SaasDictType.class, reverseConvertGenerate = false)
public class SaasDictTypeBo implements Serializable {
    @NotNull(groups = EditGroup.class)
    private Long dictId;
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    private String dictName;
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    private String dictType;
    private String remark;
}
