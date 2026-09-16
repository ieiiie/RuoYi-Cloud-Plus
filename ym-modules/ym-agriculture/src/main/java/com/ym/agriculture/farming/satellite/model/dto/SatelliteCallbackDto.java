package com.ym.agriculture.farming.satellite.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 遥感服务回调体（与 ym-gis 一致，使用 Jackson 字段名）。
 */
@Data
public class SatelliteCallbackDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonProperty("dk_id")
    private String dkId;

    @JsonProperty("dk_bounds")
    private Bounds dkBounds;

    @JsonProperty("task_type")
    private String taskType;

    @JsonProperty("break_value")
    private List<List<Double>> breakValue;

    @JsonProperty("area")
    private List<Double> area;

    @JsonProperty("object_key1")
    private String objectKey1;

    @JsonProperty("object_key2")
    private String objectKey2;

    @JsonProperty("image_pixel")
    private Integer imagePixel;

    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("image_date")
    private String imageDate;

    @JsonProperty("bucket")
    private String bucket;

    @Data
    public static class Bounds implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @JsonProperty("west")
        private Double west;

        @JsonProperty("south")
        private Double south;

        @JsonProperty("east")
        private Double east;

        @JsonProperty("north")
        private Double north;

        @JsonProperty("center_lon")
        private Double centerLon;

        @JsonProperty("center_lat")
        private Double centerLat;
    }
}
