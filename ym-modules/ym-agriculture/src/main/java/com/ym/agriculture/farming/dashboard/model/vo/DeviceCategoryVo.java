package com.ym.agriculture.farming.dashboard.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 设备大类（树形一级），如"农业设备"、"气象环境设备"、"监控设备"、"其它设备"。
 *
 * @author ym-cloud
 */
@Data
public class DeviceCategoryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 设备大类编码（字典 {@code dictValue} 或 {@code "OTHER"}） */
    private String categoryCode;

    /** 设备大类展示名（字典 {@code dictLabel} 或 {@code "其它设备"}） */
    private String categoryLabel;

    /** 该大类下在线设备总数 */
    private long onlineCount;

    /** 该大类下离线设备总数 */
    private long offlineCount;

    /** 状态待确认数量。 */
    private long unknownCount;

    /** 设备总数 */
    private long totalCount;

    /** 红灯标记：只要有1个设备离线则为 {@code true} */
    private boolean hasOffline;

    /** 产品分组列表（树形二级） */
    private List<ProductGroupVo> products = new ArrayList<>();
}
