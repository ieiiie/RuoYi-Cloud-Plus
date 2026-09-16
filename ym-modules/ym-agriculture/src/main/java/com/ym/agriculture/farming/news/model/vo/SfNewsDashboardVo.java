package com.ym.agriculture.farming.news.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 移动端农业资讯首页聚合结果。 */
@Data
public class SfNewsDashboardVo {

    /** 天气、预警等指标；实时数据由已有专门服务装配。 */
    private List<Object> metrics = new ArrayList<>();
    /** 推荐资讯。 */
    private List<SfNewsMobileListVo> featured = new ArrayList<>();
}
