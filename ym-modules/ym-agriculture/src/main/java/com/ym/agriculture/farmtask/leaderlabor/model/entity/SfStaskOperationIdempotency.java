package com.ym.agriculture.farmtask.leaderlabor.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/** stask 批量操作持久化幂等记录。 */
@Data
@TableName("sf_stask_operation_idempotency")
public class SfStaskOperationIdempotency {

    /** 操作ID。 */
    @TableId(value = "operation_id", type = IdType.ASSIGN_ID)
    private Long operationId;
    /** 租户编号。 */
    private String tenantId;
    /** 发起员工ID。 */
    private Long employeeId;
    /** 操作类型：BATCH_ACCEPT/BATCH_CLOCK_IN。 */
    private String operationType;
    /** 客户端幂等键。 */
    private String idempotencyKey;
    /** 规范化请求摘要。 */
    private String requestHash;
    /** 状态：PROCESSING/SUCCEEDED。 */
    private String operationStatus;
    /** 首次成功响应JSON。 */
    private String responseJson;
    /** 创建时间。 */
    private Date createTime;
    /** 更新时间。 */
    private Date updateTime;
}
