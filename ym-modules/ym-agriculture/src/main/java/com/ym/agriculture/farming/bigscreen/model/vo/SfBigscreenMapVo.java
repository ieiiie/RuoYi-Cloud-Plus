package com.ym.agriculture.farming.bigscreen.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 大屏数字孪生地图数据。
 */
@Data
public class SfBigscreenMapVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 地块列表 */
    private List<SfBigscreenMapFieldVo> fields = new ArrayList<>();

    /** 全租户设备点 */
    private List<SfBigscreenDeviceVo> devices = new ArrayList<>();

    /** 图层 Tab 汇总 */
    private List<SfBigscreenDeviceLayerTabVo> layerTabs = new ArrayList<>();

    /** 租户默认水肥机设备编号（product_key=FERTILIZER），供水肥底栏请求使用 */
    private String defaultFertilizerDeviceCode;

    /** 租户默认无人机/机场设备编号（layerType=uav 的首台） */
    private String defaultUavDeviceCode;
}
