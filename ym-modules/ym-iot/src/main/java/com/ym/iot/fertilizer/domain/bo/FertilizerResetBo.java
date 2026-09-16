package com.ym.iot.fertilizer.domain.bo;

import lombok.Data;

/**
 * 施肥机复位请求。
 *
 * @author ym-cloud
 */
@Data
public class FertilizerResetBo {

    /**
     * 是否确认继续上一次未完成施肥。
     */
    private Boolean continuePending;
}
