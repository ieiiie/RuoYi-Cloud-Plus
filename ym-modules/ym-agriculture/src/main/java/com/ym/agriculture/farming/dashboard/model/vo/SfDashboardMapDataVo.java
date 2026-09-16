package com.ym.agriculture.farming.dashboard.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 农田地图聚合数据。
 *
 * @author ym-cloud
 */
@Data
public class SfDashboardMapDataVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 地块列表（已按当前用户地块数据权限过滤）
     */
    private List<SfDashboardMapPlotVo> plots = new ArrayList<>();
}
