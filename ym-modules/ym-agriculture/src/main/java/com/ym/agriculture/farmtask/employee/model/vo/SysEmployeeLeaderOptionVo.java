package com.ym.agriculture.farmtask.employee.model.vo;

import com.ym.agriculture.farmtask.employee.model.entity.SysEmployee;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * stask 农事分配组长候选视图对象。
 */
@Data
@AutoMapper(target = SysEmployee.class)
public class SysEmployeeLeaderOptionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 员工ID。
     */
    private Long employeeId;

    /**
     * 姓名。
     */
    private String name;

    /**
     * 手机号。
     */
    private String phone;

    /**
     * 性别：0男 1女 2未知。
     */
    private String gender;

    /**
     * 出生日期。
     */
    private Date birthDate;

    /**
     * 年龄，根据出生日期计算。
     */
    private Integer age;

    /**
     * 应用角色编码，固定为 stask:group_leader。
     */
    private String appRoleCode;

    /**
     * 人员状态：0正常 1停用。
     */
    private String status;

    /**
     * 应用角色名称，对应 ym.employee.app-roles 配置中的 name。
     */
    private String appRoleName;

    /**
     * 匹配农事项的技能等级：ADVANCED-高级 MEDIUM-中级 JUNIOR-初级。
     */
    private String skillLevel;

    /**
     * 累计从事次数。
     */
    private Integer workCount;

    /**
     * 最近一次作业日期。
     */
    private Date lastWorkDate;

    /**
     * 平均得分预留字段，当前不计算。
     */
    private BigDecimal averageScore;
}
