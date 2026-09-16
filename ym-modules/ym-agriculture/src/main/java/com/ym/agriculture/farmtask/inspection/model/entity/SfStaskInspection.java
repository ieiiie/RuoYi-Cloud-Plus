package com.ym.agriculture.farmtask.inspection.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDate;
import java.util.Date;

/**
 * stask 农事抽检记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_stask_inspection")
public class SfStaskInspection extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 抽检记录主键。 */
    @TableId(value = "inspection_id", type = IdType.ASSIGN_ID)
    private Long inspectionId;

    /** 问题发现日期。 */
    private LocalDate foundDate;
    /** 严重程度：NORMAL-一般，SERIOUS-严重。 */
    private String severity;
    /** 处理状态：UNPROCESSED-未处理，PROCESSED-已处理。 */
    private String status;
    /** 问题描述，最多500字。 */
    private String problemDescription;
    /** 问题照片，JSON数组，保存规范化 OSS ID。 */
    private String problemPhotoJson;

    /** 大棚 ID。 */
    private Long greenhouseId;
    /** 创建时的大棚名称快照。 */
    private String greenhouseNameSnapshot;
    /** 创建时的种植批次 ID 快照。 */
    private Long plantingBatchIdSnapshot;
    /** 创建时的物种 ID 快照。 */
    private Long cropIdSnapshot;
    /** 创建时的物种名称快照。 */
    private String cropNameSnapshot;
    /** 创建时的品种 ID 快照。 */
    private Long cropVarietyIdSnapshot;
    /** 创建时的品种名称快照。 */
    private String cropVarietyNameSnapshot;

    /** 关联拆分工单 ID，可空。 */
    private Long orderId;
    /** 创建时的农事项 ID 快照。 */
    private Long workItemIdSnapshot;
    /** 创建时的农事项名称快照。 */
    private String workItemNameSnapshot;

    /** 负责技术员员工 ID。 */
    private Long responsibleTechnicianEmployeeId;
    /** 负责技术员姓名快照。 */
    private String responsibleTechnicianNameSnapshot;
    /** 负责组长员工 ID，可空。 */
    private Long responsibleLeaderEmployeeId;
    /** 负责组长姓名快照。 */
    private String responsibleLeaderNameSnapshot;
    /** 问题发现人员工 ID。 */
    private Long discovererEmployeeId;
    /** 问题发现人姓名快照。 */
    private String discovererNameSnapshot;
    /** 创建人员工 ID。 */
    private Long creatorEmployeeId;
    /** 创建人姓名快照。 */
    private String creatorNameSnapshot;

    /** 首次处理说明。 */
    private String firstHandleDescription;
    /** 首次处理照片，JSON数组，保存规范化 OSS ID。 */
    private String firstHandlePhotoJson;
    /** 首次处理人员工 ID。 */
    private Long firstHandleEmployeeId;
    /** 首次处理人姓名快照。 */
    private String firstHandleEmployeeNameSnapshot;
    /** 首次处理时间。 */
    private Date firstHandledAt;

    /** 当前最新处理说明。 */
    private String currentHandleDescription;
    /** 当前最新处理照片，JSON数组，保存规范化 OSS ID。 */
    private String currentHandlePhotoJson;
    /** 当前最新处理人员工 ID。 */
    private Long currentHandleEmployeeId;
    /** 当前最新处理人姓名快照。 */
    private String currentHandleEmployeeNameSnapshot;
    /** 当前最新处理时间。 */
    private Date currentHandledAt;

    /** 最新打回原因。 */
    private String latestRejectReason;
    /** 最新打回人员工 ID。 */
    private Long latestRejectEmployeeId;
    /** 最新打回人姓名快照。 */
    private String latestRejectEmployeeNameSnapshot;
    /** 最新打回时间。 */
    private Date latestRejectedAt;

    /** 成功处理次数，只增不减。 */
    private Integer handleCount;
    /** 成功打回次数，只增不减。 */
    private Integer rejectCount;

    /** 乐观锁版本。 */
    @Version
    private Long version;

    /** 逻辑删除标志：0-存在，1-删除。 */
    @TableLogic
    private String delFlag;

    /** 删除人员工 ID。 */
    private Long deletedBy;
    /** 删除时间。 */
    private Date deletedAt;
}
