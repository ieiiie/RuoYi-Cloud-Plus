package com.ym.agriculture.farming.batch.model.vo;

import com.ym.agriculture.farming.batch.model.entity.SfPlantingBatch;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 种植批次视图。
 */
@Data
@AutoMapper(target = SfPlantingBatch.class)
public class SfPlantingBatchVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long batchId;
    private String tenantId;
    private Long fieldId;
    private Long varietyId;
    private String batchCode;
    private Integer croppingIndex;
    private Date sowingDate;
    private Date expectedHarvestDate;
    private Date actualHarvestDate;
    private String batchStatus;
    private Date statusTime;
    private Date createTime;
    private Date updateTime;
    private String remark;
    private String fieldName;
    private String varietyName;
    private Long speciesId;
    private String speciesName;
    private String speciesImageUrl;
}
