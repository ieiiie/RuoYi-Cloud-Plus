package com.ym.agriculture.farmtask.i18n;

import com.ym.agriculture.shared.i18n.StaskI18nRegistrationResult;

import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.shared.i18n.model.I18nTextLookup;
import com.ym.agriculture.shared.i18n.model.I18nTextSource;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import com.ym.agriculture.shared.i18n.service.ISfI18nTextService;
import com.ym.agriculture.farmtask.assignment.model.entity.SfFarmWorkAssignment;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskAcceptanceMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskCompletionMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskDispatchMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskTaskPackageMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderGreenhouseMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderItemMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderMapper;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskAcceptance;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskCompletion;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskDispatch;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskTaskPackage;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrderGreenhouse;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * stask 业务快照的翻译资源登记器。
 *
 * <p>调用方必须在业务写入事务内调用本组件，使中文快照与对应的待翻译记录原子提交。
 * 流转日志 {@code remark} 可能是固定消息，不作为动态翻译源登记。</p>
 */
@Component
@RequiredArgsConstructor
public class StaskI18nResourceRegistrar {

    private static final Set<String> PACKAGE_RESOURCE_TYPES = Set.of(
        I18nResourceType.STASK_TASK_PACKAGE,
        I18nResourceType.STASK_WORK_ORDER_ITEM,
        I18nResourceType.STASK_WORK_ORDER_GREENHOUSE,
        I18nResourceType.STASK_WORK_ORDER,
        I18nResourceType.STASK_DISPATCH,
        I18nResourceType.STASK_COMPLETION,
        I18nResourceType.STASK_ACCEPTANCE
    );

    private final ISfI18nTextService i18nTextService;
    private final SfStaskTaskPackageMapper taskPackageMapper;
    private final SfStaskWorkOrderItemMapper workOrderItemMapper;
    private final SfStaskWorkOrderGreenhouseMapper greenhouseMapper;
    private final SfStaskWorkOrderMapper workOrderMapper;
    private final SfStaskDispatchMapper dispatchMapper;
    private final SfStaskCompletionMapper completionMapper;
    private final SfStaskAcceptanceMapper acceptanceMapper;

    /**
     * 登记任务包及其当前已生成的全部子资源。
     *
     * @param tenantId 租户编号
     * @param packageId 任务包编号
     */
    public void registerPackage(String tenantId, Long packageId) {
        if (isInvalid(tenantId, packageId)) {
            return;
        }
        SfStaskTaskPackage taskPackage = taskPackageMapper.selectById(packageId);
        if (taskPackage == null || !Objects.equals(tenantId, taskPackage.getTenantId())) {
            return;
        }
        List<SfStaskWorkOrder> orders = workOrderMapper.selectByPackageId(tenantId, packageId);
        List<I18nTextSource> sources = new ArrayList<>();
        appendPackage(sources, taskPackage);
        workOrderItemMapper.selectByPackageId(tenantId, packageId).forEach(row -> appendItem(sources, row));
        greenhouseMapper.selectByPackageId(tenantId, packageId).forEach(row -> appendGreenhouse(sources, row));
        orders.forEach(row -> appendOrderResources(sources, tenantId, row));
        register(tenantId, sources);
    }

    /**
     * 批量登记任务包及其子资源。关联表均按批读取，且可精确限定资源类型，供历史回填使用。
     *
     * @param tenantId 租户编号
     * @param taskPackages 已按租户读取的任务包
     * @param resourceTypes 本批需要登记的资源类型；空集合表示全部任务资源
     */
    public StaskI18nRegistrationResult registerPackages(String tenantId,
        Collection<SfStaskTaskPackage> taskPackages,
        Collection<String> resourceTypes) {
        if (tenantId == null || tenantId.isBlank() || taskPackages == null || taskPackages.isEmpty()) {
            return StaskI18nRegistrationResult.empty();
        }
        Set<String> selected = resourceTypes == null || resourceTypes.isEmpty()
            ? PACKAGE_RESOURCE_TYPES : Set.copyOf(resourceTypes);
        List<SfStaskTaskPackage> packages = taskPackages.stream()
            .filter(Objects::nonNull)
            .filter(row -> Objects.equals(tenantId, row.getTenantId()))
            .filter(row -> row.getPackageId() != null)
            .toList();
        if (packages.isEmpty()) {
            return StaskI18nRegistrationResult.empty();
        }
        List<Long> packageIds = packages.stream().map(SfStaskTaskPackage::getPackageId).distinct().toList();
        boolean needsOrders = selected.contains(I18nResourceType.STASK_WORK_ORDER)
            || selected.contains(I18nResourceType.STASK_DISPATCH)
            || selected.contains(I18nResourceType.STASK_COMPLETION)
            || selected.contains(I18nResourceType.STASK_ACCEPTANCE);
        List<SfStaskWorkOrder> orders = needsOrders
            ? workOrderMapper.selectByPackageIds(tenantId, packageIds) : List.of();
        List<Long> orderIds = orders.stream().map(SfStaskWorkOrder::getOrderId)
            .filter(Objects::nonNull).distinct().toList();
        List<I18nTextSource> sources = new ArrayList<>();
        int resourceCount = 0;
        if (selected.contains(I18nResourceType.STASK_TASK_PACKAGE)) {
            packages.forEach(row -> appendPackage(sources, row));
            resourceCount += packages.size();
        }
        if (selected.contains(I18nResourceType.STASK_WORK_ORDER_ITEM)) {
            List<SfStaskWorkOrderItem> rows = workOrderItemMapper.selectByPackageIds(tenantId, packageIds);
            rows.forEach(row -> appendItem(sources, row));
            resourceCount += rows.size();
        }
        if (selected.contains(I18nResourceType.STASK_WORK_ORDER_GREENHOUSE)) {
            List<SfStaskWorkOrderGreenhouse> rows = greenhouseMapper.selectByPackageIds(tenantId, packageIds);
            rows.forEach(row -> appendGreenhouse(sources, row));
            resourceCount += rows.size();
        }
        if (selected.contains(I18nResourceType.STASK_WORK_ORDER)) {
            orders.forEach(row -> appendOrder(sources, row));
            resourceCount += orders.size();
        }
        if (!orderIds.isEmpty() && selected.contains(I18nResourceType.STASK_DISPATCH)) {
            List<SfStaskDispatch> rows = dispatchMapper.selectByOrderIds(tenantId, orderIds);
            rows.forEach(row -> appendDispatch(sources, row));
            resourceCount += rows.size();
        }
        if (!orderIds.isEmpty() && selected.contains(I18nResourceType.STASK_COMPLETION)) {
            List<SfStaskCompletion> rows = completionMapper.selectByOrderIds(tenantId, orderIds);
            rows.forEach(row -> appendCompletion(sources, row));
            resourceCount += rows.size();
        }
        if (!orderIds.isEmpty() && selected.contains(I18nResourceType.STASK_ACCEPTANCE)) {
            List<SfStaskAcceptance> rows = acceptanceMapper.selectByOrderIds(tenantId, orderIds);
            rows.forEach(row -> appendAcceptance(sources, row));
            resourceCount += rows.size();
        }
        register(tenantId, sources);
        return new StaskI18nRegistrationResult(resourceCount, sources);
    }

    /**
     * 登记工单及其关联的派工和执行文本。
     *
     * @param tenantId 租户编号
     * @param orderId 工单编号
     */
    public void registerOrder(String tenantId, Long orderId) {
        if (isInvalid(tenantId, orderId)) {
            return;
        }
        SfStaskWorkOrder order = workOrderMapper.selectById(orderId);
        if (order == null || !Objects.equals(tenantId, order.getTenantId())) {
            return;
        }
        List<I18nTextSource> sources = new ArrayList<>();
        appendOrderResources(sources, tenantId, order);
        register(tenantId, sources);
    }

    /**
     * 兼容任务包和拆分工单共用的命令参数。
     *
     * @param tenantId 租户编号
     * @param packageOrOrderId 任务包或工单编号
     */
    public void registerPackageOrOrder(String tenantId, Long packageOrOrderId) {
        if (isInvalid(tenantId, packageOrOrderId)) {
            return;
        }
        SfStaskTaskPackage taskPackage = taskPackageMapper.selectById(packageOrOrderId);
        if (taskPackage != null && Objects.equals(tenantId, taskPackage.getTenantId())) {
            registerPackage(tenantId, packageOrOrderId);
            return;
        }
        registerOrder(tenantId, packageOrOrderId);
    }

    /**
     * 登记农事分配快照。
     *
     * @param tenantId 租户编号
     * @param assignments 新建或更新的农事分配
     */
    public List<I18nTextSource> registerAssignments(String tenantId, Collection<SfFarmWorkAssignment> assignments) {
        if (tenantId == null || assignments == null || assignments.isEmpty()) {
            return List.of();
        }
        List<I18nTextSource> sources = new ArrayList<>();
        assignments.stream()
            .filter(row -> Objects.equals(tenantId, row.getTenantId()))
            .forEach(row -> {
                add(sources, I18nResourceType.STASK_ASSIGNMENT, row.getAssignmentId(),
                    "workItemNameSnapshot", row.getWorkItemNameSnapshot());
                add(sources, I18nResourceType.STASK_ASSIGNMENT, row.getAssignmentId(),
                    "categoryNameSnapshot", row.getCategoryNameSnapshot());
            });
        register(tenantId, sources);
        return List.copyOf(sources);
    }

    private void appendOrderResources(List<I18nTextSource> sources, String tenantId, SfStaskWorkOrder order) {
        appendOrder(sources, order);
        greenhouseMapper.selectByOrderIds(tenantId, List.of(order.getOrderId()))
            .forEach(row -> appendGreenhouse(sources, row));
        dispatchMapper.selectByOrderId(tenantId, order.getOrderId())
            .forEach(row -> appendDispatch(sources, row));
        completionMapper.selectByOrderIds(tenantId, List.of(order.getOrderId()))
            .forEach(row -> appendCompletion(sources, row));
        acceptanceMapper.selectByOrderIds(tenantId, List.of(order.getOrderId()))
            .forEach(row -> appendAcceptance(sources, row));
    }

    private void appendPackage(List<I18nTextSource> sources, SfStaskTaskPackage row) {
        add(sources, I18nResourceType.STASK_TASK_PACKAGE, row.getPackageId(),
            "overallTechNote", row.getOverallTechNote());
        add(sources, I18nResourceType.STASK_TASK_PACKAGE, row.getPackageId(), "remark", row.getRemark());
    }

    private void appendItem(List<I18nTextSource> sources, SfStaskWorkOrderItem row) {
        add(sources, I18nResourceType.STASK_WORK_ORDER_ITEM, row.getItemId(),
            "workItemNameSnapshot", row.getWorkItemNameSnapshot());
        add(sources, I18nResourceType.STASK_WORK_ORDER_ITEM, row.getItemId(),
            "categoryNameSnapshot", row.getCategoryNameSnapshot());
        add(sources, I18nResourceType.STASK_WORK_ORDER_ITEM, row.getItemId(),
            "managerRequirement", row.getManagerRequirement());
        add(sources, I18nResourceType.STASK_WORK_ORDER_ITEM, row.getItemId(),
            "techInstruction", row.getTechInstruction());
        add(sources, I18nResourceType.STASK_WORK_ORDER_ITEM, row.getItemId(),
            "manualLeaderNameSnapshot", row.getManualLeaderNameSnapshot());
        add(sources, I18nResourceType.STASK_WORK_ORDER_ITEM, row.getItemId(),
            "leaderNameSnapshot", row.getLeaderNameSnapshot());
    }

    private void appendGreenhouse(List<I18nTextSource> sources, SfStaskWorkOrderGreenhouse row) {
        add(sources, I18nResourceType.STASK_WORK_ORDER_GREENHOUSE, row.getGreenhouseItemId(),
            "greenhouseNameSnapshot", row.getGreenhouseNameSnapshot());
    }

    private void appendOrder(List<I18nTextSource> sources, SfStaskWorkOrder row) {
        add(sources, I18nResourceType.STASK_WORK_ORDER, row.getOrderId(),
            "greenhouseNameSnapshot", row.getGreenhouseNameSnapshot());
        add(sources, I18nResourceType.STASK_WORK_ORDER, row.getOrderId(),
            "workItemNameSnapshot", row.getWorkItemNameSnapshot());
        add(sources, I18nResourceType.STASK_WORK_ORDER, row.getOrderId(),
            "categoryNameSnapshot", row.getCategoryNameSnapshot());
        add(sources, I18nResourceType.STASK_WORK_ORDER, row.getOrderId(),
            "managerRequirement", row.getManagerRequirement());
        add(sources, I18nResourceType.STASK_WORK_ORDER, row.getOrderId(),
            "techInstruction", row.getTechInstruction());
        add(sources, I18nResourceType.STASK_WORK_ORDER, row.getOrderId(),
            "overallTechNote", row.getOverallTechNote());
        add(sources, I18nResourceType.STASK_WORK_ORDER, row.getOrderId(), "remark", row.getRemark());
    }

    private void appendDispatch(List<I18nTextSource> sources, SfStaskDispatch row) {
        add(sources, I18nResourceType.STASK_DISPATCH, row.getDispatchId(), "rejectReason", row.getRejectReason());
        add(sources, I18nResourceType.STASK_DISPATCH, row.getDispatchId(),
            "leaderEvaluation", row.getLeaderEvaluation());
    }

    private void appendCompletion(List<I18nTextSource> sources, SfStaskCompletion row) {
        add(sources, I18nResourceType.STASK_COMPLETION, row.getCompletionId(),
            "completionRemark", row.getCompletionRemark());
    }

    private void appendAcceptance(List<I18nTextSource> sources, SfStaskAcceptance row) {
        add(sources, I18nResourceType.STASK_ACCEPTANCE, row.getAcceptanceId(),
            "rejectReason", row.getRejectReason());
    }

    private void add(List<I18nTextSource> sources, String resourceType, Long resourceId, String fieldKey,
        String sourceText) {
        if (resourceId != null && StringUtils.isNotBlank(sourceText)) {
            sources.add(new I18nTextSource(resourceType, resourceId, fieldKey, sourceText));
        }
    }

    private void register(String tenantId, List<I18nTextSource> sources) {
        if (!sources.isEmpty()) {
            i18nTextService.registerTextsWithPreferredTranslations(tenantId, sources,
                preferredDictionaryTranslations(tenantId, sources));
        }
    }

    /**
     * 农事项和分类快照优先继承字典已成功的维文，避免同一术语被重新机翻。
     */
    private Map<I18nTextSource, String> preferredDictionaryTranslations(String tenantId,
        List<I18nTextSource> sources) {
        List<String> snapshotNames = sources.stream()
            .filter(source -> "workItemNameSnapshot".equals(source.fieldKey())
                || "categoryNameSnapshot".equals(source.fieldKey()))
            .map(I18nTextSource::sourceText)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (snapshotNames.isEmpty()) {
            return Map.of();
        }
        List<I18nTextLookup> lookups = snapshotNames.stream()
            .map(sourceText -> new I18nTextLookup(I18nResourceType.FARM_WORK_DICT, "dictName", sourceText))
            .toList();
        Map<I18nTextLookup, String> dictionaryTranslations = i18nTextService.resolveUyghurTexts(tenantId, lookups);
        if (dictionaryTranslations.isEmpty()) {
            return Map.of();
        }
        Map<I18nTextSource, String> result = new LinkedHashMap<>();
        for (I18nTextSource source : sources) {
            if (!"workItemNameSnapshot".equals(source.fieldKey())
                && !"categoryNameSnapshot".equals(source.fieldKey())) {
                continue;
            }
            String translated = dictionaryTranslations.get(new I18nTextLookup(
                I18nResourceType.FARM_WORK_DICT, "dictName", source.sourceText()));
            if (translated != null && !translated.isBlank()) {
                result.put(source, translated);
            }
        }
        return result;
    }

    private boolean isInvalid(String tenantId, Long resourceId) {
        return tenantId == null || tenantId.isBlank() || resourceId == null;
    }
}
