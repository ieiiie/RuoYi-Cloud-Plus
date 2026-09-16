package com.ym.agriculture.api.farming.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/** 农业小程序通用查询契约。 */
@Data
public class RemoteAgricultureMobileQueryBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private Map<String, Object> filters = new LinkedHashMap<>();
}
