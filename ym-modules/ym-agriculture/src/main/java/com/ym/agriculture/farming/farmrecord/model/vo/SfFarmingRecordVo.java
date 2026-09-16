package com.ym.agriculture.farming.farmrecord.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ym.agriculture.farming.solarterms.model.vo.CalendarDateInfoVo;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 移动端农事记录列表/详情出参。
 *
 * @author ym-cloud
 */
@Data
public class SfFarmingRecordVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 农事记录主键。
     */
    private Long recordId;

    /**
     * 状态：DRAFT-草稿，SUBMITTED-已提交。
     */
    private String status;

    /**
     * 农事发生时间，格式：yyyy-MM-dd HH:mm:ss。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date happenedAt;

    /**
     * 发生时间对应的农历与节气信息；列表查询不填充，详情查询自动填充。
     */
    private CalendarDateInfoVo calendarInfo;

    /**
     * 作业时段，数组格式：[开始时间, 结束时间]，精确到分钟。
     */
    private List<String> workPeriod = new ArrayList<>();

    /**
     * 今日小结，最大 500 字。
     */
    private String summary;

    /**
     * 天气快照对象。
     */
    private Object weather;

    /**
     * 生长阶段快照对象。
     */
    private Object growthStage;

    /**
     * 资源投入快照对象。
     */
    private Object resource;

    /**
     * 现场反馈快照对象。
     */
    private Object feedback;

    /**
     * 植株高度，单位见 plantHeightUnit；从 feedback.plantHeight 冗余回显，便于前端直接读取。
     */
    private BigDecimal plantHeight;

    /**
     * 植株高度单位，固定为 cm。
     */
    private String plantHeightUnit;

    /**
     * 传感器快照对象。
     */
    private Object sensorSnapshot;

    /**
     * 环境摘要快照对象。
     */
    private Object environmentSummary;

    /**
     * 提交时间，草稿为空。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date submitTime;

    /**
     * 创建人用户 ID，用于草稿归属判断和列表展示。
     */
    private Long createBy;

    /**
     * 创建人昵称。
     */
    private String creatorName;

    /**
     * 备注。
     */
    private String remark;

    /**
     * 地块明细快照列表。
     */
    private List<SfFarmingRecordFieldVo> fields = new ArrayList<>();

    /**
     * 农事项目明细快照列表。
     */
    private List<SfFarmingRecordWorkItemVo> workItems = new ArrayList<>();

    /**
     * 列表预览媒体，通常只返回前几条。
     */
    private List<SfFarmingRecordMediaVo> previewMedia = new ArrayList<>();

    /**
     * 详情媒体列表。
     */
    private List<SfFarmingRecordMediaVo> media = new ArrayList<>();
}
