package com.ym.system.domain.bo;

import lombok.Data;

/** 租户字典类型查询条件。 */
@Data
public class SysTenantDictTypeBo {
    private String dictName;
    private String dictType;
}
