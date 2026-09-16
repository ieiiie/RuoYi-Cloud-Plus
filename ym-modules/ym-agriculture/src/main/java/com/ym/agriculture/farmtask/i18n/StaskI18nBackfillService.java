package com.ym.agriculture.farmtask.i18n;

import com.ym.agriculture.shared.i18n.StaskI18nRegistrationResult;
import com.ym.agriculture.shared.i18n.StaskI18nScanResult;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farming.farmwork.dao.SfFarmWorkDictMapper;
import com.ym.agriculture.farming.farmwork.model.entity.SfFarmWorkDict;
import com.ym.agriculture.shared.i18n.model.I18nTextSource;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import com.ym.agriculture.shared.i18n.service.ISfI18nTextService;
import com.ym.agriculture.farming.i18n.support.FarmWorkI18nSourceFactory;
import com.ym.agriculture.farmtask.assignment.dao.SfFarmWorkAssignmentMapper;
import com.ym.agriculture.farmtask.assignment.model.entity.SfFarmWorkAssignment;
import com.ym.agriculture.farmtask.clocklocation.model.vo.SfStaskClockLocationVo;
import com.ym.agriculture.farmtask.clocklocation.service.ISfStaskClockLocationService;
import com.ym.agriculture.farmtask.i18n.model.vo.SfStaskFarmWorkDictPretranslateVo;
import com.ym.agriculture.farmtask.inventory.support.InventoryI18nBackfillService;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskTaskPackageMapper;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskTaskPackage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * stask 历史中文业务数据翻译待办回填服务。
 *
 * <p>所有资源按主键游标分批扫描，每批在独立事务中幂等登记。中途失败时已提交批次保留，
 * 再次调用会从头扫描但不会重复翻译相同原文。</p>
 */
@Service
@RequiredArgsConstructor
public class StaskI18nBackfillService {

    static final int BATCH_SIZE = 200;

    private static final Set<String> ORDER_RESOURCE_TYPES = Set.of(
        I18nResourceType.STASK_TASK_PACKAGE,
        I18nResourceType.STASK_WORK_ORDER_ITEM,
        I18nResourceType.STASK_WORK_ORDER_GREENHOUSE,
        I18nResourceType.STASK_WORK_ORDER,
        I18nResourceType.STASK_DISPATCH,
        I18nResourceType.STASK_COMPLETION,
        I18nResourceType.STASK_ACCEPTANCE
    );

    private static final Set<String> ALL_RESOURCE_TYPES = Set.of(
        I18nResourceType.FARM_WORK_DICT,
        I18nResourceType.STASK_ASSIGNMENT,
        I18nResourceType.STASK_TASK_PACKAGE,
        I18nResourceType.STASK_WORK_ORDER_ITEM,
        I18nResourceType.STASK_WORK_ORDER_GREENHOUSE,
        I18nResourceType.STASK_WORK_ORDER,
        I18nResourceType.STASK_DISPATCH,
        I18nResourceType.STASK_COMPLETION,
        I18nResourceType.STASK_ACCEPTANCE,
        I18nResourceType.STASK_CLOCK_LOCATION,
        I18nResourceType.INVENTORY_MATERIAL_CATEGORY,
        I18nResourceType.INVENTORY_MATERIAL,
        I18nResourceType.INVENTORY_LEDGER,
        I18nResourceType.INVENTORY_INBOUND,
        I18nResourceType.INVENTORY_OUTBOUND,
        I18nResourceType.INVENTORY_RETURN,
        I18nResourceType.INVENTORY_STOCKTAKE,
        I18nResourceType.INVENTORY_RECEIPT,
        I18nResourceType.ASSET_TYPE,
        I18nResourceType.ASSET_DEVICE,
        I18nResourceType.ASSET_USAGE_LOG
    );

    private final ISfI18nTextService i18nTextService;
    private final StaskI18nResourceRegistrar resourceRegistrar;
    private final SfFarmWorkDictMapper farmWorkDictMapper;
    private final SfFarmWorkAssignmentMapper assignmentMapper;
    private final SfStaskTaskPackageMapper taskPackageMapper;
    private final ISfStaskClockLocationService clockLocationService;
    private final InventoryI18nBackfillService inventoryBackfillService;
    private final PlatformTransactionManager transactionManager;

    /**
     * 扫描所有有效农事字典并幂等登记维文翻译待办。
     *
     * <p>自定义表单模板不属于翻译范围。本方法仅写入翻译待办并唤醒 Worker，
     * 不在请求线程调用翻译供应商。</p>
     *
     * @return 租户、字典和非空候选翻译资源统计
     */
    public SfStaskFarmWorkDictPretranslateVo pretranslateFarmWorkDict() {
        List<String> tenantIds = TenantHelper.ignore(this::selectFarmWorkTenantIds);
        int dictCount = 0;
        int sourceCount = 0;
        for (String tenantId : tenantIds) {
            StaskI18nScanResult summary = TenantHelper.dynamic(tenantId,
                () -> backfillFarmWork(tenantId));
            dictCount += summary.resourceCount();
            sourceCount += Math.toIntExact(summary.sourceTextCount());
        }
        return new SfStaskFarmWorkDictPretranslateVo(tenantIds.size(), dictCount, sourceCount);
    }

    /**
     * 幂等登记租户内历史文本。未传资源类型时回填所有本期资源。
     *
     * @param tenantId 租户编号
     * @param resourceTypes 资源类型集合
     * @return 扫描到的顶层业务记录数量
     */
    public int backfill(String tenantId, Collection<String> resourceTypes) {
        return backfillWithSummary(tenantId, resourceTypes).resourceCount();
    }

    /**
     * 幂等登记租户历史文本并返回本次扫描统计。
     *
     * @param tenantId 租户编号
     * @param resourceTypes 资源类型集合
     * @return 业务记录、文本和唯一中文原文统计
     */
    public StaskI18nScanResult backfillWithSummary(String tenantId, Collection<String> resourceTypes) {
        Set<String> selected = resourceTypes == null || resourceTypes.isEmpty()
            ? ALL_RESOURCE_TYPES : Set.copyOf(resourceTypes);
        StaskI18nScanResult summary = StaskI18nScanResult.empty();
        if (selected.contains(I18nResourceType.FARM_WORK_DICT)) {
            summary = summary.merge(backfillFarmWork(tenantId));
        }
        if (selected.contains(I18nResourceType.STASK_ASSIGNMENT)) {
            summary = summary.merge(backfillAssignments(tenantId));
        }
        if (selected.stream().anyMatch(ORDER_RESOURCE_TYPES::contains)) {
            summary = summary.merge(backfillPackages(tenantId, selected));
        }
        if (selected.contains(I18nResourceType.STASK_CLOCK_LOCATION)) {
            summary = summary.merge(backfillClockLocation(tenantId));
        }
        if (selected.stream().anyMatch(InventoryI18nBackfillService.RESOURCE_TYPES::contains)) {
            summary = summary.merge(inventoryBackfillService.backfill(tenantId, selected));
        }
        return summary;
    }

    private StaskI18nScanResult backfillFarmWork(String tenantId) {
        StaskI18nScanResult summary = StaskI18nScanResult.empty();
        long cursor = 0L;
        while (true) {
            List<SfFarmWorkDict> rows = selectFarmWorkBatch(tenantId, cursor);
            if (rows.isEmpty()) {
                return summary;
            }
            List<I18nTextSource> sources = new ArrayList<>();
            rows.forEach(row -> sources.addAll(FarmWorkI18nSourceFactory.sources(row)));
            inBatchTransaction(() -> i18nTextService.registerTexts(tenantId, sources));
            summary = summary.merge(StaskI18nScanResult.of(rows.size(), sources));
            cursor = rows.get(rows.size() - 1).getDictId();
        }
    }

    private List<String> selectFarmWorkTenantIds() {
        return farmWorkDictMapper.selectObjs(
            Wrappers.<SfFarmWorkDict>query()
                .select("tenant_id")
                .eq("del_flag", SystemConstants.NORMAL)
                .isNotNull("tenant_id")
                .ne("tenant_id", "")
                .groupBy("tenant_id")
                .orderByAsc("tenant_id"))
            .stream()
            .map(String::valueOf)
            .toList();
    }

    private List<SfFarmWorkDict> selectFarmWorkBatch(String tenantId, long cursor) {
        return farmWorkDictMapper.selectList(Wrappers.<SfFarmWorkDict>lambdaQuery()
            .eq(SfFarmWorkDict::getTenantId, tenantId)
            .eq(SfFarmWorkDict::getDelFlag, SystemConstants.NORMAL)
            .gt(SfFarmWorkDict::getDictId, cursor)
            .orderByAsc(SfFarmWorkDict::getDictId)
            .last("limit " + BATCH_SIZE));
    }

    private StaskI18nScanResult backfillAssignments(String tenantId) {
        StaskI18nScanResult summary = StaskI18nScanResult.empty();
        long cursor = 0L;
        while (true) {
            List<SfFarmWorkAssignment> rows = assignmentMapper.selectList(
                Wrappers.<SfFarmWorkAssignment>lambdaQuery()
                    .eq(SfFarmWorkAssignment::getTenantId, tenantId)
                    .gt(SfFarmWorkAssignment::getAssignmentId, cursor)
                    .orderByAsc(SfFarmWorkAssignment::getAssignmentId)
                    .last("limit " + BATCH_SIZE));
            if (rows.isEmpty()) {
                return summary;
            }
            List<I18nTextSource> sources = new ArrayList<>();
            inBatchTransaction(() -> sources.addAll(resourceRegistrar.registerAssignments(tenantId, rows)));
            summary = summary.merge(StaskI18nScanResult.of(rows.size(), sources));
            cursor = rows.get(rows.size() - 1).getAssignmentId();
        }
    }

    private StaskI18nScanResult backfillPackages(String tenantId, Set<String> selected) {
        StaskI18nScanResult summary = StaskI18nScanResult.empty();
        long cursor = 0L;
        while (true) {
            List<SfStaskTaskPackage> rows = taskPackageMapper.selectList(
                Wrappers.<SfStaskTaskPackage>lambdaQuery()
                    .eq(SfStaskTaskPackage::getTenantId, tenantId)
                    .gt(SfStaskTaskPackage::getPackageId, cursor)
                    .orderByAsc(SfStaskTaskPackage::getPackageId)
                    .last("limit " + BATCH_SIZE));
            if (rows.isEmpty()) {
                return summary;
            }
            StaskI18nRegistrationResult registration = inBatchTransactionResult(
                () -> resourceRegistrar.registerPackages(tenantId, rows, selected));
            summary = summary.merge(StaskI18nScanResult.of(
                registration.resourceCount(), registration.sources()));
            cursor = rows.get(rows.size() - 1).getPackageId();
        }
    }

    private StaskI18nScanResult backfillClockLocation(String tenantId) {
        SfStaskClockLocationVo location = clockLocationService.getCurrent();
        if (location == null || location.getClockLocationId() == null) {
            return StaskI18nScanResult.empty();
        }
        List<I18nTextSource> sources = List.of(
            new I18nTextSource(I18nResourceType.STASK_CLOCK_LOCATION, location.getClockLocationId(),
                "locationName", location.getLocationName()),
            new I18nTextSource(I18nResourceType.STASK_CLOCK_LOCATION, location.getClockLocationId(),
                "addressText", location.getAddressText()),
            new I18nTextSource(I18nResourceType.STASK_CLOCK_LOCATION, location.getClockLocationId(),
                "remark", location.getRemark())
        );
        inBatchTransaction(() -> i18nTextService.registerTexts(tenantId, sources));
        return StaskI18nScanResult.of(1, sources);
    }

    private void inBatchTransaction(Runnable action) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.executeWithoutResult(status -> action.run());
    }

    private <T> T inBatchTransactionResult(Supplier<T> action) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionTemplate.execute(status -> action.get());
    }
}
