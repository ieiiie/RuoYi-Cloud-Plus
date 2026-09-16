package com.ym.agriculture.farmtask.workorder.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ym.agriculture.shared.i18n.annotation.StaskI18nField;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskFlowLog;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.util.Date;

/**
 * stask 工单流转日志视图对象。
 */
@Data
@AutoMapper(target = SfStaskFlowLog.class)
public class SfStaskFlowLogVo {

    /**
     * 流转日志主键，仅用于服务端定位翻译资源。
     */
    @JsonIgnore
    private Long logId;

    /**
     * 流转前状态。
     */
    private String fromStatus;

    /**
     * 流转后状态。
     */
    private String toStatus;

    /**
     * 流转事件。
     */
    private String event;

    /**
     * 操作人员工ID。
     */
    private Long operatorEmployeeId;

    /**
     * 操作人员姓名。
     */
    @StaskI18nField(resourceType = I18nResourceType.SYSTEM_EMPLOYEE,
        idProperty = "operatorEmployeeId", fieldKey = "name")
    private String operatorEmployeeName;

    /**
     * 操作人角色编码。
     */
    private String operatorRoleCode;

    /**
     * 操作人角色名称，对应 ym.employee.app-roles 配置中的 name。
     */
    private String operatorRoleName;

    /**
     * 流转说明。
     */
    @StaskI18nField(resourceType = I18nResourceType.STASK_FLOW_LOG,
        idProperty = "logId", fieldKey = "remark")
    private String remark;

    /**
     * 创建时间。
     */
    private Date createTime;
}
