package com.ym.iot.wvp.domain.dto.gb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WvpGbChannelListResponse {

    @JsonProperty("ChannelCount")
    private Integer channelCount;

    @JsonProperty("ChannelList")
    private List<WvpGbChannelItem> channelList;
}
