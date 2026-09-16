package com.ym.iot.device.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 虫情设备图表与识别详情。
 */
@Data
public class IotPestChartVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 设备主键。
     */
    private Long deviceId;

    /**
     * 图表时间轴，按采集时间升序排列。
     */
    private List<Date> times;

    /**
     * 害虫总数折线数据，对应测点 {@code hfzk_number}。
     */
    private List<TimeValuePoint> totalSeries;

    /**
     * 虫种数量堆叠柱图数据，按 {@link #times} 对齐。
     */
    private List<PestStackSeries> pestSeries;

    /**
     * 每次采集的虫情识别详情，按采集时间升序排列。
     */
    private List<PestRecord> records;

    /**
     * 默认展示的识别详情：优先最新有虫种明细，其次最新有总数记录。
     */
    private PestRecord defaultRecord;

    /**
     * 单时间点数值。
     */
    @Data
    public static class TimeValuePoint implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 采集时间。
         */
        private Date time;

        /**
         * 数值。
         */
        private BigDecimal value;
    }

    /**
     * 单个虫种在各时间点的堆叠柱图数据。
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
         * 各时间点数量，顺序与 {@link IotPestChartVo#times} 一致。
         */
        private List<Integer> data;
    }

    /**
     * 单次虫情识别记录。
     */
    @Data
    public static class PestRecord implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 采集时间。
         */
        private Date collectTime;

        /**
         * 害虫总数，对应 {@code hfzk_number}。
         */
        private Integer totalCount;

        /**
         * 识别图片 URL，优先 {@code hfzk_newImage}，其次 {@code hfzk_image}，最后 {@code hfzk_yImage}。
         */
        private String imageUrl;

        /**
         * 虫种明细列表。
         */
        private List<PestItem> items;
    }

    /**
     * 单个虫种识别数量。
     */
    @Data
    public static class PestItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 虫害名称。
         */
        private String name;

        /**
         * 识别数量。
         */
        private Integer count;

        /**
         * 识别时间，沿用 HFZK 返回的字符串格式。
         */
        private String recognizeTime;
    }
}
