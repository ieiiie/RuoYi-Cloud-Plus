package com.ym.agriculture.api.farming.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 地块跨服务摘要。
 */
@Data
public class RemoteFieldSummaryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long fieldId;
    private String fieldName;
    private String fieldCode;
    private String status;
}
