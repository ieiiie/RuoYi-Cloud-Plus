package com.ym.agriculture.farming.uav.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ym.agriculture.farming.uav.model.entity.SfUavFlightPlan;
import com.ym.agriculture.farming.uav.support.SfUavAiModelNoSupport;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 无人机飞行计划列表/详情中的计划摘要信息。
 * <p>除 {@link #getAiModelNoList()} 外，时间与枚举类字段在 JSON 中与创建/落库约定一致。</p>
 */
@Data
@AutoMapper(target = SfUavFlightPlan.class)
public class SfUavFlightPlanVo implements Serializable {

    /** 旧平台停用说明；不覆盖历史任务的原始执行状态。 */
    public boolean isLegacyPlatformRetired() { return true; }

    public String getLegacyPlatformMessage() { return "旧无人机平台已停用，仅提供历史查询"; }

    @Serial
    private static final long serialVersionUID = 1L;

    /** 计划主键。示例：{@code 1987654321000123001} */
    private Long planId;
    /** 租户 ID。示例：{@code "000000"}、{@code "tenant_abc"} */
    private String tenantId;
    /** 种植批次主键。示例：{@code 1987654321000123456} */
    private Long plantingBatchId;
    /** 种植批次名称（冗余快照）。示例：{@code "2026春-东区小麦批次"} */
    private String plantingBatchName;
    /** 地块主键。示例：{@code 1987654321000123457} */
    private Long fieldId;
    /** 地块名称（冗余快照）。示例：{@code "东区1号地"} */
    private String fieldName;
    /** 作物种类主键。示例：{@code 1001} */
    private Long speciesId;
    /** 作物种类名称（冗余快照）。示例：{@code "小麦"} */
    private String speciesName;
    /** 品种主键。示例：{@code 2002} */
    private Long varietyId;
    /** 品种名称（冗余快照）。示例：{@code "济麦22"} */
    private String varietyName;

    /** 计划名称。示例：{@code "东区1号地-晨间巡飞"} */
    private String planName;
    /** 大疆工作空间 ID。示例：{@code "ws-xxxxx"} */
    private String workspaceId;
    /** 创建人或飞手标识。示例：{@code "uav_operator_01"} */
    private String userId;
    /** 任务来源类型。示例：{@code 1} */
    private Integer source;
    /** 航线或媒体文件标识。示例：{@code "wl-uuid-xxxx"} */
    private String fileId;
    /** 机场序列号。示例：{@code "DOCK7CU9999ABCD"} */
    private String dockSn;
    /** 航线类型。示例：{@code 0}、{@code 1} */
    private Integer waylineType;
    /** 返航高度（常见为米）。示例：{@code 80} */
    private Integer rthAltitude;
    /** 失控处置动作编码。示例：{@code 0} */
    private Integer outOfControlAction;
    /** 最低剩余电量阈值。示例：{@code 30} */
    private Integer minBatteryCapacity;
    /** 最低剩余存储阈值。示例：{@code 512} */
    private Integer minStorageCapacity;
    /** 遥控失联是否退出航线。示例：{@code 1} */
    private Integer exitWaylineWhenRcLost;

    /** 主展示用单个 AI 模型编号。示例：{@code "CROP_HEALTH_V1"} */
    private String aiModelNo;

    /**
     * 持久化多模型串（逗号分隔），仅内部映射，不参与 JSON。
     * <p>示例：{@code "CROP_HEALTH_V1,WEED_SEG_V2"}</p>
     */
    @JsonIgnore
    private String aiModelNos;

    /**
     * 计划模式。
     * <p>示例：{@code "IMMEDIATE"}、{@code "SCHEDULED"}、{@code "REPEAT"}</p>
     */
    private String planMode;
    /**
     * 计划状态。
     * <p>示例：{@code "PLANNING"}、{@code "PAUSED"}、{@code "COMPLETED"}、{@code "FAILED"}</p>
     */
    private String status;
    /** 单次或基准执行时间。示例：{@code 2026-05-01 08:30:00}（序列化格式以全局 Jackson 为准） */
    private Date executeTime;
    /** 过期策略。示例：{@code "SKIP_EXPIRED"} */
    private String expirePolicy;
    /**
     * 重复子模式。
     * <p>示例：{@code "DAILY"}、{@code "WEEKLY"}、{@code "MONTHLY"}</p>
     */
    private String repeatMode;
    /**
     * 每日执行时刻串（库内存储，多时刻逗号拼接）。
     * <p>示例：{@code "06:30,14:00"}</p>
     */
    private String dailyTimes;
    /**
     * 按周重复的星期串（库内存储，逗号分隔数字）。
     * <p>示例：{@code "1,3,5"}（周一、三、五）</p>
     */
    private String weekdays;
    /**
     * 按月重复的日期串（库内存储，逗号分隔）。
     * <p>示例：{@code "1,15"}</p>
     */
    private String monthDays;
    /** 按周/按月时的每日执行时刻。示例：{@code "08:00"} */
    private String startTime;
    /** 重复计划生效起点。示例：{@code 2026-04-01 00:00:00} */
    private Date effectiveStartAt;
    /** 重复计划生效终点。示例：{@code 2026-10-31 23:59:59} */
    private Date effectiveEndAt;
    /** 下次触发时间；无后续则为 {@code null}。示例：{@code 2026-05-02 06:30:00} */
    private Date nextOccurrenceAt;
    /** 最近一次错误摘要。示例：{@code "SKIP_EXPIRED"}、{@code "飞控返回超时"} */
    private String lastError;
    /** 创建时间 */
    private Date createTime;
    /** 更新时间 */
    private Date updateTime;

    /**
     * 多模型编号列表；由存储字段解析，无则回退 {@link #aiModelNo}。
     * <p>JSON 属性名为 {@code aiModelNos}。</p>
     * <p>示例：{@code ["CROP_HEALTH_V1", "WEED_SEG_V2"]}</p>
     */
    @JsonProperty("aiModelNos")
    public List<String> getAiModelNoList() {
        return SfUavAiModelNoSupport.splitWithFallback(aiModelNos, aiModelNo);
    }
}
