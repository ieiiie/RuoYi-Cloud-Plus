package com.ym.agriculture.api.farmtask.domain.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

/** 组长接单命令。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RemoteTaskLeaderAcceptBo extends RemoteTaskCommandBo {
    @Serial
    private static final long serialVersionUID = 1L;
    private Double requiredWorkerCount;
    private BigDecimal dailyLaborCount;
    private Long laborRecordVersion;
}
