package com.ym.agriculture.farming.batch.model.vo;

import com.ym.agriculture.farming.satellite.model.vo.SatelliteScheduleDetailVo;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 种植批次下的遥感子任务视图，含该子任务对应的遥感分析结果列表。
 *
 * @author ym-cloud
 */
@Data
public class BatchSatelliteSubTaskVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 子任务主键 {@code sf_satellite_task.task_id} */
    private Long taskId;

    /** 关联周期计划 {@code sf_satellite_schedule.schedule_id}，单次任务为 null */
    private Long scheduleId;

    /** 对外遥感地块唯一标识（雪花字符串） */
    private String dkId;

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

    /** 是否成功（status == SUCCESS） */
    private Boolean success;

    /** 失败原因 */
    private String message;

    /** 创建时间 */
    private Date createTime;

    /** 最近一次提交时间 */
    private Date lastSubmitTime;

    /** 该子任务对应的遥感结果列表（按 image_date 倒序），可能为空 */
    private List<SatelliteScheduleDetailVo.ResultItem> results;
}
