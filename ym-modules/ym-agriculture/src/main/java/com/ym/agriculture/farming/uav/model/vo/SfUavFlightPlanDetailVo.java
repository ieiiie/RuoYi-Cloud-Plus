package com.ym.agriculture.farming.uav.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.Collections;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SfUavFlightPlanDetailVo extends SfUavFlightPlanVo {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<SfUavFlightPlanExecutionVo> executions = Collections.emptyList();
}
