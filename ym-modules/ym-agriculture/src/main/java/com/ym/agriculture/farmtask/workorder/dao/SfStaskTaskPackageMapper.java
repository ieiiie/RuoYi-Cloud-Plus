package com.ym.agriculture.farmtask.workorder.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskCreatorRole;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskTaskPackage;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * stask 任务包 Mapper。
 */
@Mapper
public interface SfStaskTaskPackageMapper extends BaseMapperPlus<SfStaskTaskPackage, SfStaskTaskPackage> {

    /**
     * 统计创建人指定状态的任务包数量。
     *
     * @param tenantId   租户编号
     * @param employeeId 创建人员工 ID
     * @param statuses   状态集合
     * @return 数量
     */
    default long countByCreatorAndStatuses(String tenantId, Long employeeId, Collection<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return 0L;
        }
        return selectCount(Wrappers.<SfStaskTaskPackage>lambdaQuery()
            .eq(SfStaskTaskPackage::getTenantId, tenantId)
            .eq(SfStaskTaskPackage::getCreatorEmployeeId, employeeId)
            .in(SfStaskTaskPackage::getStatus, statuses));
    }

    /**
     * 查询创建人的任务包列表。
     *
     * @param tenantId   租户编号
     * @param employeeId 创建人员工 ID
     * @param statuses   状态集合
     * @return 任务包列表
     */
    default List<SfStaskTaskPackage> selectByCreatorAndStatuses(String tenantId, Long employeeId,
        Collection<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfStaskTaskPackage>lambdaQuery()
            .eq(SfStaskTaskPackage::getTenantId, tenantId)
            .eq(SfStaskTaskPackage::getCreatorEmployeeId, employeeId)
            .in(SfStaskTaskPackage::getStatus, statuses)
            .orderByAsc(SfStaskTaskPackage::getPlanDate)
            .orderByDesc(SfStaskTaskPackage::getCreateTime));
    }

    /**
     * 按通用列表条件查询任务包。
     *
     * @param tenantId      租户编号
     * @param status        任务包状态
     * @param planStartDate 计划开始日期
     * @param planEndDate   计划结束日期
     * @return 任务包列表
     */
    default List<SfStaskTaskPackage> selectByPageFilters(String tenantId, String status, Date planStartDate,
        Date planEndDate) {
        return selectList(Wrappers.<SfStaskTaskPackage>lambdaQuery()
            .eq(SfStaskTaskPackage::getTenantId, tenantId)
            .eq(status != null && !status.isBlank(), SfStaskTaskPackage::getStatus, status)
            .ge(planStartDate != null, SfStaskTaskPackage::getPlanDate, planStartDate)
            .le(planEndDate != null, SfStaskTaskPackage::getPlanDate, planEndDate)
            .orderByDesc(SfStaskTaskPackage::getCreateTime));
    }

    /**
     * 查询生产管理员全部任务页可见任务包（非草稿租户全量 + 本人草稿）。
     *
     * @param tenantId      租户编号
     * @param employeeId    当前员工 ID
     * @param statuses      状态集合
     * @param greenhouseIds 大棚 ID 集合
     * @param workItemIds   农事项目 ID 集合
     * @param planStartDate 计划开始日期
     * @param planEndDate   计划结束日期
     * @return 任务包列表
     */
    default List<SfStaskTaskPackage> selectManagerAllTasks(String tenantId, Long employeeId,
        Collection<String> statuses, Collection<Long> greenhouseIds, Collection<Long> workItemIds,
        Date planStartDate, Date planEndDate) {
        return selectManagerAllTasks(tenantId, employeeId, statuses, greenhouseIds, workItemIds,
            planStartDate, planEndDate, null);
    }

    /**
     * 查询生产管理员全部任务，并按发起人筛选。
     */
    default List<SfStaskTaskPackage> selectManagerAllTasks(String tenantId, Long employeeId,
        Collection<String> statuses, Collection<Long> greenhouseIds, Collection<Long> workItemIds,
        Date planStartDate, Date planEndDate, Long creatorEmployeeId) {
        LambdaQueryWrapper<SfStaskTaskPackage> wrapper = buildAllTaskPackageWrapper(
            tenantId, statuses, greenhouseIds, workItemIds, planStartDate, planEndDate);
        wrapper.eq(creatorEmployeeId != null, SfStaskTaskPackage::getCreatorEmployeeId, creatorEmployeeId);
        wrapper.and(w -> w.ne(SfStaskTaskPackage::getStatus, StaskOrderStatus.DRAFT)
            .or().eq(SfStaskTaskPackage::getCreatorEmployeeId, employeeId));
        return selectList(wrapper);
    }

    /**
     * 查询租户内全部任务包，供领导只读总览使用。
     */
    default List<SfStaskTaskPackage> selectLeaderAdminAllTasks(String tenantId,
        Collection<String> statuses, Collection<Long> greenhouseIds, Collection<Long> workItemIds,
        Date planStartDate, Date planEndDate) {
        return selectLeaderAdminAllTasks(tenantId, statuses, greenhouseIds, workItemIds,
            planStartDate, planEndDate, null);
    }

    /**
     * 查询领导全部任务，并按发起人筛选。
     */
    default List<SfStaskTaskPackage> selectLeaderAdminAllTasks(String tenantId,
        Collection<String> statuses, Collection<Long> greenhouseIds, Collection<Long> workItemIds,
        Date planStartDate, Date planEndDate, Long creatorEmployeeId) {
        return selectList(buildAllTaskPackageWrapper(tenantId, statuses, greenhouseIds, workItemIds,
            planStartDate, planEndDate)
            .eq(creatorEmployeeId != null, SfStaskTaskPackage::getCreatorEmployeeId, creatorEmployeeId)
            .ne(SfStaskTaskPackage::getStatus, StaskOrderStatus.DRAFT));
    }

    /**
     * 查询技术员全部任务页可见任务包（生产管理员已提交 + 本人创建含草稿）。
     *
     * @param tenantId      租户编号
     * @param employeeId    当前员工 ID
     * @param draftStatus   草稿状态编码
     * @param statuses      状态集合
     * @param greenhouseIds 大棚 ID 集合
     * @param workItemIds   农事项目 ID 集合
     * @param planStartDate 计划开始日期
     * @param planEndDate   计划结束日期
     * @return 任务包列表
     */
    default List<SfStaskTaskPackage> selectTechnicianAllTasks(String tenantId, Long employeeId, String draftStatus,
        Collection<String> statuses, Collection<Long> greenhouseIds, Collection<Long> workItemIds,
        Date planStartDate, Date planEndDate) {
        return selectTechnicianAllTasks(tenantId, employeeId, draftStatus, statuses, greenhouseIds, workItemIds,
            planStartDate, planEndDate, true);
    }

    /**
     * 查询技术员全部任务；相关范围为本人发起/经手，并保留公共待技术审核任务。
     */
    default List<SfStaskTaskPackage> selectTechnicianAllTasks(String tenantId, Long employeeId, String draftStatus,
        Collection<String> statuses, Collection<Long> greenhouseIds, Collection<Long> workItemIds,
        Date planStartDate, Date planEndDate, boolean relatedOnly) {
        return selectTechnicianAllTasks(tenantId, employeeId, draftStatus, statuses, greenhouseIds, workItemIds,
            planStartDate, planEndDate, relatedOnly, null);
    }

    /**
     * 查询技术员全部任务，并按发起人筛选。
     */
    default List<SfStaskTaskPackage> selectTechnicianAllTasks(String tenantId, Long employeeId, String draftStatus,
        Collection<String> statuses, Collection<Long> greenhouseIds, Collection<Long> workItemIds,
        Date planStartDate, Date planEndDate, boolean relatedOnly, Long creatorEmployeeId) {
        LambdaQueryWrapper<SfStaskTaskPackage> wrapper = buildAllTaskPackageWrapper(
            tenantId, statuses, greenhouseIds, workItemIds, planStartDate, planEndDate);
        wrapper.eq(creatorEmployeeId != null, SfStaskTaskPackage::getCreatorEmployeeId, creatorEmployeeId);
        if (relatedOnly) {
            wrapper.and(w -> w.eq(SfStaskTaskPackage::getCreatorEmployeeId, employeeId)
                .or().eq(SfStaskTaskPackage::getHandlerTechnicianEmployeeId, employeeId)
                .or(admin -> admin
                    .eq(SfStaskTaskPackage::getCreatorRoleCode, StaskCreatorRole.PRODUCTION_ADMIN)
                    .eq(SfStaskTaskPackage::getStatus, StaskOrderStatus.PENDING_TECH_CONFIRM)));
        } else {
            wrapper.and(w -> w.ne(SfStaskTaskPackage::getStatus, draftStatus)
                .or().eq(SfStaskTaskPackage::getCreatorEmployeeId, employeeId));
        }
        return selectList(wrapper);
    }

    /**
     * 构建全部任务页任务包基础查询条件。
     *
     * @param tenantId      租户编号
     * @param statuses      状态集合
     * @param greenhouseIds 大棚 ID 集合
     * @param workItemIds   农事项目 ID 集合
     * @param planStartDate 计划开始日期
     * @param planEndDate   计划结束日期
     * @return 查询条件
     */
    default LambdaQueryWrapper<SfStaskTaskPackage> buildAllTaskPackageWrapper(String tenantId,
        Collection<String> statuses, Collection<Long> greenhouseIds, Collection<Long> workItemIds,
        Date planStartDate, Date planEndDate) {
        LambdaQueryWrapper<SfStaskTaskPackage> wrapper = Wrappers.<SfStaskTaskPackage>lambdaQuery()
            .eq(SfStaskTaskPackage::getTenantId, tenantId)
            .in(statuses != null && !statuses.isEmpty(), SfStaskTaskPackage::getStatus, statuses)
            .ge(planStartDate != null, SfStaskTaskPackage::getPlanDate, planStartDate)
            .le(planEndDate != null, SfStaskTaskPackage::getPlanDate, planEndDate)
            .orderByAsc(SfStaskTaskPackage::getPlanDate)
            .orderByDesc(SfStaskTaskPackage::getCreateTime);
        if (greenhouseIds != null && !greenhouseIds.isEmpty()) {
            String ids = greenhouseIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            wrapper.apply("exists (select 1 from sf_stask_work_order_greenhouse g "
                + "where g.tenant_id = {0} and g.package_id = sf_stask_task_package.package_id "
                + "and g.greenhouse_id in (" + ids + "))", tenantId);
        }
        if (workItemIds != null && !workItemIds.isEmpty()) {
            String ids = workItemIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            wrapper.apply("exists (select 1 from sf_stask_work_order_item i "
                + "where i.tenant_id = {0} and i.package_id = sf_stask_task_package.package_id "
                + "and i.work_item_id in (" + ids + "))", tenantId);
        }
        return wrapper;
    }

    /**
     * 查询租户内指定状态与创建角色的任务包。
     *
     * @param tenantId        租户编号
     * @param status          任务包状态
     * @param creatorRoleCode 创建人角色编码
     * @return 任务包列表
     */
    default List<SfStaskTaskPackage> selectByStatusAndCreatorRole(String tenantId, String status,
        String creatorRoleCode) {
        return selectList(Wrappers.<SfStaskTaskPackage>lambdaQuery()
            .eq(SfStaskTaskPackage::getTenantId, tenantId)
            .eq(SfStaskTaskPackage::getStatus, status)
            .eq(SfStaskTaskPackage::getCreatorRoleCode, creatorRoleCode)
            .orderByDesc(SfStaskTaskPackage::getCreateTime));
    }

    /**
     * 统计租户内指定状态与创建角色的任务包数量。
     *
     * @param tenantId        租户编号
     * @param status          任务包状态
     * @param creatorRoleCode 创建人角色编码
     * @return 数量
     */
    default long countByStatusAndCreatorRole(String tenantId, String status, String creatorRoleCode) {
        return selectCount(Wrappers.<SfStaskTaskPackage>lambdaQuery()
            .eq(SfStaskTaskPackage::getTenantId, tenantId)
            .eq(SfStaskTaskPackage::getStatus, status)
            .eq(SfStaskTaskPackage::getCreatorRoleCode, creatorRoleCode));
    }
}
