package com.ym.agriculture.api.farmtask.domain.vo;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 库存 V1.5 兼容视图。
 *
 * <p>保留原小程序 JSON 字段，但不向 BFF 暴露领域 Entity、Mapper 或 Service 类型。</p>
 */
public class RemoteInventoryViewVo extends LinkedHashMap<String, Object> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public RemoteInventoryViewVo() {
        super();
    }

    public RemoteInventoryViewVo(Map<String, Object> source) {
        super(source == null ? Map.of() : source);
    }
}
