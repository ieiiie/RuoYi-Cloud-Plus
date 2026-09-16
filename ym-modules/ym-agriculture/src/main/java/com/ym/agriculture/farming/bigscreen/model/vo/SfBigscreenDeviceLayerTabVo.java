package com.ym.agriculture.farming.bigscreen.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 大屏地图设备图层 Tab 汇总。
 */
@Data
public class SfBigscreenDeviceLayerTabVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 图层类型：sensor / camera / valve / facility */
    private String layerType;

    /** 图层中文名 */
    private String layerLabel;

    /** 在线数 */
    private Integer onlineCount;

    private Integer offlineCount;

    private Integer unknownCount;

    /** 总数 */
    private Integer totalCount;
}
