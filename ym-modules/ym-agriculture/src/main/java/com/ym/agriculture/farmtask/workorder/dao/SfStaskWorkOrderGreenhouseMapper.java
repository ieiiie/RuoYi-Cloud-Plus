package com.ym.agriculture.farmtask.workorder.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskTaskScopeBo;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrderGreenhouse;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskTaskScopeConflictVo;
import com.ym.agriculture.farmtask.workorder.support.StaskTaskScopeSqlHelper;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * stask 任务包大棚明细 Mapper。
 */
@Mapper
public interface SfStaskWorkOrderGreenhouseMapper
    extends BaseMapperPlus<SfStaskWorkOrderGreenhouse, SfStaskWorkOrderGreenhouse> {

    /**
     * 查询任务包大棚明细。
     *
     * @param tenantId  租户编号
     * @param packageId 任务包ID
     * @return 大棚明细
     */
    default List<SfStaskWorkOrderGreenhouse> selectByPackageId(String tenantId, Long packageId) {
        return selectList(Wrappers.<SfStaskWorkOrderGreenhouse>lambdaQuery()
            .eq(SfStaskWorkOrderGreenhouse::getTenantId, tenantId)
            .eq(SfStaskWorkOrderGreenhouse::getPackageId, packageId)
            .orderByAsc(SfStaskWorkOrderGreenhouse::getItemId)
            .orderByAsc(SfStaskWorkOrderGreenhouse::getGreenhouseId));
    }

    /**
     * 批量查询任务包大棚明细。
     *
     * @param tenantId   租户编号
     * @param packageIds 任务包ID集合
     * @return 大棚明细
     */
    default List<SfStaskWorkOrderGreenhouse> selectByPackageIds(String tenantId, Collection<Long> packageIds) {
        if (packageIds == null || packageIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfStaskWorkOrderGreenhouse>lambdaQuery()
            .eq(SfStaskWorkOrderGreenhouse::getTenantId, tenantId)
            .in(SfStaskWorkOrderGreenhouse::getPackageId, packageIds)
            .orderByAsc(SfStaskWorkOrderGreenhouse::getPackageId)
            .orderByAsc(SfStaskWorkOrderGreenhouse::getItemId)
            .orderByAsc(SfStaskWorkOrderGreenhouse::getGreenhouseId));
    }

    /**
     * 批量查询拆分工单绑定的大棚明细。
     *
     * @param tenantId 租户编号
     * @param orderIds 拆分工单ID集合
     * @return 大棚明细
     */
    default List<SfStaskWorkOrderGreenhouse> selectByOrderIds(String tenantId, Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfStaskWorkOrderGreenhouse>lambdaQuery()
            .eq(SfStaskWorkOrderGreenhouse::getTenantId, tenantId)
            .in(SfStaskWorkOrderGreenhouse::getOrderId, orderIds)
            .orderByAsc(SfStaskWorkOrderGreenhouse::getOrderId)
            .orderByAsc(SfStaskWorkOrderGreenhouse::getGreenhouseId));
    }

    /**
     * 绑定任务包大棚明细到拆分工单。
     *
     * @param tenantId           租户编号
     * @param greenhouseItemIds  大棚明细ID集合
     * @param orderId            拆分工单ID
     * @return 更新行数
     */
    default int updateOrderIdByIds(String tenantId, Collection<Long> greenhouseItemIds, Long orderId) {
        if (greenhouseItemIds == null || greenhouseItemIds.isEmpty() || orderId == null) {
            return 0;
        }
        return update(null, Wrappers.<SfStaskWorkOrderGreenhouse>lambdaUpdate()
            .eq(SfStaskWorkOrderGreenhouse::getTenantId, tenantId)
            .in(SfStaskWorkOrderGreenhouse::getGreenhouseItemId, greenhouseItemIds)
            .set(SfStaskWorkOrderGreenhouse::getOrderId, orderId));
    }

    /**
     * 清空任务包下大棚明细的拆分工单绑定。
     *
     * @param tenantId  租户编号
     * @param packageId 任务包ID
     * @return 更新行数
     */
    default int clearOrderIdByPackageId(String tenantId, Long packageId) {
        if (packageId == null) {
            return 0;
        }
        return update(null, Wrappers.<SfStaskWorkOrderGreenhouse>lambdaUpdate()
            .eq(SfStaskWorkOrderGreenhouse::getTenantId, tenantId)
            .eq(SfStaskWorkOrderGreenhouse::getPackageId, packageId)
            .set(SfStaskWorkOrderGreenhouse::getOrderId, null));
    }

    /**
     * 物理删除任务包大棚明细。
     *
     * @param tenantId  租户编号
     * @param packageId 任务包ID
     * @return 删除行数
     */
    default int deleteByPackageId(String tenantId, Long packageId) {
        return delete(Wrappers.<SfStaskWorkOrderGreenhouse>lambdaQuery()
            .eq(SfStaskWorkOrderGreenhouse::getTenantId, tenantId)
            .eq(SfStaskWorkOrderGreenhouse::getPackageId, packageId));
    }

    /**
     * 查询未拆单任务包中与指定范围冲突的进行中任务。
     *
     * @param tenantId                 租户编号
     * @param planDate                 计划作业日期
     * @param scopes                   任务范围列表
     * @param excludePackageId         排除的任务包 ID（重新提交时排除自身）
     * @param blockingPackageStatuses  进行中的任务包状态
     * @return 冲突列表
     */
    default List<SfStaskTaskScopeConflictVo> selectActiveScopeConflictsForPackages(String tenantId, Date planDate,
        Collection<SfStaskTaskScopeBo> scopes, Long excludePackageId, Collection<String> blockingPackageStatuses) {
        if (tenantId == null || tenantId.isBlank() || planDate == null || scopes == null || scopes.isEmpty()
            || blockingPackageStatuses == null || blockingPackageStatuses.isEmpty()) {
            return List.of();
        }
        var wrapper = Wrappers.<SfStaskWorkOrderGreenhouse>lambdaQuery()
            .eq(SfStaskWorkOrderGreenhouse::getTenantId, tenantId);
        StaskTaskScopeSqlHelper.applyScopePairMatch(wrapper, scopes, "greenhouse_id", "work_item_id");
        List<Object> params = new ArrayList<>();
        params.add(tenantId);
        params.add(planDate);
        StringBuilder existsSql = new StringBuilder(
            "exists (select 1 from sf_stask_task_package p where p.package_id = sf_stask_work_order_greenhouse.package_id "
                + "and p.tenant_id = {0} and p.plan_date = {1} and p.status in ("
                + StaskTaskScopeSqlHelper.toSqlInLiterals(blockingPackageStatuses) + ")");
        if (excludePackageId != null) {
            existsSql.append(" and p.package_id <> {2}");
            params.add(excludePackageId);
        }
        existsSql.append(" and not exists (select 1 from sf_stask_work_order o where o.tenant_id = p.tenant_id "
            + "and o.package_id = p.package_id))");
        wrapper.apply(existsSql.toString(), params.toArray());
        return selectList(wrapper).stream().map(row -> {
            SfStaskTaskScopeConflictVo vo = new SfStaskTaskScopeConflictVo();
            vo.setGreenhouseId(row.getGreenhouseId());
            vo.setGreenhouseNameSnapshot(row.getGreenhouseNameSnapshot());
            vo.setWorkItemId(row.getWorkItemId());
            return vo;
        }).toList();
    }

    /**
     * 查询已拆分工单中与指定范围冲突的进行中任务。
     * 以工单大棚关联表为准，避免多棚工单只校验代表大棚。
     */
    default List<SfStaskTaskScopeConflictVo> selectActiveScopeConflictsForSplitOrders(String tenantId, Date planDate,
        Collection<SfStaskTaskScopeBo> scopes, Collection<String> blockingOrderStatuses) {
        if (tenantId == null || tenantId.isBlank() || planDate == null || scopes == null || scopes.isEmpty()
            || blockingOrderStatuses == null || blockingOrderStatuses.isEmpty()) {
            return List.of();
        }
        var wrapper = Wrappers.<SfStaskWorkOrderGreenhouse>lambdaQuery()
            .eq(SfStaskWorkOrderGreenhouse::getTenantId, tenantId)
            .isNotNull(SfStaskWorkOrderGreenhouse::getOrderId);
        StaskTaskScopeSqlHelper.applyScopePairMatch(wrapper, scopes, "greenhouse_id", "work_item_id");
        wrapper.apply("exists (select 1 from sf_stask_work_order o where o.order_id = "
                + "sf_stask_work_order_greenhouse.order_id and o.tenant_id = {0} and o.plan_date = {1} "
                + "and o.status in (" + StaskTaskScopeSqlHelper.toSqlInLiterals(blockingOrderStatuses) + "))",
            tenantId, planDate);
        return selectList(wrapper).stream().map(row -> {
            SfStaskTaskScopeConflictVo vo = new SfStaskTaskScopeConflictVo();
            vo.setGreenhouseId(row.getGreenhouseId());
            vo.setGreenhouseNameSnapshot(row.getGreenhouseNameSnapshot());
            vo.setWorkItemId(row.getWorkItemId());
            return vo;
        }).toList();
    }
}
