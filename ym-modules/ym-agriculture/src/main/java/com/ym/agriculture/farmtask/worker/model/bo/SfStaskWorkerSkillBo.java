package com.ym.agriculture.farmtask.worker.model.bo;

import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import com.ym.agriculture.farmtask.worker.model.entity.SfStaskWorkerSkill;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * stask 工人农事技能编辑入参。
 */
@Data
@AutoMapper(target = SfStaskWorkerSkill.class, reverseConvertGenerate = false)
public class SfStaskWorkerSkillBo {

    /**
     * 技能记录主键，新增时为空。
     */
    private Long skillId;

    /**
     * 员工ID，对应 sys_employee.employee_id。
     */
    private Long employeeId;

    /**
     * 农事项目ID，对应 sf_farm_work_dict.dict_id。
     */
    @NotNull(message = "{" + StaskMessageKeys.VALIDATION_WORK_ITEM_REQUIRED + "}")
    private Long workItemId;

    /**
     * 作物类型编码，来源于 sf_crop_species.species_code；ALL 表示全部作物。
     */
    @NotBlank(message = "{" + StaskMessageKeys.VALIDATION_CROP_TYPE_REQUIRED + "}")
    private String cropType;

    /**
     * 技能等级：ADVANCED-高级 MEDIUM-中级 JUNIOR-初级。
     */
    @NotBlank(message = "{" + StaskMessageKeys.VALIDATION_SKILL_LEVEL_REQUIRED + "}")
    private String skillLevel;
}
