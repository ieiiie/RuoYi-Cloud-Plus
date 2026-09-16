package com.ym.agriculture.farming.satellite.remote.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 删除外部遥感服务任务的请求体。
 */
@Data
public class SatelliteRemoteTaskDeleteRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonProperty("dk_id")
    private String dkId;

    @JsonProperty("start_date")
    private String startDate;

    @JsonProperty("end_date")
    private String endDate;

    @JsonProperty("task_type")
    private String taskType;
}
