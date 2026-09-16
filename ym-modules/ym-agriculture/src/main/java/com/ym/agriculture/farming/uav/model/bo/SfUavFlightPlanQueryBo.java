package com.ym.agriculture.farming.uav.model.bo;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 飞行计划分页查询条件（GET 参数绑定）。
 * <p>非空才拼接条件；{@code planName} 为模糊匹配，{@code status}、{@code planMode} 为精确匹配（忽略首尾空白，状态类字段会转大写）。</p>
 */
@Data
public class SfUavFlightPlanQueryBo {

    /** 种植批次 ID */
    private Long plantingBatchId;

    /** 地块 ID */
    private Long fieldId;

    /** 计划模式：IMMEDIATE / SCHEDULED / REPEAT */
    private String planMode;

    /** 计划状态：PLANNING / PAUSED / COMPLETED / FAILED */
    private String status;

    /** 计划名称，模糊匹配 */
    private String planName;

    /** 创建时间起（含） */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTimeBegin;

    /** 创建时间止（含） */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTimeEnd;
}
