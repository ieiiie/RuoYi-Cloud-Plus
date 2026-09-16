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
@TableName("sf_uav_flight_plan")
public class SfUavFlightPlan extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "plan_id", type = IdType.ASSIGN_ID)
    private Long planId;

    private Long plantingBatchId;
    private String plantingBatchName;
    private Long fieldId;
    private String fieldName;
    private Long speciesId;
    private String speciesName;
    private Long varietyId;
    private String varietyName;

    private String planName;
    private String workspaceId;
    private String userId;
    private Integer source;
    private String fileId;
    private String dockSn;
    private Integer waylineType;
    private Integer rthAltitude;
    private Integer outOfControlAction;
    private Integer minBatteryCapacity;
    private Integer minStorageCapacity;
    private Integer exitWaylineWhenRcLost;

    private String aiModelNo;
    private String aiModelNos;

    /** IMMEDIATE / SCHEDULED / REPEAT. */
    private String planMode;

    /** PLANNING / PAUSED / COMPLETED / FAILED. */
    private String status;

    private Date executeTime;
    private String expirePolicy;

    /** DAILY / WEEKLY / MONTHLY. */
    private String repeatMode;
    private String dailyTimes;
    private String weekdays;
    private String monthDays;
    private String startTime;
    private Date effectiveStartAt;
    private Date effectiveEndAt;
    private Date nextOccurrenceAt;

    private String lastError;

    @TableLogic
    private String delFlag;
}
