package com.ym.agriculture.farmtask.inspection.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Date;

/** 抽检关联拆分工单候选。 */
@Data
public class SfStaskInspectionOrderOptionVo {

    /** 拆分工单 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long orderId;
    /** 工单展示名称，优先使用工单编号。 */
    private String orderName;
    /** 农事项 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long workItemId;
    /** 农事项名称。 */
    private String workItemName;
    /** 有效责任技术员 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long responsibleTechnicianEmployeeId;
    /** 有效责任技术员姓名。 */
    private String responsibleTechnicianName;
    /** 有效责任组长 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long responsibleLeaderEmployeeId;
    /** 有效责任组长姓名。 */
    private String responsibleLeaderName;
    /** 工单状态编码。 */
    private String orderStatus;
    /** 工单状态本地化名称。 */
    private String orderStatusLabel;
    /** 计划作业日期。 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date plannedDate;
}
