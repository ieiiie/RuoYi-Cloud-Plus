package com.ym.system.saas.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.ym.system.saas.domain.SaasTenantDictDefaultData;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 租户字典默认值视图。 */
@Data
@AutoMapper(target = SaasTenantDictDefaultData.class)
public class SaasTenantDictDefaultDataVo implements Serializable {
    private Long dictCode;
    private Integer dictSort;
    private String dictLabel;
    private String dictValue;
    private String dictType;
    private String cssClass;
    private String listClass;
    private String isDefault;
    private String remark;
    private LocalDateTime createTime;
}
