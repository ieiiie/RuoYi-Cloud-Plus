package com.ym.agriculture.farmtask.workorder.service.packagecmd;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.agriculture.shared.i18n.BilingualContent;
import com.ym.agriculture.shared.i18n.StaskBilingualMessageFormatter;
import com.ym.agriculture.farmtask.inventory.service.TaskMaterialLifecycleCoordinator;
import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskTaskPackageMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderItemMapper;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskRejectBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskTechConfirmBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskWorkItemCreateBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskWorkOrderCreateBo;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskCreatorRole;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderEvent;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskTaskPackage;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrderItem;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeAccessor;
import com.ym.agriculture.farmtask.workorder.support.SfStaskNotifyService;
import com.ym.agriculture.farmtask.workorder.support.SfStaskOrderNoGenerator;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 任务包创建、编辑和技术审核生命周期命令。
 *
 * <p>该组件由事务门面调用，自身不声明新事务。</p>
 */
@Component
public class SfStaskPackageLifecycleCommand {

    private final com.ym.agriculture.shared.i18n.StaskMessageResolver messages;
    private final SfStaskTaskPackageMapper taskPackageMapper;
    private final SfStaskWorkOrderItemMapper itemMapper;
    private final SfStaskOrderNoGenerator orderNoGenerator;
    private final SfStaskNotifyService notifyService;
    private final SfStaskPackageContentPlanner planner;
    private final SfStaskPackageContentWriter writer;
    private final SfStaskPackageSplitter splitter;
    private final SfStaskStateFlowSupport stateFlow;
    private final SfStaskPackageCommandGuard guard;
    private final StaskBilingualMessageFormatter bilingualMessageFormatter;

    private final SfStaskEmployeeAccessor employeeAccessor;

    @Autowired(required = false)
    private TaskMaterialLifecycleCoordinator taskMaterialCoordinator;

    /**
     * Spring 运行时使用的完整构造函数，确保姓名快照查询依赖由容器注入。
     */
    @Autowired
    public SfStaskPackageLifecycleCommand(
        com.ym.agriculture.shared.i18n.StaskMessageResolver messages,
        SfStaskTaskPackageMapper taskPackageMapper,
        SfStaskWorkOrderItemMapper itemMapper,
        SfStaskOrderNoGenerator orderNoGenerator,
        SfStaskNotifyService notifyService,
        SfStaskPackageContentPlanner planner,
        SfStaskPackageContentWriter writer,
        SfStaskPackageSplitter splitter,
        SfStaskStateFlowSupport stateFlow,
        SfStaskPackageCommandGuard guard,
        StaskBilingualMessageFormatter bilingualMessageFormatter,
        SfStaskEmployeeAccessor employeeAccessor) {
        this.messages = messages;
        this.taskPackageMapper = taskPackageMapper;
        this.itemMapper = itemMapper;
        this.orderNoGenerator = orderNoGenerator;
        this.notifyService = notifyService;
        this.planner = planner;
        this.writer = writer;
        this.splitter = splitter;
        this.stateFlow = stateFlow;
        this.guard = guard;
        this.bilingualMessageFormatter = bilingualMessageFormatter;
        this.employeeAccessor = employeeAccessor;
    }

    /**
     * 兼容旧的手工构造测试；生产 Bean 必须使用上面的完整构造函数。
     */
    public SfStaskPackageLifecycleCommand(
        com.ym.agriculture.shared.i18n.StaskMessageResolver messages,
        SfStaskTaskPackageMapper taskPackageMapper,
        SfStaskWorkOrderItemMapper itemMapper,
        SfStaskOrderNoGenerator orderNoGenerator,
        SfStaskNotifyService notifyService,
        SfStaskPackageContentPlanner planner,
        SfStaskPackageContentWriter writer,
        SfStaskPackageSplitter splitter,
        SfStaskStateFlowSupport stateFlow,
        SfStaskPackageCommandGuard guard,
        StaskBilingualMessageFormatter bilingualMessageFormatter) {
        this(messages, taskPackageMapper, itemMapper, orderNoGenerator, notifyService,
            planner, writer, splitter, stateFlow, guard, bilingualMessageFormatter, null);
    }

    /**
     * 创建任务包；技术员直接提交时同步拆分工单。
     *
     * @param bo              创建信息
     * @param creatorRoleCode 创建人业务角色
     * @return 任务包ID
     */
    public Long createPackage(SfStaskWorkOrderCreateBo bo, String creatorRoleCode) {
        String tenantId = guard.requireTenantId();
        requireWorkItems(bo);
        Long employeeId = LoginHelper.getUserId();
        Date now = new Date();
        Long packageId = IdWorker.getId();
        boolean draft = Boolean.TRUE.equals(bo.getDraft());
        List<SfStaskWorkItemCreateBo> workItems = bo.getWorkItems();
        planner.validateNoDuplicateRequestScopes(workItems);
        validateForSubmit(tenantId, bo, workItems, draft, null);

        SfStaskTaskPackage header = buildPackageHeader(
            bo, creatorRoleCode, tenantId, employeeId, packageId, draft, now);
        taskPackageMapper.insert(header);
        List<SfStaskPackageContentPlanner.NormalizedWorkItem> normalizedItems =
            planner.normalizeWorkItems(tenantId, workItems, !draft);
        List<SfStaskWorkOrderItem> items = writer.writePackageContent(
            tenantId, packageId, normalizedItems, now,
            SfStaskPackageContentWriter.PreservedTechInstructions.empty(),
            StaskCreatorRole.EXPERT.equals(creatorRoleCode));
        replaceTaskMaterials(tenantId, packageId, creatorRoleCode, normalizedItems, !draft);
        stateFlow.insertFlowLog(header, null, header.getStatus(),
            draft ? StaskOrderEvent.SAVE_DRAFT : submitEvent(creatorRoleCode), messages.chinese(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_PACKAGE_CREATED));
        if (!draft && StaskCreatorRole.EXPERT.equals(creatorRoleCode)) {
            splitAndSyncPendingReceipts(header, items, bo.getOverallTechNote(), now);
        }
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
    public Long savePackageDraft(Long packageId, SfStaskWorkOrderCreateBo bo, String creatorRoleCode) {
        return updatePackage(packageId, bo, creatorRoleCode, false);
    }

    /**
     * 提交任务包。
     *
     * @param packageId       任务包ID
     * @param bo              编辑信息
     * @param creatorRoleCode 当前创建角色
     * @return 任务包ID
     */
    public Long submitPackage(Long packageId, SfStaskWorkOrderCreateBo bo, String creatorRoleCode) {
        return updatePackage(packageId, bo, creatorRoleCode, true);
    }

    /**
     * 技术员确认生产管理员任务包并拆分工单。
     *
     * @param packageId 任务包ID或拆分工单ID
     * @param bo        技术说明
     * @return 新增拆分工单数
     */
    public int techConfirm(Long packageId, SfStaskTechConfirmBo bo) {
        if (bo == null) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_COMMON_REQUEST_REQUIRED);
        }
        guard.requireCurrentRole(EmployeeConstants.APP_ROLE_STASK_EXPERT, messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_PACKAGE_TECHNICIAN_CONFIRM_ONLY));
        SfStaskTaskPackage header = guard.requirePackageHeader(packageId);
        guard.ensurePendingManagerPackage(header);
        List<SfStaskWorkOrderItem> items = itemMapper.selectByPackageId(
            header.getTenantId(), header.getPackageId());
        if (CollUtil.isEmpty(items)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_WORK_ITEM_EMPTY);
        }
        planner.validatePackageLeaderAssignments(items);
        header.setOverallTechNote(bo.getOverallTechNote());
        header.setHandlerTechnicianEmployeeId(LoginHelper.getUserId());
        header.setHandlerTechnicianEmployeeNameSnapshot(currentEmployeeName());
        header.setUpdateBy(LoginHelper.getUserId());
        header.setUpdateTime(java.time.LocalDateTime.now());
        writer.applyTechInstructions(items, bo);
        stateFlow.changeStatus(header, StaskOrderEvent.TECH_CONFIRM, messages.chinese(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_PACKAGE_TECHNICIAN_CONFIRMED));
        return splitter.splitPackage(header, items, bo.getOverallTechNote(), new Date());
    }

    /**
     * 技术员退回生产管理员任务包。
     *
     * @param packageId 任务包ID或拆分工单ID
     * @param bo        退回原因
     * @return 固定返回1
     */
    public int techReject(Long packageId, SfStaskRejectBo bo) {
        guard.requireReason(bo, messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_PACKAGE_REJECT_REASON_PROMPT));
        guard.requireCurrentRole(EmployeeConstants.APP_ROLE_STASK_EXPERT, messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_PACKAGE_TECHNICIAN_REJECT_ONLY));
        SfStaskTaskPackage header = guard.requirePackageHeader(packageId);
        guard.ensurePendingManagerPackage(header);
        header.setHandlerTechnicianEmployeeId(null);
        header.setHandlerTechnicianEmployeeNameSnapshot(null);
        stateFlow.changeStatus(header, StaskOrderEvent.TECH_REJECT, bo.getReason());
        notifyService.notify("TECH_REJECT", header.getCreatorEmployeeId(),
            bilingual(StaskMessageKeys.NOTIFY_TECH_REJECTED, bo.getReason()));
        return 1;
    }

    private Long updatePackage(Long packageId, SfStaskWorkOrderCreateBo bo,
        String creatorRoleCode, boolean submit) {
        requireWorkItems(bo);
        SfStaskTaskPackage header = guard.requirePackageHeader(packageId);
        guard.ensurePackageCreator(header);
        guard.ensurePackageEditable(header, creatorRoleCode, submit);
        planner.validateNoDuplicateRequestScopes(bo.getWorkItems());
        validateForSubmit(header.getTenantId(), bo, bo.getWorkItems(), !submit, packageId);

        Date now = new Date();
        SfStaskPackageContentWriter.PreservedTechInstructions preservedTech =
            writer.buildPreservedTechInstructions(header.getTenantId(), packageId);
        updatePackageHeader(header, bo, creatorRoleCode, now);
        if (StaskCreatorRole.EXPERT.equals(creatorRoleCode)) {
            header.setHandlerTechnicianEmployeeNameSnapshot(currentEmployeeName());
        }
        stateFlow.updatePackageWithLock(header);
        writer.deletePackageContent(header.getTenantId(), packageId);
        List<SfStaskPackageContentPlanner.NormalizedWorkItem> normalizedItems =
            planner.normalizeWorkItems(header.getTenantId(), bo.getWorkItems(), submit);
        List<SfStaskWorkOrderItem> items = writer.writePackageContent(
            header.getTenantId(), packageId, normalizedItems, now,
            StaskCreatorRole.PRODUCTION_ADMIN.equals(creatorRoleCode)
                ? preservedTech : SfStaskPackageContentWriter.PreservedTechInstructions.empty(),
            StaskCreatorRole.EXPERT.equals(creatorRoleCode));
        replaceTaskMaterials(header.getTenantId(), packageId, creatorRoleCode, normalizedItems, submit);

        if (!submit) {
            stateFlow.insertFlowLog(header, header.getStatus(), header.getStatus(),
                StaskOrderEvent.SAVE_DRAFT, messages.chinese(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_PACKAGE_DRAFT_SAVED));
            return packageId;
        }
        stateFlow.changeStatus(header, submitEvent(creatorRoleCode),
            StaskOrderStatus.TECH_REJECTED.equals(header.getStatus()) ? messages.chinese(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_PACKAGE_RESUBMITTED) : messages.chinese(com.ym.agriculture.shared.i18n.StaskMessageKeys.MESSAGE_PACKAGE_SUBMITTED));
        if (StaskCreatorRole.EXPERT.equals(creatorRoleCode)) {
            splitAndSyncPendingReceipts(header, items, bo.getOverallTechNote(), now);
        }
        return packageId;
    }

    private int splitAndSyncPendingReceipts(SfStaskTaskPackage header, List<SfStaskWorkOrderItem> items,
        String overallTechNote, Date now) {
        int rows = splitter.splitPackage(header, items, overallTechNote, now);
        if (taskMaterialCoordinator != null) {
            taskMaterialCoordinator.syncPendingReceiptsForPackage(header.getTenantId(), header.getPackageId());
        }
        return rows;
    }

    private void replaceTaskMaterials(String tenantId, Long packageId, String creatorRoleCode,
        List<SfStaskPackageContentPlanner.NormalizedWorkItem> normalizedItems, boolean submit) {
        // 兼容只构造旧命令依赖的单元测试；Spring 运行时该协调器始终存在。
        if (taskMaterialCoordinator != null) {
            taskMaterialCoordinator.replacePackageMaterials(
                tenantId, packageId, creatorRoleCode, normalizedItems, submit);
        }
    }

    private BilingualContent bilingual(String key, Object... args) {
        return bilingualMessageFormatter.format(key, args);
    }

    private void validateForSubmit(String tenantId, SfStaskWorkOrderCreateBo bo,
        List<SfStaskWorkItemCreateBo> workItems, boolean skip, Long excludePackageId) {
        if (skip) {
            return;
        }
        planner.ensureNoDuplicateActiveTaskScope(tenantId, bo.getPlanDate(), workItems, excludePackageId);
        planner.validateWorkItemLeaderAssignments(tenantId, workItems);
    }

    private SfStaskTaskPackage buildPackageHeader(SfStaskWorkOrderCreateBo bo, String creatorRoleCode,
        String tenantId, Long employeeId, Long packageId, boolean draft, Date now) {
        SfStaskTaskPackage header = new SfStaskTaskPackage();
        header.setPackageId(packageId);
        header.setPackageNo(orderNoGenerator.next(now));
        header.setTenantId(tenantId);
        header.setCreatorEmployeeId(employeeId);
        header.setCreatorRoleCode(creatorRoleCode);
        if (StaskCreatorRole.EXPERT.equals(creatorRoleCode)) {
            header.setHandlerTechnicianEmployeeId(employeeId);
            header.setHandlerTechnicianEmployeeNameSnapshot(currentEmployeeName());
        }
        header.setPlanDate(bo.getPlanDate());
        header.setOverallTechNote(StaskCreatorRole.EXPERT.equals(creatorRoleCode)
            ? bo.getOverallTechNote() : null);
        header.setStatus(draft ? StaskOrderStatus.DRAFT : initialStatus(creatorRoleCode));
        header.setVersion(0);
        header.setCreateBy(employeeId);
        header.setCreateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now));
        header.setUpdateBy(employeeId);
        header.setUpdateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now));
        return header;
    }

    private void updatePackageHeader(SfStaskTaskPackage header, SfStaskWorkOrderCreateBo bo,
        String creatorRoleCode, Date now) {
        header.setPlanDate(bo.getPlanDate());
        if (StaskCreatorRole.EXPERT.equals(creatorRoleCode)) {
            header.setOverallTechNote(bo.getOverallTechNote());
            header.setHandlerTechnicianEmployeeId(LoginHelper.getUserId());
            header.setHandlerTechnicianEmployeeNameSnapshot(currentEmployeeName());
        } else {
            header.setHandlerTechnicianEmployeeId(null);
            header.setHandlerTechnicianEmployeeNameSnapshot(null);
        }
        header.setUpdateBy(LoginHelper.getUserId());
        header.setUpdateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now));
    }

    private void requireWorkItems(SfStaskWorkOrderCreateBo bo) {
        if (bo == null || CollUtil.isEmpty(bo.getWorkItems())) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_PACKAGE_WORK_ITEM_REQUIRED);
        }
    }

    private String currentEmployeeName() {
        Long employeeId = LoginHelper.getUserId();
        if (employeeAccessor == null || employeeId == null) {
            return null;
        }
        List<SysEmployeeVo> employees = employeeAccessor.queryBasicByIds(List.of(employeeId));
        if (CollUtil.isEmpty(employees)) {
            employees = employeeAccessor.queryByIds(List.of(employeeId));
        }
        return CollUtil.isEmpty(employees) ? null : employees.get(0).getName();
    }

    private static String initialStatus(String creatorRoleCode) {
        return StaskCreatorRole.EXPERT.equals(creatorRoleCode)
            ? StaskOrderStatus.PENDING_LEADER_ACCEPT : StaskOrderStatus.PENDING_TECH_CONFIRM;
    }

    private static String submitEvent(String creatorRoleCode) {
        return StaskCreatorRole.EXPERT.equals(creatorRoleCode)
            ? StaskOrderEvent.SUBMIT_BY_TECHNICIAN : StaskOrderEvent.SUBMIT_BY_MANAGER;
    }
}
