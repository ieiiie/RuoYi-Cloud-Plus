package com.ym.agriculture.farming.algback.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 算法中台回调确认体：响应 JSON 中 {@code code} 须为 Integer 且等于 200，否则中台视为推送失败。
 *
 * @author ym-cloud
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlgBackCallbackAckVo {

    /**
     * 业务码，成功固定 200
     */
    private Integer code;

    /**
     * 提示信息
     */
    private String msg;

    public static AlgBackCallbackAckVo ok() {
        return new AlgBackCallbackAckVo(200, "ok");
    }

    public static AlgBackCallbackAckVo fail(String msg) {
        return new AlgBackCallbackAckVo(500, msg);
    }
}
