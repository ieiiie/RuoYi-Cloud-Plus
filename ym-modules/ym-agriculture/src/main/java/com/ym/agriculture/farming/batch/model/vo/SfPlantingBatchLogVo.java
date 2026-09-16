package com.ym.agriculture.farming.batch.model.vo;

import com.ym.agriculture.farming.batch.model.entity.SfPlantingBatchLog;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 批次状态流水视图。
 */
@Data
@AutoMapper(target = SfPlantingBatchLog.class)
public class SfPlantingBatchLogVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long logId;
    private Long batchId;
    private String fromStatus;
    private String toStatus;
    private Long operateBy;
    private Date operateTime;
    private String remark;
}
