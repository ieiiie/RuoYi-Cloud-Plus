package com.ym.agriculture.farming.uav.model.bo;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 本地飞行任务分页查询条件（GET 参数绑定）。
 * <p>
 * 非空（trim 后）才拼接条件；名称、jobId、uavJobId、taskName 等为 {@code LIKE}，{@code status} 为 {@code =}。
 */
@Data
public class SfUavFlightTaskQueryBo {

    /** 地块 ID */
    private Long fieldId;

    /** 冗余地块名称，模糊匹配 {@code field_name}（LIKE） */
    private String fieldName;

    /** 冗余物种名称，模糊匹配 {@code species_name} */
    private String speciesName;

    /** 冗余品种名称，模糊匹配 {@code variety_name} */
    private String varietyName;

    /** 冗余种植批次展示名，模糊匹配 {@code planting_batch_name} */
    private String plantingBatchName;

    /** 飞控任务状态，精确匹配 */
    private Integer status;

    /** 本地 jobId，模糊匹配 */
    private String jobId;

    /** 飞控任务列表 job_id，模糊匹配 */
    private String uavJobId;

    /** 任务名称，模糊匹配 */
    private String taskName;

    /** 创建时间起（含） */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTimeBegin;

    /** 创建时间止（含） */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTimeEnd;
}
