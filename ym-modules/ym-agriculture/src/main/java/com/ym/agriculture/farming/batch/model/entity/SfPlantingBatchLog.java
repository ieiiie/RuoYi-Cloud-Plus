package com.ym.agriculture.farming.batch.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 种植批次状态流水。
 */
@Data
@TableName("sf_planting_batch_log")
public class SfPlantingBatchLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("log_id")
    private Long logId;
    private String tenantId;
    private Long batchId;
    private String fromStatus;
    private String toStatus;
    private Long operateBy;
    private Date operateTime;
    private String remark;
}
