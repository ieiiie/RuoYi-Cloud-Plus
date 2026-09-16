package com.ym.agriculture.farmtask.workorder.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskTaskScopeBo;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskCreatorRole;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkOrderCategoryCountVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkOrderStatusCountVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkOrderWorkItemCountVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskTaskScopeConflictVo;
import com.ym.agriculture.farmtask.workorder.support.StaskTaskScopeSqlHelper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * stask 工单 Mapper。
 */
@Mapper
public interface SfStaskWorkOrderMapper extends BaseMapperPlus<SfStaskWorkOrder, SfStaskWorkOrder> {

    /**
     * 按分类名称快照统计有效运营工单排行。
     */
    @Select("""
        <script>
        SELECT COALESCE(NULLIF(TRIM(category_name_snapshot), ''), '未分类') AS categoryName,
               COUNT(*) AS orderCount
        FROM sf_stask_work_order
        WHERE tenant_id = #{tenantId}
          AND greenhouse_id IS NOT NULL
          AND status IN
          <foreach collection="statuses" item="status" open="(" separator="," close=")">
            #{status}
          </foreach>
        GROUP BY COALESCE(NULLIF(TRIM(category_name_snapshot), ''), '未分类')
        ORDER BY orderCount DESC, categoryName ASC
        LIMIT #{limit}
        </script>
        """)
    List<SfStaskWorkOrderCategoryCountVo> selectCategoryRanking(
        @Param("tenantId") String tenantId,
        @Param("statuses") Collection<String> statuses,
        @Param("limit") int limit);

    /**
     * 按具体农事项目名称快照统计有效运营工单排行。
     */
    @Select("""
        <script>
        SELECT COALESCE(NULLIF(TRIM(work_item_name_snapshot), ''), '未命名农事') AS workItemName,
               COUNT(*) AS orderCount
        FROM sf_stask_work_order
        WHERE tenant_id = #{tenantId}
          AND greenhouse_id IS NOT NULL
          AND status IN
          <foreach collection="statuses" item="status" open="(" separator="," close=")">
            #{status}
          </foreach>
        GROUP BY COALESCE(NULLIF(TRIM(work_item_name_snapshot), ''), '未命名农事')
        ORDER BY orderCount DESC, workItemName ASC
        LIMIT #{limit}
        </script>
        """)
    List<SfStaskWorkOrderWorkItemCountVo> selectWorkItemRanking(
        @Param("tenantId") String tenantId,
        @Param("statuses") Collection<String> statuses,
        @Param("limit") int limit);

    /**
     * 按状态汇总指定计划日期范围内的拆分工单数量。
     */
    @Select("""
        <script>
        SELECT status,
               COUNT(*) AS orderCount
        FROM sf_stask_work_order
        WHERE tenant_id = #{tenantId}
          AND greenhouse_id IS NOT NULL
          AND plan_date <![CDATA[>=]]> #{startTime}
          AND plan_date <![CDATA[<]]> #{endTime}
          AND status IN
          <foreach collection="statuses" item="status" open="(" separator="," close=")">
            #{status}
          </foreach>
        GROUP BY status
        </script>
        """)
    List<SfStaskWorkOrderStatusCountVo> selectStatusCountsByPlanDate(
        @Param("tenantId") String tenantId,
        @Param("startTime") Date startTime,
        @Param("endTime") Date endTime,
        @Param("statuses") Collection<String> statuses);

    /**
     * 汇总某日计划拆分工单的组长填写工人数，不含组长本人。
     */
    @Select("""
        <script>
        SELECT COALESCE(SUM(required_worker_count), 0)
        FROM sf_stask_work_order
        WHERE tenant_id = #{tenantId}
          AND greenhouse_id IS NOT NULL
          AND required_worker_count IS NOT NULL
          AND plan_date <![CDATA[>=]]> #{startTime}
          AND plan_date <![CDATA[<]]> #{endTime}
          AND status IN
          <foreach collection="statuses" item="status" open="(" separator="," close=")">
            #{status}
          </foreach>
        </script>
        """)
    BigDecimal sumRequiredWorkerCountByPlanDate(@Param("tenantId") String tenantId,
                                                @Param("startTime") Date startTime,
                                                @Param("endTime") Date endTime,
                                                @Param("statuses") Collection<String> statuses);

    /**
     * 汇总全部历史有效拆分工单的组长填写工人数，不含组长本人。
     */
    @Select("""
        <script>
        SELECT COALESCE(SUM(required_worker_count), 0)
        FROM sf_stask_work_order
        WHERE tenant_id = #{tenantId}
          AND greenhouse_id IS NOT NULL
          AND required_worker_count IS NOT NULL
          AND status IN
          <foreach collection="statuses" item="status" open="(" separator="," close=")">
            #{status}
          </foreach>
        </script>
        """)
    BigDecimal sumAllRequiredWorkerCount(
        @Param("tenantId") String tenantId,
        @Param("statuses") Collection<String> statuses);

    /**
     * 查询任务包下的工单。
     *
     * @param tenantId  租户编号
     * @param packageId 任务包ID
     * @return 工单列表
     */
    default List<SfStaskWorkOrder> selectByPackageId(String tenantId, Long packageId) {
        return selectList(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .eq(SfStaskWorkOrder::getPackageId, packageId)
            .orderByAsc(SfStaskWorkOrder::getOrderId));
    }

    /**
     * 批量查询任务包下的工单，供回填等批处理避免逐任务包查询。
     */
    default List<SfStaskWorkOrder> selectByPackageIds(String tenantId, Collection<Long> packageIds) {
        if (packageIds == null || packageIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .in(SfStaskWorkOrder::getPackageId, packageIds)
            .orderByAsc(SfStaskWorkOrder::getPackageId)
            .orderByAsc(SfStaskWorkOrder::getOrderId));
    }

    /**
     * 批量查询已存在拆分工单的任务包 ID。
     *
     * @param tenantId   租户编号
     * @param packageIds 任务包 ID 集合
     * @return 已有拆单的 packageId 集合
     */
    default Set<Long> selectPackageIdsHavingSplits(String tenantId, Collection<Long> packageIds) {
        if (tenantId == null || tenantId.isBlank() || packageIds == null || packageIds.isEmpty()) {
            return Set.of();
        }
        return selectList(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .select(SfStaskWorkOrder::getPackageId)
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .in(SfStaskWorkOrder::getPackageId, packageIds)
            .isNotNull(SfStaskWorkOrder::getPackageId))
            .stream()
            .map(SfStaskWorkOrder::getPackageId)
            .collect(Collectors.toSet());
    }

    /**
     * 查询租户内多个工单。
     *
     * @param tenantId 租户编号
     * @param orderIds 工单ID集合
     * @return 工单列表
     */
    default List<SfStaskWorkOrder> selectByIds(String tenantId, Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .in(SfStaskWorkOrder::getOrderId, orderIds)
            .orderByDesc(SfStaskWorkOrder::getCreateTime));
    }

    /**
     * 按工单 ID 升序锁定租户内工单，用于批量状态变更。
     */
    default List<SfStaskWorkOrder> selectByIdsForUpdate(String tenantId, Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .in(SfStaskWorkOrder::getOrderId, orderIds)
            .orderByAsc(SfStaskWorkOrder::getOrderId)
            .last("FOR UPDATE"));
    }

    /**
     * 查询员工关联的指定状态工单。
     *
     * @param tenantId   租户编号
     * @param employeeId 员工ID
     * @param statuses   状态集合
     * @return 工单列表
     */
    default List<SfStaskWorkOrder> selectByEmployeeAndStatuses(String tenantId, Long employeeId,
        Collection<String> statuses) {
        return selectList(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .and(w -> w.eq(SfStaskWorkOrder::getCreatorEmployeeId, employeeId)
                .or().eq(SfStaskWorkOrder::getLeaderId, employeeId))
            .in(statuses != null && !statuses.isEmpty(), SfStaskWorkOrder::getStatus, statuses)
            .orderByDesc(SfStaskWorkOrder::getCreateTime));
    }

    /**
     * 查询技术员本人发起或最终经手的指定状态工单。
     *
     * <p>与旧版通用员工查询分开，避免组长兼容入口意外获得技术员经手任务。</p>
     */
    default List<SfStaskWorkOrder> selectTechnicianByEmployeeAndStatuses(String tenantId, Long employeeId,
        Collection<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .and(w -> w.eq(SfStaskWorkOrder::getCreatorEmployeeId, employeeId)
                .or().eq(SfStaskWorkOrder::getHandlerTechnicianEmployeeId, employeeId))
            .in(statuses != null && !statuses.isEmpty(), SfStaskWorkOrder::getStatus, statuses)
            .orderByDesc(SfStaskWorkOrder::getCreateTime));
    }

    /**
     * 统计创建人指定状态的工单数量。
     *
     * @param tenantId   租户编号
     * @param employeeId 创建人员工 ID
     * @param statuses   状态集合
     * @param packageOnly 已废弃；工单表拆分后仅保存拆分工单
     * @return 数量
     */
    default long countByCreatorAndStatuses(String tenantId, Long employeeId, Collection<String> statuses,
        boolean packageOnly) {
        if (statuses == null || statuses.isEmpty()) {
            return 0L;
        }
        return selectCount(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .eq(SfStaskWorkOrder::getCreatorEmployeeId, employeeId)
            .in(SfStaskWorkOrder::getStatus, statuses));
    }

    /**
     * 查询创建人的拆分工单列表。
     *
     * @param tenantId   租户编号
     * @param employeeId 创建人员工 ID
     * @param statuses   状态集合
     * @return 拆分工单列表
     */
    default List<SfStaskWorkOrder> selectSplitOrdersByCreatorAndStatuses(String tenantId, Long employeeId,
        Collection<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .eq(SfStaskWorkOrder::getCreatorEmployeeId, employeeId)
            .in(SfStaskWorkOrder::getStatus, statuses)
            .orderByAsc(SfStaskWorkOrder::getPlanDate)
            .orderByDesc(SfStaskWorkOrder::getCreateTime));
    }

    /**
     * 统计租户内指定状态的拆分工单数量。
     *
     * @param tenantId 租户编号
     * @param statuses 状态集合
     * @return 数量
     */
    default long countByStatuses(String tenantId, Collection<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return 0L;
        }
        return selectCount(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .in(SfStaskWorkOrder::getStatus, statuses));
    }

    /**
     * 查询租户内指定状态的拆分工单列表。
     *
     * @param tenantId 租户编号
     * @param statuses 状态集合
     * @return 拆分工单列表
     */
    default List<SfStaskWorkOrder> selectSplitOrdersByStatuses(String tenantId, Collection<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .in(SfStaskWorkOrder::getStatus, statuses)
            .orderByAsc(SfStaskWorkOrder::getPlanDate)
            .orderByDesc(SfStaskWorkOrder::getCreateTime));
    }

    /**
     * 查询大屏某个大棚验收通过的农事任务。
     *
     * <p>日期使用计划作业日期；结束时间为开区间，避免一天边界的时间精度问题。</p>
     *
     * @param tenantId     租户编号
     * @param greenhouseId 大棚 ID
     * @param planStart    计划日期开始（含），为空表示不限制
     * @param planEnd      计划日期结束（不含），为空表示不限制
     * @return 验收通过的拆分工单
     */
    default List<SfStaskWorkOrder> selectScreenAcceptedFarmOrders(String tenantId, Long greenhouseId,
        Date planStart, Date planEnd) {
        return selectList(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .eq(SfStaskWorkOrder::getGreenhouseId, greenhouseId)
            .eq(SfStaskWorkOrder::getStatus, StaskOrderStatus.ACCEPTANCE_PASSED)
            .isNotNull(SfStaskWorkOrder::getPlanDate)
            .ge(planStart != null, SfStaskWorkOrder::getPlanDate, planStart)
            .lt(planEnd != null, SfStaskWorkOrder::getPlanDate, planEnd)
            .orderByAsc(SfStaskWorkOrder::getPlanDate)
            .orderByAsc(SfStaskWorkOrder::getOrderId));
    }

    /**
     * 查询租户内全部大棚验收通过的农事任务，供大屏批量统计农事档案照片。
     *
     * @param tenantId      租户编号
     * @param greenhouseIds 大棚 ID 集合
     * @return 验收通过的拆分工单
     */
    default List<SfStaskWorkOrder> selectScreenAcceptedFarmOrders(String tenantId,
        Collection<Long> greenhouseIds) {
        if (greenhouseIds == null || greenhouseIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .in(SfStaskWorkOrder::getGreenhouseId, greenhouseIds)
            .eq(SfStaskWorkOrder::getStatus, StaskOrderStatus.ACCEPTANCE_PASSED)
            .isNotNull(SfStaskWorkOrder::getPlanDate)
            .orderByAsc(SfStaskWorkOrder::getGreenhouseId)
            .orderByAsc(SfStaskWorkOrder::getPlanDate)
            .orderByAsc(SfStaskWorkOrder::getOrderId));
    }

    /**
     * 构建全部任务页拆分工单基础查询条件。
     *
     * @param tenantId      租户编号
     * @param statuses      状态集合
     * @param greenhouseIds 大棚 ID 集合
     * @param workItemIds   农事项目 ID 集合
     * @param planStartDate 计划开始日期
     * @param planEndDate   计划结束日期
     * @return 查询条件
     */
    default LambdaQueryWrapper<SfStaskWorkOrder> buildAllTaskSplitWrapper(String tenantId, Collection<String> statuses,
        Collection<Long> greenhouseIds, Collection<Long> workItemIds, Date planStartDate, Date planEndDate) {
        return Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .in(statuses != null && !statuses.isEmpty(), SfStaskWorkOrder::getStatus, statuses)
            .in(greenhouseIds != null && !greenhouseIds.isEmpty(), SfStaskWorkOrder::getGreenhouseId, greenhouseIds)
            .in(workItemIds != null && !workItemIds.isEmpty(), SfStaskWorkOrder::getWorkItemId, workItemIds)
            .ge(planStartDate != null, SfStaskWorkOrder::getPlanDate, planStartDate)
            .le(planEndDate != null, SfStaskWorkOrder::getPlanDate, planEndDate)
            .orderByAsc(SfStaskWorkOrder::getPlanDate)
            .orderByDesc(SfStaskWorkOrder::getCreateTime);
    }

    /**
     * 查询全部任务页拆分工单列表。
     *
     * @param tenantId      租户编号
     * @param statuses      状态集合
     * @param greenhouseIds 大棚 ID 集合
     * @param workItemIds   农事项目 ID 集合
     * @param planStartDate 计划开始日期
     * @param planEndDate   计划结束日期
     * @return 拆分工单列表
     */
    default List<SfStaskWorkOrder> selectAllTaskSplits(String tenantId, Collection<String> statuses,
        Collection<Long> greenhouseIds, Collection<Long> workItemIds, Date planStartDate, Date planEndDate) {
        return selectAllTaskSplits(tenantId, statuses, greenhouseIds, workItemIds, planStartDate, planEndDate, null);
    }

    /**
     * 查询全部任务拆分工单，并按发起人筛选。
     */
    default List<SfStaskWorkOrder> selectAllTaskSplits(String tenantId, Collection<String> statuses,
        Collection<Long> greenhouseIds, Collection<Long> workItemIds, Date planStartDate, Date planEndDate,
        Long creatorEmployeeId) {
        LambdaQueryWrapper<SfStaskWorkOrder> wrapper = buildAllTaskSplitWrapper(tenantId, statuses, greenhouseIds,
            workItemIds, planStartDate, planEndDate);
        wrapper.eq(creatorEmployeeId != null, SfStaskWorkOrder::getCreatorEmployeeId, creatorEmployeeId);
        return selectList(wrapper);
    }

    /**
     * 查询生产管理员全部任务页可见拆分工单（租户内全量）。
     *
     * @param tenantId      租户编号
     * @param statuses      状态集合
     * @param greenhouseIds 大棚 ID 集合
     * @param workItemIds   农事项目 ID 集合
     * @param planStartDate 计划开始日期
     * @param planEndDate   计划结束日期
     * @return 拆分工单列表
     */
    default List<SfStaskWorkOrder> selectManagerAllTaskSplits(String tenantId, Collection<String> statuses,
        Collection<Long> greenhouseIds, Collection<Long> workItemIds, Date planStartDate, Date planEndDate) {
        return selectManagerAllTaskSplits(tenantId, statuses, greenhouseIds, workItemIds,
            planStartDate, planEndDate, null);
    }

    /**
     * 查询生产管理员全部任务拆分工单，并按发起人筛选。
     */
    default List<SfStaskWorkOrder> selectManagerAllTaskSplits(String tenantId, Collection<String> statuses,
        Collection<Long> greenhouseIds, Collection<Long> workItemIds, Date planStartDate, Date planEndDate,
        Long creatorEmployeeId) {
        return selectAllTaskSplits(tenantId, statuses, greenhouseIds, workItemIds,
            planStartDate, planEndDate, creatorEmployeeId);
    }

    /**
     * 查询技术员全部任务页可见拆分工单（租户内全量）。
     *
     * @param tenantId      租户编号
     * @param employeeId    当前员工 ID（保留参数，兼容调用方）
     * @param statuses      状态集合
     * @param greenhouseIds 大棚 ID 集合
     * @param workItemIds   农事项目 ID 集合
     * @param planStartDate 计划开始日期
     * @param planEndDate   计划结束日期
     * @return 拆分工单列表
     */
    default List<SfStaskWorkOrder> selectExpertAllTaskSplits(String tenantId, Long employeeId,
        Collection<String> statuses, Collection<Long> greenhouseIds, Collection<Long> workItemIds,
        Date planStartDate, Date planEndDate) {
        return selectExpertAllTaskSplits(tenantId, employeeId, statuses, greenhouseIds, workItemIds,
            planStartDate, planEndDate, true);
    }

    /**
     * 查询技术员全部任务页拆分工单。
     *
     * @param relatedOnly 是否仅查询本人发起或本人经手
     */
    default List<SfStaskWorkOrder> selectExpertAllTaskSplits(String tenantId, Long employeeId,
        Collection<String> statuses, Collection<Long> greenhouseIds, Collection<Long> workItemIds,
        Date planStartDate, Date planEndDate, boolean relatedOnly) {
        return selectExpertAllTaskSplits(tenantId, employeeId, statuses, greenhouseIds, workItemIds,
            planStartDate, planEndDate, relatedOnly, null);
    }

    /**
     * 查询技术员全部任务拆分工单，并按发起人筛选。
     */
    default List<SfStaskWorkOrder> selectExpertAllTaskSplits(String tenantId, Long employeeId,
        Collection<String> statuses, Collection<Long> greenhouseIds, Collection<Long> workItemIds,
        Date planStartDate, Date planEndDate, boolean relatedOnly, Long creatorEmployeeId) {
        LambdaQueryWrapper<SfStaskWorkOrder> wrapper = buildAllTaskSplitWrapper(
            tenantId, statuses, greenhouseIds, workItemIds, planStartDate, planEndDate);
        wrapper.eq(creatorEmployeeId != null, SfStaskWorkOrder::getCreatorEmployeeId, creatorEmployeeId);
        if (relatedOnly) {
            wrapper.and(w -> w.eq(SfStaskWorkOrder::getCreatorEmployeeId, employeeId)
                .or().eq(SfStaskWorkOrder::getHandlerTechnicianEmployeeId, employeeId));
        }
        return selectList(wrapper);
    }

    /**
     * 查询技术员工作台可见拆分工单（租户内全量）。
     *
     * @param tenantId   租户编号
     * @param employeeId 当前技术员员工 ID（保留参数，兼容调用方）
     * @param statuses   状态集合
     * @return 拆分工单列表
     */
    default List<SfStaskWorkOrder> selectExpertSplitOrdersByStatuses(String tenantId, Long employeeId,
        Collection<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .and(w -> w.eq(SfStaskWorkOrder::getCreatorEmployeeId, employeeId)
                .or().eq(SfStaskWorkOrder::getHandlerTechnicianEmployeeId, employeeId))
            .in(SfStaskWorkOrder::getStatus, statuses)
            .orderByAsc(SfStaskWorkOrder::getPlanDate)
            .orderByDesc(SfStaskWorkOrder::getCreateTime));
    }

    /**
     * 统计技术员工作台可见拆分工单数量（租户内全量）。
     *
     * @param tenantId   租户编号
     * @param employeeId 当前技术员员工 ID（保留参数，兼容调用方）
     * @param statuses   状态集合
     * @return 数量
     */
    default long countExpertSplitOrdersByStatuses(String tenantId, Long employeeId, Collection<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return 0L;
        }
        return selectCount(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .and(w -> w.eq(SfStaskWorkOrder::getCreatorEmployeeId, employeeId)
                .or().eq(SfStaskWorkOrder::getHandlerTechnicianEmployeeId, employeeId))
            .in(SfStaskWorkOrder::getStatus, statuses));
    }

    /**
     * 查询技术员工作台可见、当日已验收的拆分工单（租户内全量）。
     *
     * @param tenantId   租户编号
     * @param employeeId 当前技术员员工 ID（保留参数，兼容调用方）
     * @param statuses   终态状态集合
     * @param startTime  当日开始（含）
     * @param endTime    当日结束（不含）
     * @return 拆分工单列表
     */
    default List<SfStaskWorkOrder> selectSplitOrdersAcceptedToday(String tenantId, Collection<String> statuses,
        Date startTime, Date endTime) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .in(SfStaskWorkOrder::getStatus, statuses)
            .apply("exists (select 1 from sf_stask_acceptance a where a.tenant_id = {0} "
                    + "and a.order_id = sf_stask_work_order.order_id "
                    + "and a.accepted_at >= {1} and a.accepted_at < {2})",
                tenantId, startTime, endTime)
            .orderByDesc(SfStaskWorkOrder::getUpdateTime));
    }

    /**
     * 查询租户内当日已验收的拆分工单。
     *
     * @param tenantId  租户编号
     * @param statuses  终态状态集合
     * @param startTime 当日开始（含）
     * @param endTime   当日结束（不含）
     * @return 拆分工单列表
     */
    default List<SfStaskWorkOrder> selectExpertSplitOrdersAcceptedToday(String tenantId, Long employeeId,
        Collection<String> statuses, Date startTime, Date endTime) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .and(w -> w.eq(SfStaskWorkOrder::getCreatorEmployeeId, employeeId)
                .or().eq(SfStaskWorkOrder::getHandlerTechnicianEmployeeId, employeeId))
            .in(SfStaskWorkOrder::getStatus, statuses)
            .apply("exists (select 1 from sf_stask_acceptance a where a.tenant_id = {0} "
                    + "and a.order_id = sf_stask_work_order.order_id "
                    + "and a.accepted_at >= {1} and a.accepted_at < {2})",
                tenantId, startTime, endTime)
            .orderByDesc(SfStaskWorkOrder::getUpdateTime));
    }

    /**
     * 统计技术员工作台可见、当日已验收的拆分工单数量（租户内全量）。
     *
     * @param tenantId   租户编号
     * @param employeeId 当前技术员员工 ID（保留参数，兼容调用方）
     * @param statuses   终态状态集合
     * @param startTime  当日开始（含）
     * @param endTime    当日结束（不含）
     * @return 数量
     */
    default long countSplitOrdersAcceptedToday(String tenantId, Collection<String> statuses, Date startTime,
        Date endTime) {
        if (statuses == null || statuses.isEmpty()) {
            return 0L;
        }
        return selectCount(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .in(SfStaskWorkOrder::getStatus, statuses)
            .apply("exists (select 1 from sf_stask_acceptance a where a.tenant_id = {0} "
                    + "and a.order_id = sf_stask_work_order.order_id "
                    + "and a.accepted_at >= {1} and a.accepted_at < {2})",
                tenantId, startTime, endTime));
    }

    /**
     * 统计租户内当日已验收的拆分工单数量。
     *
     * @param tenantId  租户编号
     * @param statuses  终态状态集合
     * @param startTime 当日开始（含）
     * @param endTime   当日结束（不含）
     * @return 数量
     */
    default long countExpertSplitOrdersAcceptedToday(String tenantId, Long employeeId, Collection<String> statuses,
        Date startTime, Date endTime) {
        if (statuses == null || statuses.isEmpty()) {
            return 0L;
        }
        return selectCount(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .and(w -> w.eq(SfStaskWorkOrder::getCreatorEmployeeId, employeeId)
                .or().eq(SfStaskWorkOrder::getHandlerTechnicianEmployeeId, employeeId))
            .in(SfStaskWorkOrder::getStatus, statuses)
            .apply("exists (select 1 from sf_stask_acceptance a where a.tenant_id = {0} "
                    + "and a.order_id = sf_stask_work_order.order_id "
                    + "and a.accepted_at >= {1} and a.accepted_at < {2})",
                tenantId, startTime, endTime));
    }

    /**
     * 查询组长全部任务页拆分工单列表。
     *
     * @param tenantId      租户编号
     * @param leaderId      组长员工 ID
     * @param statuses      状态集合
     * @param greenhouseIds 大棚 ID 集合
     * @param workItemIds   农事项目 ID 集合
     * @param planStartDate 计划开始日期
     * @param planEndDate   计划结束日期
     * @return 拆分工单列表
     */
    default List<SfStaskWorkOrder> selectLeaderAllTaskSplits(String tenantId, Long leaderId,
        Collection<String> statuses, Collection<Long> greenhouseIds, Collection<Long> workItemIds,
        Date planStartDate, Date planEndDate) {
        LambdaQueryWrapper<SfStaskWorkOrder> wrapper = Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .eq(SfStaskWorkOrder::getLeaderId, leaderId)
            .in(statuses != null && !statuses.isEmpty(), SfStaskWorkOrder::getStatus, statuses)
            .in(greenhouseIds != null && !greenhouseIds.isEmpty(), SfStaskWorkOrder::getGreenhouseId, greenhouseIds)
            .in(workItemIds != null && !workItemIds.isEmpty(), SfStaskWorkOrder::getWorkItemId, workItemIds)
            .ge(planStartDate != null, SfStaskWorkOrder::getPlanDate, planStartDate)
            .le(planEndDate != null, SfStaskWorkOrder::getPlanDate, planEndDate)
            .orderByDesc(SfStaskWorkOrder::getPlanDate)
            .orderByDesc(SfStaskWorkOrder::getCreateTime);
        return selectList(wrapper);
    }

    /**
     * 查询创建人当日已验收的拆分工单（验收通过或不通过）。
     *
     * @param tenantId   租户编号
     * @param employeeId 创建人员工 ID
     * @param statuses   终态状态集合
     * @param startTime  当日开始时间（含）
     * @param endTime    当日结束时间（不含）
     * @return 拆分工单列表
     */
    default List<SfStaskWorkOrder> selectSplitOrdersAcceptedTodayByCreator(String tenantId, Long employeeId,
        Collection<String> statuses, Date startTime, Date endTime) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .eq(SfStaskWorkOrder::getCreatorEmployeeId, employeeId)
            .in(SfStaskWorkOrder::getStatus, statuses)
            .apply("exists (select 1 from sf_stask_acceptance a where a.tenant_id = {0} "
                    + "and a.order_id = sf_stask_work_order.order_id "
                    + "and a.accepted_at >= {1} and a.accepted_at < {2})",
                tenantId, startTime, endTime)
            .orderByDesc(SfStaskWorkOrder::getUpdateTime));
    }

    /**
     * 统计创建人当日已验收的拆分工单数量。
     *
     * @param tenantId   租户编号
     * @param employeeId 创建人员工 ID
     * @param statuses   终态状态集合
     * @param startTime  当日开始时间（含）
     * @param endTime    当日结束时间（不含）
     * @return 数量
     */
    default long countSplitOrdersAcceptedTodayByCreator(String tenantId, Long employeeId, Collection<String> statuses,
        Date startTime, Date endTime) {
        if (statuses == null || statuses.isEmpty()) {
            return 0L;
        }
        return selectCount(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .eq(SfStaskWorkOrder::getCreatorEmployeeId, employeeId)
            .in(SfStaskWorkOrder::getStatus, statuses)
            .apply("exists (select 1 from sf_stask_acceptance a where a.tenant_id = {0} "
                    + "and a.order_id = sf_stask_work_order.order_id "
                    + "and a.accepted_at >= {1} and a.accepted_at < {2})",
                tenantId, startTime, endTime));
    }

    /**
     * 统计员工关联（创建人或组长）指定状态的工单数量。
     *
     * @param tenantId    租户编号
     * @param employeeId  员工 ID
     * @param statuses    状态集合
     * @param packageOnly 已废弃；工单表拆分后仅保存拆分工单
     * @return 数量
     */
    default long countByEmployeeAndStatuses(String tenantId, Long employeeId, Collection<String> statuses,
        boolean packageOnly) {
        if (statuses == null || statuses.isEmpty()) {
            return 0L;
        }
        return selectCount(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .and(w -> w.eq(SfStaskWorkOrder::getCreatorEmployeeId, employeeId)
                .or().eq(SfStaskWorkOrder::getLeaderId, employeeId))
            .in(SfStaskWorkOrder::getStatus, statuses));
    }

    /**
     * 查询员工关联（创建人或组长）的拆分工单列表。
     *
     * @param tenantId   租户编号
     * @param employeeId 员工 ID
     * @param statuses   状态集合
     * @return 拆分工单列表
     */
    default List<SfStaskWorkOrder> selectSplitOrdersByEmployeeAndStatuses(String tenantId, Long employeeId,
        Collection<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .and(w -> w.eq(SfStaskWorkOrder::getCreatorEmployeeId, employeeId)
                .or().eq(SfStaskWorkOrder::getLeaderId, employeeId))
            .in(SfStaskWorkOrder::getStatus, statuses)
            .orderByAsc(SfStaskWorkOrder::getPlanDate)
            .orderByDesc(SfStaskWorkOrder::getCreateTime));
    }

    /**
     * 查询员工关联当日已验收的拆分工单列表。
     *
     * @param tenantId   租户编号
     * @param employeeId 员工 ID
     * @param statuses   终态状态集合
     * @param startTime  当日开始（含）
     * @param endTime    当日结束（不含）
     * @return 拆分工单列表
     */
    default List<SfStaskWorkOrder> selectSplitOrdersAcceptedTodayByEmployee(String tenantId, Long employeeId,
        Collection<String> statuses, Date startTime, Date endTime) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .and(w -> w.eq(SfStaskWorkOrder::getCreatorEmployeeId, employeeId)
                .or().eq(SfStaskWorkOrder::getLeaderId, employeeId))
            .in(SfStaskWorkOrder::getStatus, statuses)
            .apply("exists (select 1 from sf_stask_acceptance a where a.tenant_id = {0} "
                    + "and a.order_id = sf_stask_work_order.order_id "
                    + "and a.accepted_at >= {1} and a.accepted_at < {2})",
                tenantId, startTime, endTime)
            .orderByDesc(SfStaskWorkOrder::getUpdateTime));
    }

    /**
     * 统计员工关联当日已验收的拆分工单数量。
     *
     * @param tenantId   租户编号
     * @param employeeId 员工 ID
     * @param statuses   终态状态集合
     * @param startTime  当日开始（含）
     * @param endTime    当日结束（不含）
     * @return 数量
     */
    default long countSplitOrdersAcceptedTodayByEmployee(String tenantId, Long employeeId, Collection<String> statuses,
        Date startTime, Date endTime) {
        if (statuses == null || statuses.isEmpty()) {
            return 0L;
        }
        return selectCount(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .and(w -> w.eq(SfStaskWorkOrder::getCreatorEmployeeId, employeeId)
                .or().eq(SfStaskWorkOrder::getLeaderId, employeeId))
            .in(SfStaskWorkOrder::getStatus, statuses)
            .apply("exists (select 1 from sf_stask_acceptance a where a.tenant_id = {0} "
                    + "and a.order_id = sf_stask_work_order.order_id "
                    + "and a.accepted_at >= {1} and a.accepted_at < {2})",
                tenantId, startTime, endTime));
    }

    /**
     * 统计组长负责的拆分工单数量。
     *
     * @param tenantId 租户编号
     * @param leaderId 组长员工 ID
     * @param statuses 状态集合
     * @return 数量
     */
    default long countByLeaderAndStatuses(String tenantId, Long leaderId, Collection<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return 0L;
        }
        return selectCount(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .eq(SfStaskWorkOrder::getLeaderId, leaderId)
            .in(SfStaskWorkOrder::getStatus, statuses));
    }

    /**
     * 查询组长负责的拆分工单列表。
     *
     * @param tenantId 租户编号
     * @param leaderId 组长员工 ID
     * @param statuses 状态集合
     * @return 拆分工单列表
     */
    default List<SfStaskWorkOrder> selectSplitOrdersByLeaderAndStatuses(String tenantId, Long leaderId,
        Collection<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .eq(SfStaskWorkOrder::getLeaderId, leaderId)
            .in(SfStaskWorkOrder::getStatus, statuses)
            .orderByAsc(SfStaskWorkOrder::getPlanDate)
            .orderByDesc(SfStaskWorkOrder::getCreateTime));
    }

    /**
     * 查询组长负责且当日验收通过的拆分工单。
     *
     * @param tenantId  租户编号
     * @param leaderId  组长员工 ID
     * @param statuses  终态状态集合
     * @param startTime 当日开始（含）
     * @param endTime   次日开始（不含）
     * @return 拆分工单列表
     */
    default List<SfStaskWorkOrder> selectLeaderSplitOrdersAcceptedToday(String tenantId, Long leaderId,
        Collection<String> statuses, Date startTime, Date endTime) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .eq(SfStaskWorkOrder::getLeaderId, leaderId)
            .in(SfStaskWorkOrder::getStatus, statuses)
            .apply("exists (select 1 from sf_stask_acceptance a where a.tenant_id = {0} "
                    + "and a.order_id = sf_stask_work_order.order_id "
                    + "and a.accepted_at >= {1} and a.accepted_at < {2})",
                tenantId, startTime, endTime)
            .orderByDesc(SfStaskWorkOrder::getUpdateTime));
    }

    /**
     * 统计组长负责且当日验收通过的拆分工单数量。
     *
     * @param tenantId  租户编号
     * @param leaderId  组长员工 ID
     * @param statuses  终态状态集合
     * @param startTime 当日开始（含）
     * @param endTime   次日开始（不含）
     * @return 数量
     */
    default long countLeaderSplitOrdersAcceptedToday(String tenantId, Long leaderId, Collection<String> statuses,
        Date startTime, Date endTime) {
        if (statuses == null || statuses.isEmpty()) {
            return 0L;
        }
        return selectCount(Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .eq(SfStaskWorkOrder::getLeaderId, leaderId)
            .in(SfStaskWorkOrder::getStatus, statuses)
            .apply("exists (select 1 from sf_stask_acceptance a where a.tenant_id = {0} "
                    + "and a.order_id = sf_stask_work_order.order_id "
                    + "and a.accepted_at >= {1} and a.accepted_at < {2})",
                tenantId, startTime, endTime));
    }

    /**
     * 查询拆分工单中与指定范围冲突的进行中任务。
     *
     * @param tenantId               租户编号
     * @param planDate               计划作业日期
     * @param scopes                 任务范围列表
     * @param blockingOrderStatuses  进行中的拆分工单状态
     * @return 冲突列表
     */
    default List<SfStaskTaskScopeConflictVo> selectActiveScopeConflictsForOrders(String tenantId, Date planDate,
        Collection<SfStaskTaskScopeBo> scopes, Collection<String> blockingOrderStatuses) {
        if (tenantId == null || tenantId.isBlank() || planDate == null || scopes == null || scopes.isEmpty()
            || blockingOrderStatuses == null || blockingOrderStatuses.isEmpty()) {
            return List.of();
        }
        var wrapper = Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .eq(SfStaskWorkOrder::getPlanDate, planDate)
            .in(SfStaskWorkOrder::getStatus, blockingOrderStatuses);
        StaskTaskScopeSqlHelper.applyScopePairMatch(wrapper, scopes, "greenhouse_id", "work_item_id");
        return selectList(wrapper).stream().map(order -> {
            SfStaskTaskScopeConflictVo vo = new SfStaskTaskScopeConflictVo();
            vo.setGreenhouseId(order.getGreenhouseId());
            vo.setGreenhouseNameSnapshot(order.getGreenhouseNameSnapshot());
            vo.setWorkItemId(order.getWorkItemId());
            vo.setWorkItemNameSnapshot(order.getWorkItemNameSnapshot());
            return vo;
        }).toList();
    }
}
