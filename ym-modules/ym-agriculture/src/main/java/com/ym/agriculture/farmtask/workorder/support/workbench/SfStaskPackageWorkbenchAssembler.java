package com.ym.agriculture.farmtask.workorder.support.workbench;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskCreatorRole;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderEvent;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskFlowLog;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskTaskPackage;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskHomeTaskCardVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskManagerWorkbenchItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskTechnicianWorkbenchItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkOrderVo;
import com.ym.agriculture.farmtask.workorder.support.SfStaskWorkOrderAssembler;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 纯内存装配任务包工作台卡片，不访问数据库或外部服务。
 */
@Component
@lombok.RequiredArgsConstructor
public class SfStaskPackageWorkbenchAssembler {

    private final com.ym.agriculture.shared.i18n.StaskMessageResolver messages;

    private static final String CARD_TYPE_PACKAGE = "PACKAGE";
    private static final String CARD_TYPE_SPLIT = "SPLIT";
    private static final String ACTION_EDIT = "EDIT";
    private static final String ACTION_PROCESS = "PROCESS";

    /**
     * 装配生产管理员待处理任务包卡片。
     */
    public List<SfStaskHomeTaskCardVo> managerPendingCards(List<SfStaskTaskPackage> packages,
        SfStaskPackageWorkbenchBatchReader.PackageData data, Map<Long, String> employeeNames,
        Map<String, String> roleNames) {
        if (CollUtil.isEmpty(packages)) {
            return List.of();
        }
        List<SfStaskHomeTaskCardVo> cards = new ArrayList<>(packages.size());
        for (SfStaskTaskPackage taskPackage : packages) {
            SfStaskHomeTaskCardVo card = baseCard(taskPackage, data, employeeNames, roleNames);
            if (StaskOrderStatus.DRAFT.equals(taskPackage.getStatus())) {
                card.setStatusLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_PACKAGE_STATUS_DRAFT));
                card.setPrimaryAction(ACTION_EDIT);
                card.setEventTime(com.ym.agriculture.shared.common.AgricultureTimes.toDate(taskPackage.getCreateTime()));
                card.setEventTimeLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_CREATED_TIME));
            } else if (StaskOrderStatus.TECH_REJECTED.equals(taskPackage.getStatus())) {
                card.setStatusLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_PACKAGE_STATUS_TECH_REJECTED));
                card.setPrimaryAction(ACTION_PROCESS);
                SfStaskFlowLog rejectLog = SfStaskWorkbenchTimeSupport.latestFlowLog(
                    data.flowLogs(), taskPackage.getPackageId(), StaskOrderEvent.TECH_REJECT);
                card.setEventTime(rejectLog == null
                    ? com.ym.agriculture.shared.common.AgricultureTimes.toDate(taskPackage.getUpdateTime())
                    : rejectLog.getCreateTime());
                card.setEventTimeLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_PACKAGE_REJECTED_TIME));
            }
            cards.add(card);
        }
        return cards;
    }

    /**
     * 装配生产管理员处理中任务包卡片。
     */
    public List<SfStaskHomeTaskCardVo> managerProcessingCards(List<SfStaskTaskPackage> packages,
        SfStaskPackageWorkbenchBatchReader.PackageData data, Map<Long, String> employeeNames,
        Map<String, String> roleNames) {
        if (CollUtil.isEmpty(packages)) {
            return List.of();
        }
        List<SfStaskHomeTaskCardVo> cards = new ArrayList<>(packages.size());
        for (SfStaskTaskPackage taskPackage : packages) {
            SfStaskHomeTaskCardVo card = baseCard(taskPackage, data, employeeNames, roleNames);
            card.setStatusLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_PACKAGE_STATUS_PENDING_TECHNICIAN));
            card.setEventTime(SfStaskWorkbenchTimeSupport.submitTime(data.flowLogs(), taskPackage.getPackageId()));
            card.setEventTimeLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_PACKAGE_SUBMITTED_TIME));
            cards.add(card);
        }
        return cards;
    }

    /**
     * 装配技术员待处理任务包卡片。
     */
    public List<SfStaskTechnicianWorkbenchItemVo> technicianPendingItems(List<SfStaskTaskPackage> packages,
        SfStaskPackageWorkbenchBatchReader.PackageData data, Map<Long, String> employeeNames,
        Map<String, String> roleNames) {
        return technicianPendingItems(packages, data, employeeNames, roleNames, null);
    }

    /**
     * 装配技术员待处理任务包卡片，并补充当前登录人相关性。
     */
    public List<SfStaskTechnicianWorkbenchItemVo> technicianPendingItems(List<SfStaskTaskPackage> packages,
        SfStaskPackageWorkbenchBatchReader.PackageData data, Map<Long, String> employeeNames,
        Map<String, String> roleNames, Long currentEmployeeId) {
        if (CollUtil.isEmpty(packages)) {
            return List.of();
        }
        List<SfStaskTechnicianWorkbenchItemVo> items = new ArrayList<>(packages.size());
        for (SfStaskTaskPackage taskPackage : packages) {
            SfStaskHomeTaskCardVo card = baseCard(taskPackage, data, employeeNames, roleNames);
            if (StaskOrderStatus.DRAFT.equals(taskPackage.getStatus())) {
                card.setStatusLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_PACKAGE_STATUS_DRAFT));
                card.setPrimaryAction(ACTION_EDIT);
                card.setEventTime(com.ym.agriculture.shared.common.AgricultureTimes.toDate(taskPackage.getCreateTime()));
                card.setEventTimeLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_WORKORDER_CREATED_TIME));
            } else if (StaskOrderStatus.PENDING_TECH_CONFIRM.equals(taskPackage.getStatus())) {
                card.setStatusLabel(StaskCreatorRole.PRODUCTION_ADMIN.equals(taskPackage.getCreatorRoleCode())
                    ? messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_PACKAGE_STATUS_PENDING_REVIEW) : messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_PACKAGE_STATUS_PENDING_TECHNICIAN));
                card.setEventTime(SfStaskWorkbenchTimeSupport.submitTime(
                    data.flowLogs(), taskPackage.getPackageId()));
                card.setEventTimeLabel(messages.message(com.ym.agriculture.shared.i18n.StaskMessageKeys.LABEL_PACKAGE_SUBMITTED_TIME));
            }
            SfStaskTechnicianWorkbenchItemVo item = new SfStaskTechnicianWorkbenchItemVo();
            BeanUtil.copyProperties(card, item);
            item.setCardType(CARD_TYPE_PACKAGE);
            item.setCreatorEmployeeId(taskPackage.getCreatorEmployeeId());
            item.setCreatorRoleCode(taskPackage.getCreatorRoleCode());
            item.setRelatedToCurrentUser(Objects.equals(taskPackage.getCreatorEmployeeId(), currentEmployeeId)
                || Objects.equals(taskPackage.getHandlerTechnicianEmployeeId(), currentEmployeeId));
            boolean reviewable = StaskOrderStatus.PENDING_TECH_CONFIRM.equals(taskPackage.getStatus())
                && StaskCreatorRole.PRODUCTION_ADMIN.equals(taskPackage.getCreatorRoleCode());
            item.setCanTechConfirm(reviewable);
            item.setCanTechReject(reviewable);
            item.setCanAcceptance(false);
            items.add(item);
        }
        SfStaskWorkOrderAssembler.sortTechnicianWorkbenchItems(items);
        return items;
    }

    /**
     * 合并任务包和拆分工单为生产管理员统一卡片并按最新业务时间排序。
     */
    public List<SfStaskManagerWorkbenchItemVo> mergeManagerItems(List<SfStaskHomeTaskCardVo> packageCards,
        List<SfStaskWorkOrderVo> splitVos) {
        List<SfStaskManagerWorkbenchItemVo> items = new ArrayList<>();
        for (SfStaskHomeTaskCardVo card : CollUtil.emptyIfNull(packageCards)) {
            SfStaskManagerWorkbenchItemVo item = new SfStaskManagerWorkbenchItemVo();
            BeanUtil.copyProperties(card, item);
            item.setCardType(CARD_TYPE_PACKAGE);
            item.setCanAcceptance(false);
            items.add(item);
        }
        for (SfStaskWorkOrderVo splitVo : CollUtil.emptyIfNull(splitVos)) {
            SfStaskManagerWorkbenchItemVo item = new SfStaskManagerWorkbenchItemVo();
            BeanUtil.copyProperties(splitVo, item);
            item.setCardType(CARD_TYPE_SPLIT);
            item.setCanAcceptance(StaskOrderStatus.PENDING_ACCEPTANCE.equals(splitVo.getStatus()));
            item.setTitle(SfStaskWorkOrderAssembler.splitTitle(
                splitVo.getGreenhouseNameSnapshot(), splitVo.getWorkItemNameSnapshot()));
            items.add(item);
        }
        SfStaskWorkOrderAssembler.sortManagerWorkbenchItems(items);
        return items;
    }

    private SfStaskHomeTaskCardVo baseCard(SfStaskTaskPackage taskPackage,
        SfStaskPackageWorkbenchBatchReader.PackageData data, Map<Long, String> employeeNames,
        Map<String, String> roleNames) {
        SfStaskHomeTaskCardVo card = new SfStaskHomeTaskCardVo();
        card.setCardType(CARD_TYPE_PACKAGE);
        card.setOrderId(taskPackage.getPackageId());
        card.setPackageId(taskPackage.getPackageId());
        card.setPlanDate(taskPackage.getPlanDate());
        card.setStatus(taskPackage.getStatus());
        card.setCreatorEmployeeId(taskPackage.getCreatorEmployeeId());
        card.setCreatorEmployeeName(employeeNames.get(taskPackage.getCreatorEmployeeId()));
        card.setCreatorRoleName(resolveRoleName(roleNames, taskPackage.getCreatorRoleCode()));
        card.setHandlerTechnicianEmployeeId(taskPackage.getHandlerTechnicianEmployeeId());
        card.setHandlerTechnicianEmployeeName(StringUtils.isNotBlank(
            taskPackage.getHandlerTechnicianEmployeeNameSnapshot())
            ? taskPackage.getHandlerTechnicianEmployeeNameSnapshot()
            : employeeNames.get(taskPackage.getHandlerTechnicianEmployeeId()));
        card.setGreenhouses(data.greenhouses().getOrDefault(taskPackage.getPackageId(), List.of()));
        fillItemSummary(data.itemSummaries().get(taskPackage.getPackageId()), card);
        return card;
    }

    private static void fillItemSummary(SfStaskPackageWorkbenchBatchReader.PackageItemSummary summary,
        SfStaskHomeTaskCardVo card) {
        SfStaskPackageWorkbenchBatchReader.PackageItemSummary actual = summary == null
            ? SfStaskPackageWorkbenchBatchReader.PackageItemSummary.empty() : summary;
        card.setTotalItemCount(actual.total());
        card.setItemSummaries(actual.previews());
        int more = actual.total() - actual.previews().size();
        if (more > 0) {
            card.setMoreItemCount(more);
        }
    }

    private String resolveRoleName(Map<String, String> roleNames, String roleCode) {
        return SfStaskWorkOrderAssembler.localizedRoleName(roleCode, roleNames.get(roleCode), messages);
    }
}
