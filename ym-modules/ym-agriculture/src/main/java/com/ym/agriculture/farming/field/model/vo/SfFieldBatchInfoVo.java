package com.ym.agriculture.farming.field.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 地块关联种植批次摘要，用于新增/编辑页判断作物字段状态。
 *
 * @author ym-cloud
 */
@Data
public class SfFieldBatchInfoVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 是否存在进行中的种植批次 */
    private Boolean hasActiveBatch;

    /** 进行中批次 ID；无进行中批次时为空 */
    private Long batchId;

    /** 批次编号 */
    private String batchCode;

    /** 批次状态：PLANNING/PLANTING/GROWING/HARVESTING 等 */
    private String batchStatus;

    /** 关联品种 ID */
    private Long varietyId;

    /** 关联品种名称 */
    private String varietyName;

    /** 关联作物物种 ID */
    private Long speciesId;

    /** 关联作物物种名称 */
    private String speciesName;

    /** 关联作物物种图片 URL */
    private String speciesImageUrl;

    /** 播种/定植日期 */
    private Date sowingDate;
}
