package com.ym.agriculture.farmtask.worker.model.bo;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

/**
 * stask 工人农事技能批量保存入参。
 */
@Data
public class SfStaskWorkerSkillBatchBo {

    /**
     * 技能列表。
     */
    @Valid
    private List<SfStaskWorkerSkillBo> skills;
}
