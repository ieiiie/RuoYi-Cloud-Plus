package com.ym.agriculture.api.farming.domain.vo;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/** 农业移动端兼容视图，不暴露领域内部类型。 */
public class RemoteAgricultureViewVo extends LinkedHashMap<String, Object> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public RemoteAgricultureViewVo() {
        super();
    }

    public RemoteAgricultureViewVo(Map<String, Object> source) {
        super(source == null ? Map.of() : source);
    }
}
