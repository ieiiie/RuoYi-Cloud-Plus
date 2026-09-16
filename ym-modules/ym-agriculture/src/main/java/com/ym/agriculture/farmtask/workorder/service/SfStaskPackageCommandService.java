package com.ym.agriculture.farmtask.workorder.service;

import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farmtask.i18n.StaskI18nResourceRegistrar;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskRejectBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskTechConfirmBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskWorkOrderCreateBo;
import com.ym.agriculture.farmtask.workorder.service.packagecmd.SfStaskPackageLifecycleCommand;
import com.ym.agriculture.farmtask.workorder.service.packagecmd.SfStaskPackageTerminationCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * stask 任务包命令事务门面。
 *
 * <p>该门面保留原有命令入口和事务边界，具体业务由任务包生命周期与终止组件完成。</p>
 */
@RequiredArgsConstructor
@Service
public class SfStaskPackageCommandService {

    private final SfStaskPackageLifecycleCommand lifecycle;
    private final SfStaskPackageTerminationCommand termination;
    @Autowired
    private StaskI18nResourceRegistrar i18nResourceRegistrar;

    /**
     * 创建任务包；技术员直接提交时同步拆分工单。
     *
     * @param bo              创建信息
     * @param creatorRoleCode 创建人业务角色
     * @return 任务包ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createPackage(SfStaskWorkOrderCreateBo bo, String creatorRoleCode) {
        Long packageId = lifecycle.createPackage(bo, creatorRoleCode);
        register(packageId);
        return packageId;
    }

    /**
     * 保存任务包草稿。
     *
     * @param packageId       任务包ID
     * @param bo              编辑信息
     * @param creatorRoleCode 当前创建角色
     * @return 任务包ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long savePackageDraft(Long packageId, SfStaskWorkOrderCreateBo bo, String creatorRoleCode) {
        Long savedPackageId = lifecycle.savePackageDraft(packageId, bo, creatorRoleCode);
        register(savedPackageId);
        return savedPackageId;
    }

    /**
     * 提交任务包。
     *
     * @param packageId       任务包ID
     * @param bo              编辑信息
     * @param creatorRoleCode 当前创建角色
     * @return 任务包ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long submitPackage(Long packageId, SfStaskWorkOrderCreateBo bo, String creatorRoleCode) {
        Long submittedPackageId = lifecycle.submitPackage(packageId, bo, creatorRoleCode);
        register(submittedPackageId);
        return submittedPackageId;
    }

    /**
     * 技术员确认生产管理员任务包并拆分工单。
     *
     * @param packageId 任务包ID或拆分工单ID
     * @param bo        技术说明
     * @return 新增拆分工单数
     */
    @Transactional(rollbackFor = Exception.class)
    public int techConfirm(Long packageId, SfStaskTechConfirmBo bo) {
        int rows = lifecycle.techConfirm(packageId, bo);
        register(packageId);
        return rows;
    }

    /**
     * 技术员退回生产管理员任务包。
     *
     * @param packageId 任务包ID或拆分工单ID
     * @param bo        退回原因
     * @return 固定返回1
     */
    @Transactional(rollbackFor = Exception.class)
    public int techReject(Long packageId, SfStaskRejectBo bo) {
        int rows = lifecycle.techReject(packageId, bo);
        register(packageId);
        return rows;
    }

    /**
     * 撤销并作废任务包。
     *
     * @param packageId 任务包ID
     * @param bo        撤销原因
     * @return 作废记录数
     */
    @Transactional(rollbackFor = Exception.class)
    public int cancelPackage(Long packageId, SfStaskRejectBo bo) {
        int rows = termination.cancelPackage(packageId, bo);
        register(packageId);
        return rows;
    }

    /**
     * 物理删除未拆单的草稿或技术退回任务包。
     *
     * @param packageId 任务包ID
     * @return 删除行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int deletePackage(Long packageId) {
        return termination.deletePackage(packageId);
    }

    /**
     * 创建人将待技术确认任务包撤回草稿。
     *
     * @param packageId 任务包ID
     * @param bo        撤回原因
     * @return 固定返回1
     */
    @Transactional(rollbackFor = Exception.class)
    public int withdrawPackage(Long packageId, SfStaskRejectBo bo) {
        int rows = termination.withdrawPackage(packageId, bo);
        register(packageId);
        return rows;
    }

    /**
     * 技术员撤回尚未被组长接单的任务包。
     *
     * @param packageId 任务包ID或拆分工单ID
     * @param bo        撤回原因
     * @return 固定返回1
     */
    @Transactional(rollbackFor = Exception.class)
    public int technicianWithdrawPackage(Long packageId, SfStaskRejectBo bo) {
        int rows = termination.technicianWithdrawPackage(packageId, bo);
        register(packageId);
        return rows;
    }

    /**
     * 创建人作废单条拆分工单。
     *
     * @param orderId 工单ID
     * @param bo      作废原因
     * @return 固定返回1
     */
    @Transactional(rollbackFor = Exception.class)
    public int voidOrder(Long orderId, SfStaskRejectBo bo) {
        int rows = termination.voidOrder(orderId, bo);
        register(orderId);
        return rows;
    }

    /**
     * 生产管理员按任务ID作废拆分工单或未拆分任务包。
     *
     * @param id 任务包ID或拆分工单ID
     * @param bo 作废原因
     * @return 作废记录数
     */
    @Transactional(rollbackFor = Exception.class)
    public int voidManagerTask(Long id, SfStaskRejectBo bo) {
        int rows = termination.voidManagerTask(id, bo);
        register(id);
        return rows;
    }

    /**
     * 作废任务包及其全部尚未执行的拆分工单。
     *
     * @param packageId 任务包ID
     * @param bo        作废原因
     * @return 作废记录数
     */
    @Transactional(rollbackFor = Exception.class)
    public int voidPackage(Long packageId, SfStaskRejectBo bo) {
        int rows = termination.voidPackage(packageId, bo);
        register(packageId);
        return rows;
    }

    private void register(Long packageOrOrderId) {
        if (i18nResourceRegistrar != null) {
            i18nResourceRegistrar.registerPackageOrOrder(TenantHelper.getTenantId(), packageOrOrderId);
        }
    }
}
