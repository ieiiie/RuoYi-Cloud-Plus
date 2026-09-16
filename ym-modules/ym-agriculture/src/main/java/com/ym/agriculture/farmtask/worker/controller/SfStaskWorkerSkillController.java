package com.ym.agriculture.farmtask.worker.controller;

import com.ym.common.core.domain.R;
import com.ym.common.web.core.BaseController;
import com.ym.agriculture.farmtask.worker.model.bo.SfStaskWorkerSkillBatchBo;
import com.ym.agriculture.farmtask.worker.model.vo.SfStaskWorkerSkillVo;
import com.ym.agriculture.farmtask.worker.service.ISfStaskWorkerSkillService;
import com.ym.agriculture.farmtask.workorder.support.SfStaskWorkerFeatureDeprecation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * stask 工人农事技能接口。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/smart-farming/stask/worker-skill")
public class SfStaskWorkerSkillController extends BaseController {

    private final ISfStaskWorkerSkillService workerSkillService;
    private final SfStaskWorkerFeatureDeprecation workerFeatureDeprecation;

    /**
     * 查询员工农事技能列表。
     *
     * @param employeeId 员工ID
     * @return 技能列表
     */
    @GetMapping("/employee/{employeeId}")
    @Deprecated
    public R<List<SfStaskWorkerSkillVo>> listByEmployee(@NotNull @PathVariable Long employeeId) {
        workerFeatureDeprecation.reject();
        return R.ok(workerSkillService.queryByEmployeeId(employeeId));
    }

    /**
     * 批量保存员工农事技能。
     *
     * @param employeeId 员工ID
     * @param bo         技能列表
     * @return 保存数量
     */
    @PutMapping("/employee/{employeeId}")
    @Deprecated
    public R<Integer> saveByEmployee(@NotNull @PathVariable Long employeeId, @Valid @RequestBody SfStaskWorkerSkillBatchBo bo) {
        workerFeatureDeprecation.reject();
        return R.ok(workerSkillService.saveEmployeeSkills(employeeId, bo));
    }

    /**
     * 删除农事技能记录。
     *
     * @param skillId 技能记录ID
     * @return 操作结果
     */
    @DeleteMapping("/{skillId}")
    @Deprecated
    public R<Void> delete(@NotNull @PathVariable Long skillId) {
        workerFeatureDeprecation.reject();
        return toAjax(workerSkillService.deleteById(skillId));
    }
}
