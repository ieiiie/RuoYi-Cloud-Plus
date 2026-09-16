package com.ym.agriculture.farming.batch.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.Date;

/**
 * 种植批次。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_planting_batch")
public class SfPlantingBatch extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("batch_id")
    private Long batchId;
    private Long fieldId;
    private Long varietyId;
    private String batchCode;
    private Integer croppingIndex;
    private Date sowingDate;
    private Date expectedHarvestDate;
    private Date actualHarvestDate;
    private String batchStatus;
    private Date statusTime;

    @TableLogic
    private String delFlag;

    private String remark;
}
