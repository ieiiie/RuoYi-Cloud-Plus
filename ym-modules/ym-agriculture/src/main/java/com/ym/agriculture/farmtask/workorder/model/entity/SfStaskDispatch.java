package com.ym.agriculture.farmtask.workorder.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * stask 派工明细，表 {@code sf_stask_dispatch}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_stask_dispatch")
public class SfStaskDispatch extends TenantEntity {

    /**
     * 派工明细主键。
     */
    @TableId("dispatch_id")
    private Long dispatchId;

    /**
     * 工单ID。
     */
    private Long orderId;

    /**
     * 组长员工ID。
     */
    private Long leaderId;

    /**
     * 工人员工ID。
     */
    private Long workerId;

    /**
     * 派工状态：PENDING/ACCEPTED/REJECTED/CANCELLED。
     */
    private String status;

    /**
     * 拒绝原因。
     */
    private String rejectReason;

    /**
     * 组长评价。
     */
    private String leaderEvaluation;

    /**
     * 邀请时间。
     */
    private Date invitedAt;

    /**
     * 响应时间。
     */
    private Date respondedAt;

    /**
     * 撤销时间。
     */
    private Date cancelledAt;

    /**
     * 邀请短信发送时间；NULL 表示尚未发送。
     */
    private Date inviteSmsSentAt;

    /**
     * 乐观锁版本。
     */
    @Version
    private Integer version;
}
