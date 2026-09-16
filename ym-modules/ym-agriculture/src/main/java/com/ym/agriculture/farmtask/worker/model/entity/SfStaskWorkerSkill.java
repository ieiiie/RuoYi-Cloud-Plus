package com.ym.agriculture.farmtask.worker.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * stask 工人农事技能，表 {@code sf_stask_worker_skill}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_stask_worker_skill")
public class SfStaskWorkerSkill extends TenantEntity {

    /**
     * 技能记录主键。
     */
    @TableId("skill_id")
    private Long skillId;

    /**
     * 员工ID，对应 sys_employee.employee_id。
     */
    private Long employeeId;

    /**
     * 农事项目ID，对应 sf_farm_work_dict.dict_id。
     */
    private Long workItemId;

    /**
     * 作物类型编码，来源于 sf_crop_species.species_code；ALL 表示全部作物。
     */
    private String cropType;

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
