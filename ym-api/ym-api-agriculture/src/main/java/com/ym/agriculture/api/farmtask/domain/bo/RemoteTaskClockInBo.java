package com.ym.agriculture.api.farmtask.domain.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

/** 到岗打卡命令。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RemoteTaskClockInBo extends RemoteTaskCommandBo {
    @Serial
    private static final long serialVersionUID = 1L;
    private String clockType;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String proofPhotos;
}
