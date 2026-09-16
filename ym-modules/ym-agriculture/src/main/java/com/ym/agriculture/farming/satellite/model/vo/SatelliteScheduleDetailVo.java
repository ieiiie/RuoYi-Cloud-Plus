package com.ym.agriculture.farming.satellite.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 遥感周期计划详情 VO，在 {@link SatelliteScheduleVo} 基础上增加执行记录和卫星分析结果。
 *
 * @author ym-cloud
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SatelliteScheduleDetailVo extends SatelliteScheduleVo {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 执行记录列表（每次向卫星发起的分析请求） */
    private List<ScheduleTaskExecutionVo> executions;

    /** 卫星分析结果列表（来自 sf_satellite_task_result，按影像日期倒序） */
    private List<ResultItem> results;

    @Data
    public static class ResultItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /** 来源任务标识，可用于跳转查看详情 */
        private String dkId;

        /** 分析类型编码 */
        private String taskType;

        /** 分析类型中文描述 */
        private String taskTypeDesc;

        /** 影像拍摄日期 */
        private String imageDate;

        /** 影像访问地址 */
        private String ossUrl;

        /** 差异影像 OSS 对象键 */
        private String objectKey1;

        /** 备用影像 OSS 对象键 */
        private String objectKey2;

        /** 各等级面积 JSON */
        private String area;

        /** 是否成功 */
        private Boolean success;

        /** 该次分析的卫星窗口 - 开始日期 */
        private String startDate;

        /** 该次分析的卫星窗口 - 结束日期 */
        private String endDate;
    }
}
