package com.ym.agriculture.farmtask.workorder.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchVo;
import lombok.Data;

import java.util.Date;
import java.util.List;

/** 后台具体任务只读详情，不包含 SOP、语音和写操作权限字段。 */
@Data
public class SfStaskAdminTaskDetailVo {
    private Long orderId;
    private String orderNo;
    private Long packageId;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private Date planDate;
    private Long greenhouseId;
    private String greenhouseNameSnapshot;
    private List<SfStaskGreenhouseBriefVo> greenhouses;
    private List<SfPlantingBatchVo> plantingBatches;
    private Long workItemId;
    private String workItemNameSnapshot;
    private Long leaderId;
    private String leaderName;
    private Long creatorEmployeeId;
    private String creatorEmployeeName;
    private String creatorRoleCode;
    private String creatorRoleName;
    private String status;
    private String statusLabel;
    private String acceptanceResult;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date acceptedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date completedAt;
    private String eventTimeLabel;
    private Date eventTime;
    private Double requiredWorkerCount;
    private Integer acceptedWorkerCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;
    private String managerRequirement;
    private String managerPhotos;
    private String techInstruction;
    private String techPhotos;
    private String overallTechNote;
    private String workPhotos;
    private String completionRemark;
    private Long completionId;
    private List<SfStaskCompletionHistoryVo> completionHistory;
    private String rejectReason;
    private Long acceptanceId;
    private String acceptancePhotos;
    private Long acceptorEmployeeId;
    private String acceptorEmployeeName;
    private String acceptorRoleCode;
    private String acceptorRoleName;
    private String techRejectReason;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date techRejectedAt;
    private String techRejectOperatorEmployeeName;
    private List<SfStaskDispatchVo> dispatches;
    private List<SfStaskDispatchVo> workWorkers;
    private List<SfStaskFlowLogVo> flowLogs;
}
