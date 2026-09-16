package com.ym.agriculture.farmtask.workorder.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * stask 任务包大棚明细，表 {@code sf_stask_work_order_greenhouse}。
 */
@Data
@TableName("sf_stask_work_order_greenhouse")
public class SfStaskWorkOrderGreenhouse {

    /**
     * 任务包大棚明细ID。
     */
    @TableId("greenhouse_item_id")
    private Long greenhouseItemId;

    /**
     * 任务包ID。
     */
    private Long packageId;

    /**
     * 拆分工单ID；严格分组拆单后，多棚可归属同一拆分工单。
     */
    private Long orderId;

    /**
     * 租户编号。
     */
    private String tenantId;

    /**
     * 任务包农事项明细ID。
     */
    private Long itemId;

    /**
     * 农事项目ID。
     */
    private Long workItemId;

    /**
     * 大棚ID。
     */
    private Long greenhouseId;

    /**
     * 大棚编码快照。
     */
    private String greenhouseCodeSnapshot;

    /**
     * 大棚名称快照。
     */
    private String greenhouseNameSnapshot;

    /**
     * 创建时间。
     */
    private Date createTime;
}
