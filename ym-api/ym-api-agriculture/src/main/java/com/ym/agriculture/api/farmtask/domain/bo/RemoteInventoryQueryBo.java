package com.ym.agriculture.api.farmtask.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/** 库存 V1.5 查询契约，兼容历史筛选字段。 */
@Data
public class RemoteInventoryQueryBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private Map<String, Object> filters = new LinkedHashMap<>();
}
