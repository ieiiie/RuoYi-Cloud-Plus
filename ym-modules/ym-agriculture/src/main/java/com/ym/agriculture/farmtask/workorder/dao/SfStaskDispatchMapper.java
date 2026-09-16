package com.ym.agriculture.farmtask.workorder.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskDispatchStatus;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskDispatch;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * stask 派工明细 Mapper。
 */
@Mapper
public interface SfStaskDispatchMapper extends BaseMapperPlus<SfStaskDispatch, SfStaskDispatch> {

    /**
     * 查询工单派工明细。
     *
     * @param tenantId 租户编号
     * @param orderId  工单ID
     * @return 派工明细
     */
    default List<SfStaskDispatch> selectByOrderId(String tenantId, Long orderId) {
        return selectList(Wrappers.<SfStaskDispatch>lambdaQuery()
            .eq(SfStaskDispatch::getTenantId, tenantId)
            .eq(SfStaskDispatch::getOrderId, orderId)
            .orderByDesc(SfStaskDispatch::getInvitedAt));
    }

    /**
     * 批量查询多个工单的派工明细，供回填等批处理避免逐工单查询。
     */
    default List<SfStaskDispatch> selectByOrderIds(String tenantId, Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfStaskDispatch>lambdaQuery()
            .eq(SfStaskDispatch::getTenantId, tenantId)
            .in(SfStaskDispatch::getOrderId, orderIds)
            .orderByAsc(SfStaskDispatch::getOrderId)
            .orderByDesc(SfStaskDispatch::getInvitedAt));
    }

    /**
     * 查询工单已接受派工明细。
     *
     * @param tenantId 租户编号
     * @param orderId  工单ID
     * @return 已接受派工列表
     */
    default List<SfStaskDispatch> selectAcceptedByOrderId(String tenantId, Long orderId) {
        return selectList(Wrappers.<SfStaskDispatch>lambdaQuery()
            .eq(SfStaskDispatch::getTenantId, tenantId)
            .eq(SfStaskDispatch::getOrderId, orderId)
            .eq(SfStaskDispatch::getStatus, StaskDispatchStatus.ACCEPTED)
            .orderByDesc(SfStaskDispatch::getRespondedAt)
            .orderByDesc(SfStaskDispatch::getInvitedAt));
    }

    /**
     * 查询工单指定派工明细。
     *
     * @param tenantId   租户编号
     * @param orderId    工单ID
     * @param dispatchId 派工明细ID
     * @return 派工明细
     */
    default SfStaskDispatch selectByOrderAndDispatchId(String tenantId, Long orderId, Long dispatchId) {
        return selectOne(Wrappers.<SfStaskDispatch>lambdaQuery()
            .eq(SfStaskDispatch::getTenantId, tenantId)
            .eq(SfStaskDispatch::getOrderId, orderId)
            .eq(SfStaskDispatch::getDispatchId, dispatchId)
            .last("LIMIT 1"));
    }

    /**
     * 查询工人首页派工记录（待确认 + 已接受）。
     *
     * @param tenantId 租户编号
     * @param workerId 工人员工ID
     * @return 派工记录列表
     */
    default List<SfStaskDispatch> selectHomeByWorkerId(String tenantId, Long workerId) {
        return selectList(Wrappers.<SfStaskDispatch>lambdaQuery()
            .eq(SfStaskDispatch::getTenantId, tenantId)
            .eq(SfStaskDispatch::getWorkerId, workerId)
            .in(SfStaskDispatch::getStatus, StaskDispatchStatus.PENDING, StaskDispatchStatus.ACCEPTED)
            .orderByDesc(SfStaskDispatch::getInvitedAt));
    }

    /**
     * 查询工人本人的派工记录。
     *
     * @param tenantId 租户编号
     * @param workerId 工人员工ID
     * @return 派工记录列表
     * @deprecated 请使用 {@link #selectHomeByWorkerId(String, Long)}
     */
    @Deprecated
    default List<SfStaskDispatch> selectByWorkerId(String tenantId, Long workerId) {
        return selectHomeByWorkerId(tenantId, workerId);
    }

    /**
     * 查询工人已接受派工记录（历史任务候选）。
     *
     * @param tenantId 租户编号
     * @param workerId 工人员工ID
     * @return 已接受派工记录
     */
    default List<SfStaskDispatch> selectAcceptedByWorkerId(String tenantId, Long workerId) {
        return selectList(Wrappers.<SfStaskDispatch>lambdaQuery()
            .eq(SfStaskDispatch::getTenantId, tenantId)
            .eq(SfStaskDispatch::getWorkerId, workerId)
            .eq(SfStaskDispatch::getStatus, StaskDispatchStatus.ACCEPTED)
            .orderByDesc(SfStaskDispatch::getRespondedAt));
    }

    /**
     * 批量查询多个工人的已接受派工记录，用于批量装配历史完工统计。
     *
     * @param tenantId 租户编号
     * @param workerIds 工人员工 ID 集合
     * @return 已接受派工记录
     */
    default List<SfStaskDispatch> selectAcceptedByWorkerIds(String tenantId, Collection<Long> workerIds) {
        if (workerIds == null || workerIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfStaskDispatch>lambdaQuery()
            .eq(SfStaskDispatch::getTenantId, tenantId)
            .in(SfStaskDispatch::getWorkerId, workerIds)
            .eq(SfStaskDispatch::getStatus, StaskDispatchStatus.ACCEPTED)
            .orderByAsc(SfStaskDispatch::getWorkerId)
            .orderByDesc(SfStaskDispatch::getRespondedAt));
    }

    /**
     * 查询工人全部派工记录。
     *
     * @param tenantId 租户编号
     * @param workerId 工人员工 ID
     * @return 派工记录列表
     */
    default List<SfStaskDispatch> selectAllByWorkerId(String tenantId, Long workerId) {
        return selectList(Wrappers.<SfStaskDispatch>lambdaQuery()
            .eq(SfStaskDispatch::getTenantId, tenantId)
            .eq(SfStaskDispatch::getWorkerId, workerId)
            .orderByDesc(SfStaskDispatch::getInvitedAt));
    }

    /**
     * 批量查询存在未完成任务的工人ID。
     *
     * @param tenantId  租户编号
     * @param workerIds 工人员工ID集合
     * @return 存在未完成任务的工人ID列表
     */
    default List<Long> selectUnfinishedWorkerIds(String tenantId, Collection<Long> workerIds) {
        if (workerIds == null || workerIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfStaskDispatch>lambdaQuery()
            .select(SfStaskDispatch::getWorkerId)
            .eq(SfStaskDispatch::getTenantId, tenantId)
            .in(SfStaskDispatch::getWorkerId, workerIds)
            .and(w -> w.eq(SfStaskDispatch::getStatus, StaskDispatchStatus.PENDING)
                .apply("exists (select 1 from sf_stask_work_order o where o.tenant_id = sf_stask_dispatch.tenant_id "
                    + "and o.order_id = sf_stask_dispatch.order_id and o.status not in ('VOIDED','CANCELLED'))")
                .or(or -> or.eq(SfStaskDispatch::getStatus, StaskDispatchStatus.ACCEPTED)
                    .apply("exists (select 1 from sf_stask_work_order o where o.tenant_id = sf_stask_dispatch.tenant_id "
                        + "and o.order_id = sf_stask_dispatch.order_id "
                        + "and o.status not in ('ACCEPTANCE_PASSED','ACCEPTANCE_REJECTED','VOIDED','CANCELLED'))"))))
            .stream()
            .map(SfStaskDispatch::getWorkerId)
            .distinct()
            .toList();
    }

    /**
     * 查询存在历史拒绝/撤销记录的工单 ID（用于再邀请标记）。
     *
     * @param tenantId 租户编号
     * @param workerId 工人员工ID
     * @param orderIds 工单 ID 集合
     * @return 存在历史记录的工单 ID
     */
    default List<Long> selectOrderIdsWithPriorInvite(String tenantId, Long workerId, Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfStaskDispatch>lambdaQuery()
            .select(SfStaskDispatch::getOrderId)
            .eq(SfStaskDispatch::getTenantId, tenantId)
            .eq(SfStaskDispatch::getWorkerId, workerId)
            .in(SfStaskDispatch::getOrderId, orderIds)
            .in(SfStaskDispatch::getStatus, StaskDispatchStatus.REJECTED, StaskDispatchStatus.CANCELLED))
            .stream()
            .map(SfStaskDispatch::getOrderId)
            .distinct()
            .toList();
    }

    /**
     * 是否存在同工单有效邀请。
     *
     * @param tenantId 租户编号
     * @param orderId  工单ID
     * @param workerId 工人员工ID
     * @return true 表示存在
     */
    default boolean existsActiveInvite(String tenantId, Long orderId, Long workerId) {
        return exists(Wrappers.<SfStaskDispatch>lambdaQuery()
            .eq(SfStaskDispatch::getTenantId, tenantId)
            .eq(SfStaskDispatch::getOrderId, orderId)
            .eq(SfStaskDispatch::getWorkerId, workerId)
            .in(SfStaskDispatch::getStatus, StaskDispatchStatus.PENDING, StaskDispatchStatus.ACCEPTED));
    }

    /**
     * 统计工单指定状态派工人数。
     *
     * @param tenantId 租户编号
     * @param orderId  工单ID
     * @param statuses 派工状态集合
     * @return 人数
     */
    default long countByStatuses(String tenantId, Long orderId, Collection<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return 0;
        }
        return selectCount(Wrappers.<SfStaskDispatch>lambdaQuery()
            .eq(SfStaskDispatch::getTenantId, tenantId)
            .eq(SfStaskDispatch::getOrderId, orderId)
            .in(SfStaskDispatch::getStatus, statuses));
    }

    /**
     * 统计工单已接受人数。
     *
     * @param tenantId 租户编号
     * @param orderId  工单ID
     * @return 已接受人数
     */
    default long countAccepted(String tenantId, Long orderId) {
        return selectCount(Wrappers.<SfStaskDispatch>lambdaQuery()
            .eq(SfStaskDispatch::getTenantId, tenantId)
            .eq(SfStaskDispatch::getOrderId, orderId)
            .eq(SfStaskDispatch::getStatus, StaskDispatchStatus.ACCEPTED));
    }

    /**
     * 统计工人验收通过任务数（按工单去重）。
     *
     * @param tenantId 租户编号
     * @param workerId 工人员工 ID
     * @return 验收通过任务数
     */
    default long countCompletedByWorkerId(String tenantId, Long workerId) {
        if (workerId == null) {
            return 0;
        }
        return selectCount(Wrappers.<SfStaskDispatch>lambdaQuery()
            .eq(SfStaskDispatch::getTenantId, tenantId)
            .eq(SfStaskDispatch::getWorkerId, workerId)
            .eq(SfStaskDispatch::getStatus, StaskDispatchStatus.ACCEPTED)
            .apply("exists (select 1 from sf_stask_work_order o where o.tenant_id = sf_stask_dispatch.tenant_id "
                + "and o.order_id = sf_stask_dispatch.order_id and o.status = {0})",
                StaskOrderStatus.ACCEPTANCE_PASSED));
    }

    /**
     * 查询待确认且尚未发送邀请短信的派工记录。
     *
     * @return 派工明细列表
     */
    default List<SfStaskDispatch> selectPendingInviteSmsDispatches() {
        return selectList(Wrappers.<SfStaskDispatch>lambdaQuery()
            .eq(SfStaskDispatch::getStatus, StaskDispatchStatus.PENDING)
            .isNull(SfStaskDispatch::getInviteSmsSentAt)
            .orderByAsc(SfStaskDispatch::getInvitedAt));
    }

    /**
     * 批量标记邀请短信已发送。
     *
     * @param tenantId    租户编号
     * @param dispatchIds 派工明细 ID 集合
     * @param sentAt      发送时间
     * @return 更新行数
     */
    default int markInviteSmsSent(String tenantId, Collection<Long> dispatchIds, Date sentAt) {
        if (dispatchIds == null || dispatchIds.isEmpty()) {
            return 0;
        }
        return update(null, Wrappers.<SfStaskDispatch>lambdaUpdate()
            .set(SfStaskDispatch::getInviteSmsSentAt, sentAt)
            .eq(SfStaskDispatch::getTenantId, tenantId)
            .in(SfStaskDispatch::getDispatchId, dispatchIds)
            .eq(SfStaskDispatch::getStatus, StaskDispatchStatus.PENDING)
            .isNull(SfStaskDispatch::getInviteSmsSentAt));
    }
}
