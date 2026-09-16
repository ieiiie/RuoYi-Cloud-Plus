package com.ym.agriculture.farming.dashboard.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 传感器状态侧栏树形汇总。
 * <p>
 * 树形结构：一级为设备大类（从字典 {@code iot_device_type} 动态获取），二级为产品，三级为具体设备明细。
 * 无人机作为虚拟设备归入"其它设备"大类。
 * </p>
 *
 * @author ym-cloud
 */
@Data
public class SfDashboardSensorSummaryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 设备大类列表（树形一级） */
    private List<DeviceCategoryVo> categories = new ArrayList<>();
}
