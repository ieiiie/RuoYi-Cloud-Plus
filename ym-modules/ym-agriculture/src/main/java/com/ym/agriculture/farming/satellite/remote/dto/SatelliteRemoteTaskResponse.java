package com.ym.agriculture.farming.satellite.remote.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class SatelliteRemoteTaskResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("code")
    private Integer code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private Object data;
}
