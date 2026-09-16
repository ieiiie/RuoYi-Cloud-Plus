package com.ym.agriculture.farming.satellite.health.model;

import lombok.Data;

import java.util.Date;

/** 卫星健康评分批量查询行，仅在读模型内部使用。 */
@Data
public class SatelliteHealthResultRow {
    /** 结果主键。 */
    private Long resultId;
    /** 指标任务编码。 */
    private String taskType;
    /** 影像日期原值。 */
    private String imageDate;
    /** 五档面积 JSON。 */
    private String area;
    /** Web 可访问图片地址。 */
    private String ossUrl;
    /** 结果创建时间，用于同日同指标去重。 */
    private Date createTime;
}
