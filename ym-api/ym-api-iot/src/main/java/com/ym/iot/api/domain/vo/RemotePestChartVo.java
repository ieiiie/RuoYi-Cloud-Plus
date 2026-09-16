package com.ym.iot.api.domain.vo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class RemotePestChartVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long deviceId;
    private List<Date> times;
    private List<TimeValuePoint> totalSeries;
    private List<PestStackSeries> pestSeries;
    private List<PestRecord> records;
    private PestRecord defaultRecord;

    @Data public static class TimeValuePoint implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private Date time;
        private BigDecimal value;
    }
    @Data public static class PestStackSeries implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private String name;
        private List<Integer> data;
    }
    @Data public static class PestRecord implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private Date collectTime;
        private Integer totalCount;
        private String imageUrl;
        private List<PestItem> items;
    }
    @Data public static class PestItem implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private String name;
        private Integer count;
        private String recognizeTime;
    }
}
