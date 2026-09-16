package com.ym.agriculture.farming.bigscreen.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 大屏告警信息面板。
 */
@Data
public class SfBigscreenAlertVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 待处理告警数 */
    private Long pendingCount;

    /** 告警列表 */
    private List<SfBigscreenAlertItemVo> items = new ArrayList<>();
}
