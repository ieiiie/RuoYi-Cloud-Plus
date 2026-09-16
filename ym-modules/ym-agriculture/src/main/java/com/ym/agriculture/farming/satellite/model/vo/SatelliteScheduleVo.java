package com.ym.agriculture.farming.satellite.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * 遥感周期计划视图对象，用于列表和详情接口返回。
 *
 * @author ym-cloud
 */
@Data
public class SatelliteScheduleVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 计划主键 */
    private Long scheduleId;

    /** 关联种植批次 ID */
    private Long plantingBatchId;

    /** 冗余：种植批次展示名 */
    private String plantingBatchName;

    /** 关联地块 ID */
    private Long fieldId;

    /** 冗余：地块名称 */
    private String fieldName;

    /** 冗余：物种 ID */
    private Long speciesId;

    /** 冗余：物种名称 */
    private String speciesName;

    /** 冗余：品种 ID */
    private Long varietyId;

    /** 冗余：品种名称 */
    private String varietyName;

    /** 作物类型码 */
    private String codeCroptype;

    /** 任务类型原始值（逗号分隔） */
    private String taskType;

    /** 分析类型列表（由 taskType 拆分而来） */
    private List<String> taskTypeList;

    /** 分析类型数量 */
    private Integer taskTypeCount;

    /**
     * 分析类型显示文本：1种直接显示中文名，超过1种显示如"土壤墒情等3项"
     */
    private String taskTypeDisplay;

    /** 遥感影像分辨率（米） */
    private Integer pixelImage;

    /** 检测开始日期 */
    private LocalDate detectStart;

    /** 检测结束日期 */
    private LocalDate detectEnd;

    /** 执行周期（天） */
    private Integer cycleDays;

    /** 下次执行日期 */
    private LocalDate nextRunDate;

    /**
     * 计划状态：0=未开始、1=周期中、2=已完成、3=已停用。
     * 由后端根据日期动态计算（未停用时）。
     */
    private Integer scheduleStatus;

    /** 状态中文描述 */
    private String statusDesc;

    /** 已提交周期次数（从关联 task 记录统计） */
    private Integer submitCount;

    /** 提交次数显示格式如 "2*10" */
    private String submitCountDisplay;

    /** 当前计划下未逻辑删除的遥感任务数量 */
    private Integer taskCount;

    /** 回调影像数据数量 */
    private Integer imageDataCount;

    /** 遥感结果处理成功次数 */
    private Integer resultSuccessCount;

    /** 遥感结果处理失败次数 */
    private Integer resultFailCount;

    /** 创建时间 */
    private Date createTime;
}
