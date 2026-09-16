package com.ym.agriculture.farming.trace.model.vo;

import com.ym.agriculture.farming.trace.model.entity.SfTraceBatch;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 溯源批次视图。
 */
@Data
@AutoMapper(target = SfTraceBatch.class)
public class SfTraceBatchVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long traceBatchId;
    private String tenantId;
    private Long plantingBatchId;
    private Long fieldId;
    private Long varietyId;
    private String traceBatchNo;
    private String productName;
    private String qualityGrade;
    private String originText;
    private String producerName;
    private String certificationJson;
    private String labelScope;
    private Integer plannedQuantity;
    private Integer generatedQuantity;
    private String status;
    private String remark;
    private Date createTime;
    private Date updateTime;

    private String fieldName;
    private String speciesName;
    private String varietyName;
    private String batchCode;
    private Date sowingDate;
    private Date expectedHarvestDate;
    private Date actualHarvestDate;

    private List<TraceCertificationVo> certifications;
}
