package com.ym.agriculture.farmtask.worker.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farmtask.worker.model.entity.SfStaskWorkerSkill;
import com.ym.agriculture.farmtask.worker.model.vo.SfStaskWorkerSkillVo;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * stask 工人农事技能 Mapper。
 */
@Mapper
public interface SfStaskWorkerSkillMapper extends BaseMapperPlus<SfStaskWorkerSkill, SfStaskWorkerSkillVo> {

    /**
     * 查询员工技能列表。
     *
     * @param tenantId   租户编号
     * @param employeeId 员工ID
     * @return 技能列表
     */
    default List<SfStaskWorkerSkill> selectByEmployeeId(String tenantId, Long employeeId) {
        return selectList(Wrappers.<SfStaskWorkerSkill>lambdaQuery()
            .eq(SfStaskWorkerSkill::getTenantId, tenantId)
            .eq(SfStaskWorkerSkill::getEmployeeId, employeeId)
            .orderByAsc(SfStaskWorkerSkill::getWorkItemId)
            .orderByAsc(SfStaskWorkerSkill::getCropType));
    }

    /**
     * 查询多个员工的指定农事项技能。
     *
     * @param tenantId    租户编号
     * @param employeeIds 员工ID集合
     * @param workItemId  农事项目ID
     * @return 技能列表
     */
    default List<SfStaskWorkerSkill> selectByEmployeesAndWorkItem(String tenantId, Collection<Long> employeeIds, Long workItemId) {
        if (employeeIds == null || employeeIds.isEmpty() || workItemId == null) {
            return List.of();
        }
        return selectList(Wrappers.<SfStaskWorkerSkill>lambdaQuery()
            .eq(SfStaskWorkerSkill::getTenantId, tenantId)
            .in(SfStaskWorkerSkill::getEmployeeId, employeeIds)
            .eq(SfStaskWorkerSkill::getWorkItemId, workItemId));
    }

    /**
     * 批量查询员工技能列表。
     *
     * @param tenantId    租户编号
     * @param employeeIds 员工ID集合
     * @return 技能列表
     */
    default List<SfStaskWorkerSkill> selectByEmployeeIds(String tenantId, Collection<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfStaskWorkerSkill>lambdaQuery()
            .eq(SfStaskWorkerSkill::getTenantId, tenantId)
            .in(SfStaskWorkerSkill::getEmployeeId, employeeIds)
            .orderByAsc(SfStaskWorkerSkill::getEmployeeId)
            .orderByAsc(SfStaskWorkerSkill::getWorkItemId)
            .orderByAsc(SfStaskWorkerSkill::getCropType));
    }

    /**
     * 删除员工全部技能。
     *
     * @param tenantId   租户编号
     * @param employeeId 员工ID
     * @return 删除行数
     */
    default int deleteByEmployeeId(String tenantId, Long employeeId) {
        return delete(Wrappers.<SfStaskWorkerSkill>lambdaQuery()
            .eq(SfStaskWorkerSkill::getTenantId, tenantId)
            .eq(SfStaskWorkerSkill::getEmployeeId, employeeId));
    }
}
