package com.ym.agriculture.farmtask.leaderlabor.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/** stask 组长计划日用工记录。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_stask_leader_labor_record")
public class SfStaskLeaderLaborRecord extends TenantEntity {

    /** 日用工记录ID。 */
    @TableId(value = "labor_record_id", type = IdType.ASSIGN_ID)
    private Long laborRecordId;
    /** 组长员工ID。 */
    private Long leaderEmployeeId;
    /** 组长姓名快照。 */
    private String leaderNameSnapshot;
    /** 任务计划日期。 */
    private LocalDate planDate;
    /** 日用工人数，不含组长本人。 */
    private BigDecimal laborCount;
    /** 乐观锁版本。 */
    @Version
    private Long version;
    /** 最近一次写入来源：SINGLE_ACCEPT/BATCH_ACCEPT/MANUAL_EDIT。 */
    private String lastSource;
}
