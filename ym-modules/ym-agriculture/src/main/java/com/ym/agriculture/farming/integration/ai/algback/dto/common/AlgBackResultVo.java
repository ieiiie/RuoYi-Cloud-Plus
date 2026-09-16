package com.ym.agriculture.farming.integration.ai.algback.dto.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 算法中台统一响应外壳；泛型 {@code T} 为 {@code data} 类型。
 *
 * @author ym-cloud
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlgBackResultVo<T> {

    /** 业务码；{@code 200} 表示成功。 */
    private Integer code;

    /** 提示信息；失败时往往含原因说明。 */
    private String msg;

    /** 业务载荷；校验失败或异常时可能为 {@code null}。 */
    private T data;

    /** 是否成功（{@code code == 200}）。 */
    public boolean isSuccess() {
        return code != null && code == 200;
    }
}
