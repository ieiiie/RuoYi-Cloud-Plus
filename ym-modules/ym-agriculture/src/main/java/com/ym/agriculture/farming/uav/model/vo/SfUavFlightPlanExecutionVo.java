package com.ym.agriculture.farming.uav.model.vo;

import com.ym.agriculture.farming.uav.model.entity.SfUavFlightPlanExecution;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = SfUavFlightPlanExecution.class)
public class SfUavFlightPlanExecutionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long executionId;
    private String tenantId;
    private Long planId;
    private Date occurrenceAt;
    private String status;
    private Long flightTaskId;
    private String uavJobId;
    private String errorMessage;
    private Date createTime;
    private Date updateTime;
}
