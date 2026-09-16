package com.ym.agriculture.farmtask.worker.model.vo;

import com.ym.agriculture.shared.i18n.annotation.StaskI18nField;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * stask 小程序人员列表卡片视图。
 */
@Data
public class SfStaskWorkerListItemVo implements Serializable {

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
     * 姓名，兼容卡片展示字段。
     */
    @StaskI18nField(resourceType = I18nResourceType.SYSTEM_EMPLOYEE,
        idProperty = "employeeId", fieldKey = "name")
    private String name;

    /**
     * 手机号。
     */
    private String phone;

    /**
     * 照片 OSS URL。
     */
    private String photo;

    /**
     * 性别：0-男，1-女，2-未知。
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
     * 年龄。
     */
    private Integer age;

    /**
     * 人员类型：0-内部人员，1-外部人员。
     */
    private String personType;

    /**
     * 人员类型名称。
     */
    private String personTypeName;

    /**
     * 应用角色编码。
     */
    private String appRoleCode;

    /**
     * 应用角色名称。
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
     * 是否存在未完成任务。
     */
    private Boolean hasUnfinishedTask;

    /**
     * 卡片标签，含角色、状态、技能等展示文案。
     */
    private List<String> tags;

    /**
     * 农事技能标签列表。
     */
    private List<SfStaskWorkerSkillTagVo> skills;
}
