package com.ym.agriculture.farming.farmrecord.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.util.Date;

/**
 * 移动端农事记录主表，对应 {@code sf_farming_record}。
 *
 * @author ym-cloud
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sf_farming_record")
public class SfFarmingRecord extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 农事记录主键。
     */
    @TableId("record_id")
    private Long recordId;

    /**
     * 状态：DRAFT-草稿，SUBMITTED-已提交。
     */
    private String status;

    /**
     * 农事发生时间，格式：yyyy-MM-dd HH:mm:ss。
     */
    private Date happenedAt;

    /**
     * 作业时段 JSON，数组格式：[开始时间, 结束时间]，精确到分钟。
     */
    private String workPeriodJson;

    /**
     * 今日小结（农事总结），选填，最大 500 字；未填时存空字符串。
     */
    private String summary;

    /**
     * 天气快照 JSON，保存 code/name/temperature 等。
     */
    private String weatherJson;

    /**
     * 生长阶段快照 JSON，保存 code/name/reliable/manual/baseDate 等。
     */
    private String growthStageJson;

    /**
     * 资源投入 JSON，保存人工、农机、物料投入。
     */
    private String resourceJson;

    /**
     * 现场反馈 JSON，保存苗情长势等反馈。
     */
    private String feedbackJson;

    /**
     * 地块关联传感器数据快照 JSON，保存失败时允许为空快照。
     */
    private String sensorSnapshotJson;

    /**
     * 环境摘要 JSON，保存土壤湿度、温度、降雨量等摘要。
     */
    private String environmentSummaryJson;

    /**
     * 提交时间，草稿为空。
     */
    private Date submitTime;

    /**
     * 删除标志：0-存在，1-删除。
     */
    @TableLogic
    private String delFlag;

    /**
     * 备注，最大 500 字。
     */
    private String remark;
}
