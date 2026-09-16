package com.ym.agriculture.farmtask.worker.model.vo;

import com.ym.agriculture.shared.i18n.annotation.StaskI18nField;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * stask 小程序人员详情视图。
 */
@Data
public class SfStaskWorkerDetailVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 员工ID。
     */
    private Long employeeId;

    /**
     * 员工姓名。
     */
    @StaskI18nField(resourceType = I18nResourceType.SYSTEM_EMPLOYEE,
        idProperty = "employeeId", fieldKey = "name")
    private String employeeName;

    /**
     * 姓名，兼容前端通用字段。
     */
    @StaskI18nField(resourceType = I18nResourceType.SYSTEM_EMPLOYEE,
        idProperty = "employeeId", fieldKey = "name")
    private String name;

    /**
     * 手机号。
     */
    private String phone;

    /**
     * 头像或照片 OSS 地址。
     */
    private String photo;

    /**
     * 性别编码：0-男，1-女。
     */
    private String gender;

    /**
     * 性别名称。
     */
    private String genderName;

    /**
     * 出生日期。
     */
    private Date birthDate;

    /**
     * 年龄，根据出生日期计算。
     */
    private Integer age;

    /**
     * 人员类型编码：internal-内部人员，external-外部人员。
     */
    private String personType;

    /**
     * 人员类型名称。
     */
    private String personTypeName;

    /**
     * 小程序应用角色编码。
     */
    private String appRoleCode;

    /**
     * 小程序应用角色名称。
     */
    private String appRoleName;

    /**
     * 人员状态：0-在职/正常，1-离职/停用。
     */
    private String status;

    /**
     * 人员状态名称。
     */
    private String statusName;

    /**
     * 农事技能信息。
     */
    private List<SfStaskWorkerSkillDetailVo> skills;

    /**
     * 工人最近任务记录，非工人角色返回空列表。
     */
    private List<SfStaskWorkerRecentTaskVo> recentTasks;

    /**
     * 组长管理的大棚农事，非组长角色返回空列表。
     */
    private List<SfStaskLeaderManagedFarmWorkVo> managedFarmWorks;
}
