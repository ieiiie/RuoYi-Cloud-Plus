package com.ym.iot.api.domain.vo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class RemoteProductVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long productId;
    private String tenantId;
    private String productKey;
    private String productName;
    private String nodeType;
    private String netType;
    private String protocol;
    private String dataFormat;
    private String deviceCategory;
    private String status;
    private Date createTime;
    private Date updateTime;
    private String remark;
    private String mapIconUrl;
    private String mapSelectedIconUrl;
}
