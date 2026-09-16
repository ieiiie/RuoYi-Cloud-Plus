package com.ym.agriculture.farming.weatheralert.remote.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 中央气象台 findAlarm 响应。 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NmcFindAlarmResponse {

    private String msg;
    private Integer code;
    private DataBody data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataBody {
        private PageBody page;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PageBody {
        private Integer pageNo;
        private Integer pageSize;
        private Integer count;
        private Integer totalPage;
        private List<AlarmItem> list = new ArrayList<>();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AlarmItem {
        private String alertid;
        private String issuetime;
        private String title;
        private String url;
        private String pic;
    }
}
