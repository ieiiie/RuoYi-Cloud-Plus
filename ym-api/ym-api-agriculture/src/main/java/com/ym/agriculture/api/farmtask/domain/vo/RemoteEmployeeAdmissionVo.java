package com.ym.agriculture.api.farmtask.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 员工小程序业务准入结果。 */
@Data
public class RemoteEmployeeAdmissionVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private boolean allowed;
    private String message;
    private Long employeeId;
    private Long userId;
    private String tenantId;
    private String name;
    private String appRoleCode;
    private String appRoleName;
    private String reviewStatus;
}
