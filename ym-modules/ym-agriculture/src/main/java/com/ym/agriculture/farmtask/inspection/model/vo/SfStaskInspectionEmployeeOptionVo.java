package com.ym.agriculture.farmtask.inspection.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/** 抽检责任人员选项。 */
@Data
public class SfStaskInspectionEmployeeOptionVo {

    /** 员工 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long employeeId;
    /** 员工姓名。 */
    private String employeeName;
    /** 应用角色编码。 */
    private String roleCode;
    /** 应用角色本地化名称。 */
    private String roleName;
    /** 员工当前是否有效。 */
    private Boolean active;
}
