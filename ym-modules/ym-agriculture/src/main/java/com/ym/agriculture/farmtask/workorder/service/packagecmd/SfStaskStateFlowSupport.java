package com.ym.agriculture.farmtask.workorder.service.packagecmd;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.agriculture.shared.i18n.StaskErrorCodes;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskFlowLogMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskTaskPackageMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderMapper;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskFlowLog;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskTaskPackage;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.workorder.support.SfStaskOrderStateMachine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 任务包和拆分工单的状态流转支持组件。
 *
 * <p>统一保持“状态机校验、乐观锁更新、流转日志”顺序，不自行开启事务。</p>
 */
@RequiredArgsConstructor
@Component
public class SfStaskStateFlowSupport {

    private final com.ym.agriculture.shared.i18n.StaskMessageResolver messages;
    private final SfStaskWorkOrderMapper workOrderMapper;
    private final SfStaskTaskPackageMapper taskPackageMapper;
    private final SfStaskFlowLogMapper flowLogMapper;
    private final SfStaskOrderStateMachine stateMachine;
    private final SfStaskPackageCommandGuard guard;

    /**
     * 流转拆分工单状态并写入日志。
     *
     * @param order  工单
     * @param event  状态事件
     * @param remark 业务备注
     */
    public void changeStatus(SfStaskWorkOrder order, String event, String remark) {
        String oldStatus = order.getStatus();
        String newStatus = stateMachine.transit(oldStatus, event);
        order.setStatus(newStatus);
        ensureOptimisticUpdated(workOrderMapper.updateById(order));
        insertFlowLog(order, oldStatus, newStatus, event, remark);
    }

    /**
     * 流转任务包状态并写入日志。
     *
     * @param taskPackage 任务包头
     * @param event       状态事件
     * @param remark      业务备注
     */
    public void changeStatus(SfStaskTaskPackage taskPackage, String event, String remark) {
        String oldStatus = taskPackage.getStatus();
        String newStatus = stateMachine.transit(oldStatus, event);
        taskPackage.setStatus(newStatus);
        updatePackageWithLock(taskPackage);
        insertFlowLog(taskPackage, oldStatus, newStatus, event, remark);
    }

    /**
     * 使用乐观锁更新任务包头。
     *
     * @param taskPackage 任务包头
     * @return 更新行数
     */
    public int updatePackageWithLock(SfStaskTaskPackage taskPackage) {
        return ensureOptimisticUpdated(taskPackageMapper.updateById(taskPackage));
    }

    /**
     * 写入任务包流转日志。
     *
     * @param taskPackage 任务包头
     * @param fromStatus  原状态
     * @param toStatus    新状态
     * @param event       业务事件
     * @param remark      业务备注
     */
    public void insertFlowLog(SfStaskTaskPackage taskPackage, String fromStatus, String toStatus,
        String event, String remark) {
        SfStaskFlowLog log = new SfStaskFlowLog();
        log.setLogId(IdWorker.getId());
        log.setTenantId(taskPackage.getTenantId());
        log.setOrderId(taskPackage.getPackageId());
        log.setPackageId(taskPackage.getPackageId());
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setEvent(event);
        log.setOperatorEmployeeId(LoginHelper.getUserId());
        log.setOperatorRoleCode(guard.currentRoleCode());
        log.setRemark(remark);
        log.setCreateTime(new Date());
        flowLogMapper.insert(log);
    }

    /**
     * 构造拆分工单流转日志，不写入数据库。
     *
     * @param order            工单
     * @param fromStatus       原状态
     * @param toStatus         新状态
     * @param event            业务事件
     * @param remark           业务备注
     * @param operatorRoleCode 操作角色码
     * @return 流转日志
     */
    public SfStaskFlowLog buildFlowLog(SfStaskWorkOrder order, String fromStatus, String toStatus,
        String event, String remark, String operatorRoleCode) {
        SfStaskFlowLog log = new SfStaskFlowLog();
        log.setLogId(IdWorker.getId());
        log.setTenantId(order.getTenantId());
        log.setOrderId(order.getOrderId());
        log.setPackageId(order.getPackageId());
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setEvent(event);
        log.setOperatorEmployeeId(LoginHelper.getUserId());
        log.setOperatorRoleCode(operatorRoleCode);
        log.setRemark(remark);
        log.setCreateTime(new Date());
        return log;
    }

    /**
     * 仅在内存中执行一次工单状态流转并构造日志，供批量更新使用。
     *
     * @param order            工单
     * @param event            业务事件
     * @param remark           业务备注
     * @param operatorRoleCode 操作角色码
     * @return 流转日志
     */
    public SfStaskFlowLog prepareTransition(SfStaskWorkOrder order, String event, String remark,
        String operatorRoleCode) {
        String oldStatus = order.getStatus();
        String newStatus = stateMachine.transit(oldStatus, event);
        order.setStatus(newStatus);
        return buildFlowLog(order, oldStatus, newStatus, event, remark, operatorRoleCode);
    }

    private void insertFlowLog(SfStaskWorkOrder order, String fromStatus, String toStatus,
        String event, String remark) {
        flowLogMapper.insert(buildFlowLog(
            order, fromStatus, toStatus, event, remark, guard.currentRoleCode()));
    }

    private int ensureOptimisticUpdated(int rows) {
        if (rows <= 0) {
            throw messages.stableException(StaskErrorCodes.TASK_STATUS_CHANGED,
                com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_COMMON_CONCURRENT_UPDATE);
        }
        return rows;
    }
}
