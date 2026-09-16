package com.ym.system.saas.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.ym.system.saas.domain.SaasTenantDictType;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 租户字典类型视图。 */
@Data
@AutoMapper(target = SaasTenantDictType.class)
public class SaasTenantDictTypeVo implements Serializable {
    private Long dictId;
    private String dictName;
    private String dictType;
    private String remark;
    private LocalDateTime createTime;
}
