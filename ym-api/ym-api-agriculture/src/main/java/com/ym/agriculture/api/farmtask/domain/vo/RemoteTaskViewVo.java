package com.ym.agriculture.api.farmtask.domain.vo;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 农事任务兼容视图。
 *
 * <p>字段保持源移动端 JSON 的顶层结构，避免 BFF 再包一层 data；该类型不包含领域 Entity。</p>
 */
public class RemoteTaskViewVo extends LinkedHashMap<String, Object> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public RemoteTaskViewVo() {
        super();
    }

    public RemoteTaskViewVo(Map<String, Object> source) {
        super(source == null ? Map.of() : source);
    }
}
