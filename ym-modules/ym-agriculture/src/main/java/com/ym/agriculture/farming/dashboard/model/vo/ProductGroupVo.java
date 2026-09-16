package com.ym.agriculture.farming.dashboard.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 产品分组（树形二级），隶属于某一 {@link DeviceCategoryVo}。
 *
 * @author ym-cloud
 */
@Data
public class ProductGroupVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 产品ID（无人机等虚拟设备为 {@code null}） */
    private Long productId;

    /** 产品名称 */
    private String productName;

    /** 该产品下在线设备数量 */
    private long onlineCount;

    /** 该产品下离线设备数量 */
    private long offlineCount;

    /** 状态待确认数量。 */
    private long unknownCount;

    /** 设备总数 */
    private long totalCount;

    /** 红灯标记：只要有1个设备离线则为 {@code true} */
    private boolean hasOffline;

    /** 具体设备列表 */
    private List<SfDashboardSensorSummaryItemVo> devices = new ArrayList<>();
}
