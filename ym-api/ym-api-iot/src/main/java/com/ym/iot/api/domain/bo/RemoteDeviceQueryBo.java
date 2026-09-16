package com.ym.iot.api.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 设备跨服务分页查询条件。
 *
 * @author ym-cloud
 */
@Data
public class RemoteDeviceQueryBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long productId;
    private String deviceCode;
    private List<String> deviceCodeList;
    private String deviceName;
    private String deviceCategory;
    private String onlineStatus;
    private String status;
    private Integer pageNum;
    private Integer pageSize;
}
