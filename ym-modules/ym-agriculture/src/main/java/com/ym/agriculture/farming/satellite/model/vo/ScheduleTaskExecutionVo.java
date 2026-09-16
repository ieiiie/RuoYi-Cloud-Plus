package com.ym.agriculture.farming.satellite.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 遥感任务执行记录（按周期向卫星发起的单次分析请求），用于"执行结果"Tab 展示。
 *
 * @author ym-cloud
 */
@Data
public class ScheduleTaskExecutionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 遥感侧地块标识，用于跳转查看结果 */
    private String dkId;

    /** 发起时间 */
    private Date createTime;

    /** 分析类型编码 */
    private String taskType;

    /** 分析类型中文描述 */
    private String taskTypeDesc;

    /** 卫星窗口 - 开始日期 */
    private String startDate;

    /** 卫星窗口 - 结束日期 */
    private String endDate;

    /** 任务状态编码 */
    private Integer status;

    /** 任务状态中文描述 */
    private String statusDesc;

    /** 分析结果（成功/失败） */
    private Boolean success;

    /** 失败原因 */
    private String message;
}
