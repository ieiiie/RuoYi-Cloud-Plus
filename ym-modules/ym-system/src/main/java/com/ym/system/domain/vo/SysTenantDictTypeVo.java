package com.ym.system.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.ym.system.domain.SysTenantDictType;

import java.time.LocalDateTime;

/** 租户字典类型视图。 */
@Data
@AutoMapper(target = SysTenantDictType.class)
public class SysTenantDictTypeVo {
    private Long dictId;
    private String dictName;
    private String dictType;
    private String remark;
    private LocalDateTime createTime;
}
