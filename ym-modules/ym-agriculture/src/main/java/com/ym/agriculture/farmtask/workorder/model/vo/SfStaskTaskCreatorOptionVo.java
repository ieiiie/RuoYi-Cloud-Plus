package com.ym.agriculture.farmtask.workorder.model.vo;

import com.ym.agriculture.shared.i18n.annotation.StaskI18nField;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * stask 小程序任务发起人筛选项。
 */
@Data
public class SfStaskTaskCreatorOptionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 发起人员工 ID。
     */
    private Long employeeId;

    /**
     * 发起人姓名。
     */
    @StaskI18nField(resourceType = I18nResourceType.SYSTEM_EMPLOYEE,
        idProperty = "employeeId", fieldKey = "name")
    private String employeeName;

    /**
     * 发起人角色编码。
     */
    private String creatorRoleCode;

    /**
     * 发起人角色名称。
     */
    private String creatorRoleName;
}
