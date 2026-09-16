package com.ym.agriculture.farmtask.inspection.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDate;
import java.util.Date;

/** 抽检列表项。 */
@Data
public class SfStaskInspectionListVo {

    /** 抽检记录 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long inspectionId;
    /** 问题发现日期。 */
    private LocalDate foundDate;

    /** 大棚名称快照。 */
    private String greenhouseNameSnapshot;
    /** 大棚 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long greenhouseId;
    /** 物种名称快照。 */
    private String cropNameSnapshot;
    /** 物种 ID 快照。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long cropIdSnapshot;
    /** 品种名称快照。 */
    private String cropVarietyNameSnapshot;
    /** 品种 ID 快照。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long cropVarietyIdSnapshot;
    /** 农事项名称快照。 */
    private String workItemNameSnapshot;
    /** 农事项 ID 快照。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long workItemIdSnapshot;
    /** 种植批次 ID 快照。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long plantingBatchIdSnapshot;
    /** 关联拆分工单 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long orderId;

    /** 负责技术员员工 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long responsibleTechnicianEmployeeId;
    /** 负责技术员姓名快照。 */
    private String responsibleTechnicianNameSnapshot;
    /** 负责组长员工 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long responsibleLeaderEmployeeId;
    /** 负责组长姓名快照。 */
    private String responsibleLeaderNameSnapshot;
    /** 问题发现人员工 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long discovererEmployeeId;
    /** 问题发现人姓名快照。 */
    private String discovererNameSnapshot;

    /** 严重程度编码。 */
    private String severity;
    /** 严重程度本地化名称。 */
    private String severityLabel;
    /** 处理状态编码。 */
    private String status;
    /** 处理状态本地化名称。 */
    private String statusLabel;
    /** 问题描述。 */
    private String problemDescription;
    /** 问题照片数量。 */
    private Integer problemPhotoCount;
    /** 最后更新时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
    /** 乐观锁版本。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long version;

    /** 当前员工是否可以创建。 */
    private Boolean canCreate;
    /** 当前员工是否可以编辑。 */
    private Boolean canEdit;
    /** 当前员工是否可以删除。 */
    private Boolean canDelete;
    /** 当前员工是否可以处理。 */
    private Boolean canHandle;
    /** 当前员工是否可以打回。 */
    private Boolean canReject;
    /** 当前员工只读或字段受限原因。 */
    private String readonlyReason;
}
