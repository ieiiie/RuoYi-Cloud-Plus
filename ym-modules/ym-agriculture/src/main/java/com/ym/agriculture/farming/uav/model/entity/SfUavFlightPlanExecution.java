package com.ym.agriculture.farming.uav.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_uav_flight_plan_execution")
public class SfUavFlightPlanExecution extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "execution_id", type = IdType.ASSIGN_ID)
    private Long executionId;

    private Long planId;
    private Date occurrenceAt;
    private String status;
    private Long flightTaskId;
    private String uavJobId;
    private String errorMessage;

    @TableLogic
    private String delFlag;
}
