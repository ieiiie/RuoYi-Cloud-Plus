package com.ym.agriculture.farming.trace.model.bo;

import com.ym.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 溯源码查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SfTraceCodeBo extends BaseEntity {

    private Long traceBatchId;
    private String traceCode;
    private String status;
}
