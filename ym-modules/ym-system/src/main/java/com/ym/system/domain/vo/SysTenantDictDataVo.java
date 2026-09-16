package com.ym.system.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.ym.system.domain.SysTenantDictData;

import java.time.LocalDateTime;

/** 当前租户字典值视图。 */
@Data
@AutoMapper(target = SysTenantDictData.class)
public class SysTenantDictDataVo {
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
