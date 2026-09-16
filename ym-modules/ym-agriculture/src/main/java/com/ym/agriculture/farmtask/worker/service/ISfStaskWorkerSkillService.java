package com.ym.agriculture.farmtask.worker.service;

import com.ym.agriculture.farmtask.worker.model.bo.SfStaskWorkerSkillBatchBo;
import com.ym.agriculture.farmtask.worker.model.entity.SfStaskWorkerSkill;
import com.ym.agriculture.farmtask.worker.model.vo.SfStaskWorkerSkillVo;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * stask 工人农事技能服务接口。
 */
public interface ISfStaskWorkerSkillService {

    /**
     * 查询员工技能列表。
     *
     * @param employeeId 员工ID
     * @return 技能列表
     */
    List<SfStaskWorkerSkillVo> queryByEmployeeId(Long employeeId);

    /**
     * 批量查询员工技能列表。
     *
     * @param tenantId    租户编号
     * @param employeeIds 员工ID集合
     * @return 员工ID到技能列表的映射
     */
    Map<Long, List<SfStaskWorkerSkillVo>> queryByEmployeeIds(String tenantId, Collection<Long> employeeIds);

    /**
     * 批量保存员工技能。
     *
     * @param employeeId 员工ID
     * @param bo         技能列表
     * @return 保存数量
     */
    int saveEmployeeSkills(Long employeeId, SfStaskWorkerSkillBatchBo bo);

    /**
     * 删除技能记录。
     *
     * @param skillId 技能记录ID
     * @return 影响行数
     */
    int deleteById(Long skillId);

    /**
     * 查询多个员工指定农事项技能。
     *
     * @param tenantId    租户编号
     * @param employeeIds 员工ID集合
     * @param workItemId  农事项目ID
     * @return 员工ID到技能的映射
     */
    Map<Long, SfStaskWorkerSkill> queryBestSkillMap(String tenantId, Collection<Long> employeeIds, Long workItemId);

    /**
     * 验收通过后累计工人对应农事项目的从事次数。
     *
     * @param tenantId   租户编号
     * @param employeeId 员工 ID
     * @param workItemId 农事项目 ID
     * @param workDate   作业完成日期（通常为验收时间）
     */
    void recordWorkCompletion(String tenantId, Long employeeId, Long workItemId, Date workDate);

    /**
     * 验收通过后批量累计多个工人对应农事项目的从事次数。
     *
     * @param tenantId   租户编号
     * @param employeeIds 员工 ID 集合
     * @param workItemId 农事项目 ID
     * @param workDate   作业完成日期（通常为验收时间）
     */
    void recordWorkCompletions(String tenantId, Collection<Long> employeeIds, Long workItemId, Date workDate);
}
