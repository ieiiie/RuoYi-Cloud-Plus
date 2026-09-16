package com.ym.agriculture.farming.satellite.health.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 卫星综合健康评分接口视图对象集合。 */
public final class SatelliteHealthVo {

    private SatelliteHealthVo() {
    }

    /** 地块分页项。 */
    @Data
    public static class Field {
        /** 地块主键。 */
        private Long fieldId;
        /** 地块编码。 */
        private String fieldCode;
        /** 地块名称。 */
        private String fieldName;
        /** 地块类型：FIELD / GREENHOUSE。 */
        private String fieldType;
        /** 地块面积，单位亩。 */
        private BigDecimal areaMu;
    }

    /** 日期条响应。 */
    @Data
    public static class Dates {
        /** 当前活跃种植批次主键；无活跃批次时为空。 */
        private Long activeBatchId;
        /** 有效日期卡片，按日期升序。 */
        private List<DateCard> dates;
        /** 下一次查询的 beforeDate；无更多数据时为空。 */
        private String nextBeforeDate;
        /** 是否仍有更早日期。 */
        private boolean hasMore;
    }

    /** 日期卡片。 */
    @Data
    public static class DateCard {
        /** 影像自然日，格式 yyyy-MM-dd。 */
        private String imageDate;
        /** 云量百分比；缺失时为空。 */
        private BigDecimal cloudCoverPercent;
        /** 云量大于等于 60% 时为 true。 */
        private boolean cloudWarning;
        /** 固定指标顺序取得的第一张可访问缩略图；无图时为空。 */
        private String thumbnailUrl;
    }

    /** 指定日期评分详情。 */
    @Data
    public static class Detail {
        /** 当前活跃种植批次主键。 */
        private Long activeBatchId;
        /** 影像自然日。 */
        private String imageDate;
        /** 评分状态：SCORED / INSUFFICIENT_DATA。 */
        private String scoreStatus;
        /** 综合分，保留一位小数；数据不足时为空。 */
        private BigDecimal score;
        /** 评分等级：EXCELLENT / GOOD / FAIR / POOR；数据不足时为空。 */
        private String grade;
        /** 地块总面积，单位亩；无效时为空。 */
        private BigDecimal totalAreaMu;
        /** 重点关注指标，按 PRD 优先级排序。 */
        private List<String> focusItems;
        /** 低等级面积，单位亩。 */
        private BigDecimal lowLevelAreaMu;
        /** 低等级面积占地块总面积百分比；总面积无效时为空。 */
        private BigDecimal lowLevelRatioPercent;
        /** 固定顺序指标明细。 */
        private List<Metric> metrics;
    }

    /** 单项指标。 */
    @Data
    public static class Metric {
        /** 指标任务编码。 */
        private String type;
        /** 指标名称。 */
        private String name;
        /** 可访问遥感图片地址；无图时为空。 */
        private String imageUrl;
        /** 指标得分；结果无效或缺失时为空。 */
        private BigDecimal score;
        /** 好/中/差（干旱为润/中/旱）面积。 */
        private List<Level> levels;
    }

    /** 三档面积。 */
    @Data
    public static class Level {
        /** 档位编码：GOOD / MEDIUM / BAD，干旱仍复用该编码。 */
        private String code;
        /** 档位文案。 */
        private String label;
        /** 面积，单位亩；缺失时为空。 */
        private BigDecimal areaMu;
        /** 在该指标有效面积中的百分比；缺失时为空。 */
        private BigDecimal ratioPercent;
    }
}
