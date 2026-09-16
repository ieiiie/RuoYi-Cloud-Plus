package com.ym.agriculture.farmtask.worker.model.vo;

import com.ym.agriculture.shared.i18n.annotation.StaskI18nField;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import com.ym.agriculture.farmtask.worker.model.entity.SfStaskWorkerSkill;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * stask 工人农事技能视图对象。
 */
@Data
@AutoMapper(target = SfStaskWorkerSkill.class)
public class SfStaskWorkerSkillVo {

    /**
     * 技能记录主键。
     */
    private Long skillId;

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
     * 农事项目ID。
     */
    private Long workItemId;

    /**
     * 农事项目名称。
     */
    @StaskI18nField(resourceType = I18nResourceType.FARM_WORK_DICT,
        idProperty = "workItemId", fieldKey = "dictName")
    private String workItemName;

    /**
     * 作物类型编码，来源于 sf_crop_species.species_code；ALL 表示全部作物。
     */
    private String cropType;

    /**
     * 作物类型名称；ALL 时为「全部作物」。
     */
    private String cropTypeName;

    /**
     * 技能等级：ADVANCED-高级 MEDIUM-中级 JUNIOR-初级。
     */
    private String skillLevel;

    /**
     * 累计从事次数，系统统计字段。
     */
    private Integer workCount;

    /**
     * 最近一次作业日期，系统统计字段。
     */
    private Date lastWorkDate;

    /**
     * 平均得分预留字段，当前不计算。
     */
    private BigDecimal averageScore;
}
