package com.ym.agriculture.farmtask.workorder.model.vo;

import com.ym.agriculture.shared.i18n.annotation.StaskI18nField;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskDispatch;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.util.Date;

/**
 * stask 派工明细视图对象。
 */
@Data
@AutoMapper(target = SfStaskDispatch.class)
public class SfStaskDispatchVo {

    /**
     * 派工明细主键。
     */
    private Long dispatchId;

    /**
     * 工人员工ID。
     */
    private Long workerId;

    /**
     * 工人姓名。
     */
    @StaskI18nField(resourceType = I18nResourceType.SYSTEM_EMPLOYEE,
        idProperty = "workerId", fieldKey = "name")
    private String workerName;

    /**
     * 工人手机号。
     */
    private String workerPhone;

    /**
     * 派工状态。
     */
    private String status;

    /**
     * 拒绝原因。
     */
    @StaskI18nField(resourceType = I18nResourceType.STASK_DISPATCH,
        idProperty = "dispatchId", fieldKey = "rejectReason")
    private String rejectReason;

    /**
     * 邀请时间。
     */
    private Date invitedAt;

    /**
     * 响应时间。
     */
    private Date respondedAt;
}
