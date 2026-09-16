package com.ym.iot.device.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 虫情夜间聚合柱状图数据。
 */
@Data
public class IotPestNightChartVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 设备主键。
     */
    private Long deviceId;

    /**
     * 夜间统计开始小时，固定 21 表示晚 9 点。
     */
    private Integer bucketStartHour;

    /**
     * 夜间统计结束小时，固定 9 表示次日早 9 点。
     */
    private Integer bucketEndHour;

    /**
     * 夜间时间桶列表，顺序与虫种堆叠序列 data 保持一致。
     */
    private List<NightBucket> buckets;

    /**
     * 虫种数量堆叠柱图序列。
     */
    private List<PestStackSeries> pestSeries;

    /**
     * 夜间时间桶。
     */
    @Data
    public static class NightBucket implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 时间桶标识，格式 yyyy-MM-dd，取晚 9 点所属日期。
         */
        private String bucketKey;

        /**
         * 时间桶开始时间，固定为某日 21:00:00。
         */
        private Date startTime;

        /**
         * 时间桶结束时间，固定为次日 09:00:00。
         */
        private Date endTime;

        /**
         * 前端展示标签，例如：6月3晚9点~6月4早9点。
         */
        private String label;

        /**
         * 有效图片采集记录数，同一采集时间多张图只计 1 张。
         */
        private Integer photoCount;

        /**
         * 当前夜间桶内害虫总数。
         */
        private Integer totalCount;
    }

    /**
     * 单个虫种在各夜间桶内的数量序列。
     */
    @Data
    public static class PestStackSeries implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 虫害名称。
         */
        private String name;

        /**
         * 各夜间桶内数量，顺序与 buckets 一致。
         */
        private List<Integer> data;
    }
}
