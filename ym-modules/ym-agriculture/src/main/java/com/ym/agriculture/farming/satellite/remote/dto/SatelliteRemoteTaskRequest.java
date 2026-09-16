package com.ym.agriculture.farming.satellite.remote.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 提交外部遥感服务的请求体。{@code code_croptype} 为作物类型码，JSON 中按对端约定使用字符串传输。
 * 平台内部可使用数值型作物类型码，向下游时统一转为字符串以兼容对端实现。
 */
@Data
public class SatelliteRemoteTaskRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    @JsonProperty("dk_id")
    private String dkId;

    @NotNull
    @JsonProperty("dk_geom")
    private DkGeom dkGeom;

    @NotBlank
    @Size(max = 64)
    @JsonProperty("code_croptype")
    private String codeCroptype;

    @NotBlank
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$")
    @JsonProperty("start_date")
    private String startDate;

    @NotBlank
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$")
    @JsonProperty("end_date")
    private String endDate;

    @NotBlank
    @JsonProperty("task_type")
    private String taskType;

    @Min(2)
    @JsonProperty("pixel_image")
    private Integer pixelImage = 10;

    @NotBlank
    @JsonProperty("url")
    private String url;

    /**
     * 地块空间几何信息，符合 GeoJSON Polygon 结构。
     *
     * 示例：
     * {
     *   "type": "Polygon",
     *   "crs": {
     *     "type": "name",
     *     "properties": {
     *       "name": "EPSG:32644"
     *     }
     *   },
     *   "coordinates": [
     *     [
     *       [806659.747027441, 4937990.208512495],
     *       ...
     *     ]
     *   ]
     * }
     */
    @Data
    public static class DkGeom {

        @NotBlank
        private String type;

        @NotNull
        private Crs crs;

        /**
         * 三维数组结构：List<线环<List<点<List<坐标值>>>>>
         * 最内层坐标值必须为数字类型（经度/纬度或投影坐标）。
         */
        @NotNull
        private List<List<List<Double>>> coordinates;

        @Data
        public static class Crs {

            @NotBlank
            private String type;

            @NotNull
            private CrsProperties properties;
        }

        @Data
        public static class CrsProperties {

            @NotBlank
            private String name;
        }
    }
}
