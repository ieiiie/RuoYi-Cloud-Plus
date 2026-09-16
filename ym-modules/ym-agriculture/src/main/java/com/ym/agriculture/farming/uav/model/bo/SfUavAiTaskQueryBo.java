package com.ym.agriculture.farming.uav.model.bo;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * UAV AI 分析任务分页查询条件（GET 参数绑定）。
 * <p>
 * 非空（trim 后）才拼接条件；名称、uavJobId、algTaskNo 等为 {@code LIKE}，{@code status} 为 {@code =}。
 */
@Data
public class SfUavAiTaskQueryBo {

    /** 地块 ID */
    private Long fieldId;

    /** 冗余地块名称，模糊匹配 {@code field_name} */
    private String fieldName;

    /** 冗余物种名称，模糊匹配 {@code species_name} */
    private String speciesName;

    /** 冗余品种名称，模糊匹配 {@code variety_name} */
    private String varietyName;

    /** 冗余种植批次展示名（batch_code 快照），模糊匹配 {@code planting_batch_name} */
    private String plantingBatchName;

    /** 状态：SUBMITTED / FINISHED / FAILED 等，精确匹配 */
    private String status;

    /** UAV 任务 ID（本地 jobId），模糊匹配 */
    private String uavJobId;

    /** 算法平台任务号，模糊匹配 */
    private String algTaskNo;

    /** AI model number, exact match. */
    private String modelNo;

    /** 创建时间起（含） */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTimeBegin;

    /** 创建时间止（含） */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTimeEnd;
}
