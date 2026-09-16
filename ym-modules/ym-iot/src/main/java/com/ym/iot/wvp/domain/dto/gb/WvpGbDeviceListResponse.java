package com.ym.iot.wvp.domain.dto.gb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

import java.util.List;

/**
 * WVP 设备列表分页数据，对应 {@code GET /api/device/query/devices} 返回的 {@code data} 节点。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WvpGbDeviceListResponse {

    private Integer total;
    private List<WvpGbDeviceItem> list;
}
