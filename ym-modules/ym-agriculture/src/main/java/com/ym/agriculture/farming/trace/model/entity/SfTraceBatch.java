package com.ym.agriculture.farming.trace.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;

/**
 * 瓜果溯源批次 sf_trace_batch。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sf_trace_batch")
public class SfTraceBatch extends SfTraceTenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("trace_batch_id")
    private Long traceBatchId;

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

    @TableLogic
    private String delFlag;
    private String remark;

    public SfTraceBatch(Long traceBatchId) {
        this.traceBatchId = traceBatchId;
    }
}
