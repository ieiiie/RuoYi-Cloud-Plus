package com.ym.agriculture.farmtask.screen.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchVo;
import com.ym.agriculture.farming.batch.service.ISfPlantingBatchService;
import com.ym.agriculture.farming.farmwork.dao.SfFarmWorkDictMapper;
import com.ym.agriculture.farming.farmwork.model.entity.SfFarmWorkDict;
import com.ym.agriculture.farming.field.model.bo.SfFieldBo;
import com.ym.agriculture.farming.field.model.vo.SfFieldVo;
import com.ym.agriculture.farming.field.layout.model.vo.GreenhouseLayoutVo;
import com.ym.agriculture.farming.field.layout.service.IGreenhouseLayoutService;
import com.ym.agriculture.farming.field.service.ISfFieldService;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveDateVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveDetailVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveFieldPhotoCountVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveFieldVo;
import com.ym.agriculture.farmtask.inspectionphotoarchive.dao.SfInspectionPhotoArchiveFieldMapper;
import com.ym.agriculture.farmtask.inspectionphotoarchive.service.ISfInspectionPhotoArchiveService;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.MaterialQuery;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.MaterialVo;
import com.ym.agriculture.farmtask.inventory.service.IInventoryService;
import com.ym.agriculture.farmtask.screen.model.vo.ScreenDashboardVos;
import com.ym.agriculture.farmtask.screen.model.vo.ScreenPageVo;
import com.ym.agriculture.farmtask.screen.service.ScreenDashboardService;
import com.ym.agriculture.farmtask.sop.model.bo.SfStaskSopQueryBo;
import com.ym.agriculture.farmtask.sop.model.vo.SfStaskSopVo;
import com.ym.agriculture.farmtask.sop.service.ISfStaskSopService;
import com.ym.agriculture.farmtask.sop.support.StaskSopMasterOssAccessor;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskAcceptanceMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskCompletionMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderMapper;
import com.ym.agriculture.farmtask.leaderlabor.dao.SfStaskLeaderLaborRecordMapper;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskAcceptance;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskCompletion;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkOrderCategoryCountVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkOrderStatusCountVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkOrderWorkItemCountVo;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeAccessor;
import com.ym.agriculture.farmtask.yieldrecord.model.vo.SfStaskAnnualYieldRowVo;
import com.ym.agriculture.farmtask.yieldrecord.service.ISfStaskYieldRecordService;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import com.ym.resource.api.domain.RemoteFile;
import com.ym.system.api.RemoteTenantService;
import com.ym.system.api.domain.vo.RemoteTenantInfoVo;
import org.apache.dubbo.config.annotation.DubboReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * 大屏真实数据查询实现。
 */
@Service
@RequiredArgsConstructor
public class ScreenDashboardServiceImpl implements ScreenDashboardService {

    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Set<String> WORK_ORDER_SUMMARY_PENDING_STATUSES = Set.of(
        StaskOrderStatus.PENDING_LEADER_ACCEPT);
    @SuppressWarnings("deprecation")
    private static final Set<String> WORK_ORDER_SUMMARY_PROCESSING_STATUSES = Set.of(
        StaskOrderStatus.PENDING_LEADER_ASSIGN,
        StaskOrderStatus.ASSIGN_COMPLETE,
        StaskOrderStatus.LEADER_ARRIVED,
        StaskOrderStatus.ACCEPTANCE_REJECTED);
    private static final Set<String> WORK_ORDER_SUMMARY_PENDING_ACCEPTANCE_STATUSES = Set.of(
        StaskOrderStatus.PENDING_ACCEPTANCE);
    private static final Set<String> WORK_ORDER_SUMMARY_COMPLETED_STATUSES = Set.of(
        StaskOrderStatus.ACCEPTANCE_PASSED);
    @SuppressWarnings("deprecation")
    private static final Set<String> WORK_ORDER_SUMMARY_STATUSES = Set.of(
        StaskOrderStatus.PENDING_LEADER_ACCEPT,
        StaskOrderStatus.PENDING_LEADER_ASSIGN,
        StaskOrderStatus.ASSIGN_COMPLETE,
        StaskOrderStatus.LEADER_ARRIVED,
        StaskOrderStatus.PENDING_ACCEPTANCE,
        StaskOrderStatus.ACCEPTANCE_PASSED,
        StaskOrderStatus.ACCEPTANCE_REJECTED);
    @SuppressWarnings("deprecation")
    private static final Set<String> OPERATING_RANKING_STATUSES = Set.of(
        StaskOrderStatus.PENDING_LEADER_ACCEPT,
        StaskOrderStatus.PENDING_LEADER_ASSIGN,
        StaskOrderStatus.ASSIGN_COMPLETE,
        StaskOrderStatus.LEADER_ARRIVED,
        StaskOrderStatus.PENDING_ACCEPTANCE,
        StaskOrderStatus.ACCEPTANCE_PASSED,
        StaskOrderStatus.ACCEPTANCE_REJECTED);

    @DubboReference
    private RemoteTenantService tenantService;
    private final ISfFieldService fieldService;
    private final ISfPlantingBatchService plantingBatchService;
    private final IGreenhouseLayoutService greenhouseLayoutService;
    private final SfInspectionPhotoArchiveFieldMapper archiveFieldMapper;
    private final ISfInspectionPhotoArchiveService archiveService;
    private final ISfStaskSopService sopService;
    private final IInventoryService materialService;
    private final SfStaskWorkOrderMapper workOrderMapper;
    private final SfStaskLeaderLaborRecordMapper laborRecordMapper;
    private final ISfStaskYieldRecordService yieldRecordService;
    private final SfFarmWorkDictMapper farmWorkDictMapper;
    private final SfStaskAcceptanceMapper acceptanceMapper;
    private final SfStaskEmployeeAccessor employeeAccessor;
    private final StaskSopMasterOssAccessor ossAccessor;
    private final SfStaskCompletionMapper completionMapper;

    @Override
    public ScreenDashboardVos.Tenant tenant() {
        String tenantId = requireTenantId();
        RemoteTenantInfoVo tenant = tenantService.getTenant(tenantId);
        ScreenDashboardVos.Tenant vo = new ScreenDashboardVos.Tenant();
        vo.setTenantId(tenantId);
        vo.setTenantName(tenant == null ? tenantId : tenant.getCompanyName());
        return vo;
    }

    @Override
    public ScreenDashboardVos.Greenhouses greenhouses() {
        String tenantId = requireTenantId();
        SfFieldBo query = new SfFieldBo();
        query.setFieldType("GREENHOUSE");
        List<SfFieldVo> fields = fieldService.listFieldsOrdered(query);
        Map<Long, Long> photoCounts = new HashMap<>();
        List<Long> fieldIds = fields.stream().map(SfFieldVo::getFieldId).toList();
        if (!fieldIds.isEmpty()) {
            archiveFieldMapper.countPhotosByFieldIds(tenantId, fieldIds)
                .forEach(row -> photoCounts.put(row.getFieldId(), row.getPhotoCount()));
            countFarmPhotoCounts(tenantId, fieldIds).forEach((fieldId, count) ->
                photoCounts.merge(fieldId, count, Long::sum));
        }
        Map<Long, List<SfPlantingBatchVo>> activeBatchesByField =
            plantingBatchService.mapActiveVoByFieldIds(fieldIds);
        List<ScreenDashboardVos.Greenhouse> rows = fields.stream()
            .map(field -> toGreenhouse(field, null, photoCounts, activeBatchesByField))
            .toList();
        Map<Long, SfFieldVo> fieldById = new HashMap<>();
        fields.forEach(field -> fieldById.put(field.getFieldId(), field));
        GreenhouseLayoutVo layout = greenhouseLayoutService.resolveLayout(fields);
        List<ScreenDashboardVos.GreenhouseColumn> columns = layout.getColumns().stream().map(source -> {
            ScreenDashboardVos.GreenhouseColumn column = new ScreenDashboardVos.GreenhouseColumn();
            column.setColumnName(source.getColumnName());
            column.setColumnOrder(source.getColumnOrder());
            column.setRows(source.getRows().stream()
                .map(item -> toGreenhouse(fieldById.get(item.getFieldId()), item.getRowOrder(), photoCounts,
                    activeBatchesByField))
                .filter(Objects::nonNull)
                .toList());
            return column;
        }).toList();
        List<ScreenDashboardVos.Greenhouse> unplacedRows = layout.getUnplacedRows().stream()
            .map(item -> toGreenhouse(fieldById.get(item.getFieldId()), null, photoCounts, activeBatchesByField))
            .filter(Objects::nonNull)
            .toList();
        ScreenDashboardVos.Greenhouses result = new ScreenDashboardVos.Greenhouses();
        result.setTotal(rows.size());
        result.setWithPhotoArchiveCount(rows.stream().filter(row -> row.getPhotoArchiveCount() > 0).count());
        result.setColumns(columns);
        result.setUnplacedRows(unplacedRows);
        result.setRows(rows);
        return result;
    }

    /**
     * 批量统计验收通过农事任务的完工和验收有效照片数，与巡查归档照片合并为大棚照片总数。
     *
     * <p>照片取最新完工记录的 {@code work_photos} 和最新验收记录的
     * {@code acceptance_photos}，只统计能从 OSS 元数据解析出有效 URL 的照片，保证列表图标与农事档案详情保持一致。</p>
     */
    private Map<Long, Long> countFarmPhotoCounts(String tenantId, List<Long> fieldIds) {
        List<SfStaskWorkOrder> orders = workOrderMapper.selectScreenAcceptedFarmOrders(tenantId, fieldIds);
        if (orders == null || orders.isEmpty()) {
            return Map.of();
        }
        List<Long> orderIds = orders.stream().map(SfStaskWorkOrder::getOrderId)
            .filter(Objects::nonNull).toList();
        Map<Long, SfStaskCompletion> completionByOrderId = latestCompletions(
            completionMapper.selectByOrderIds(tenantId, orderIds));
        Map<Long, SfStaskAcceptance> acceptanceByOrderId = latestAcceptances(
            acceptanceMapper.selectByOrderIds(tenantId, orderIds));
        Map<Long, List<FarmPhotoReference>> referencesByOrderId = new HashMap<>();
        Set<Long> ossIds = new LinkedHashSet<>();
        for (SfStaskWorkOrder order : orders) {
            if (order.getOrderId() == null) {
                continue;
            }
            List<FarmPhotoReference> references = farmPhotoReferences(
                completionByOrderId.get(order.getOrderId()), acceptanceByOrderId.get(order.getOrderId()));
            referencesByOrderId.put(order.getOrderId(), references);
            references.stream().map(FarmPhotoReference::ossId).filter(Objects::nonNull).forEach(ossIds::add);
        }
        List<RemoteFile> ossRows = ossAccessor.listByIds(ossIds);
        Map<Long, RemoteFile> ossById = (ossRows == null ? List.<RemoteFile>of() : ossRows).stream()
            .filter(Objects::nonNull)
            .filter(item -> item.getOssId() != null)
            .collect(java.util.stream.Collectors.toMap(RemoteFile::getOssId, item -> item,
                (left, right) -> left));
        Map<Long, Long> result = new HashMap<>();
        for (SfStaskWorkOrder order : orders) {
            Long greenhouseId = order.getGreenhouseId();
            List<FarmPhotoReference> references = referencesByOrderId.get(order.getOrderId());
            if (greenhouseId == null || references == null) {
                continue;
            }
            long photoCount = toFarmPhotos(references, ossById).size();
            if (photoCount > 0) {
                result.merge(greenhouseId, photoCount, Long::sum);
            }
        }
        return result;
    }

    private ScreenDashboardVos.Greenhouse toGreenhouse(SfFieldVo field, Integer rowOrder,
                                                        Map<Long, Long> photoCounts,
                                                        Map<Long, List<SfPlantingBatchVo>> activeBatchesByField) {
        if (field == null) {
            return null;
        }
        ScreenDashboardVos.Greenhouse vo = new ScreenDashboardVos.Greenhouse();
        vo.setFieldId(field.getFieldId());
        vo.setFieldCode(field.getFieldCode());
        vo.setFieldName(field.getFieldName());
        vo.setGreenhouseShortName(field.getGreenhouseShortName());
        vo.setGreenhouseColor(field.getGreenhouseColor());
        List<SfPlantingBatchVo> activeBatches = activeBatchesByField.get(field.getFieldId());
        if (activeBatches != null && !activeBatches.isEmpty()) {
            SfPlantingBatchVo batch = activeBatches.get(0);
            vo.setSpeciesName(batch.getSpeciesName());
            vo.setSpeciesImageUrl(batch.getSpeciesImageUrl());
        }
        vo.setSortOrder(field.getSortOrder());
        vo.setRowOrder(rowOrder);
        vo.setStatusDisplay(field.getFieldStatusDisplay());
        vo.setAreaMu(field.getAreaMu());
        vo.setPhotoArchiveCount(photoCounts.getOrDefault(field.getFieldId(), 0L));
        return vo;
    }

    @Override
    public ScreenDashboardVos.PhotoArchives photoArchives(Long fieldId) {
        String tenantId = requireTenantId();
        SfFieldVo field = requireGreenhouse(fieldId);
        List<Long> archiveIds = TenantHelper.dynamic(tenantId,
            () -> archiveService.listDates().stream()
                .map(SfInspectionPhotoArchiveDateVo::getArchiveId)
                .toList());
        List<ScreenDashboardVos.PhotoArchive> archives = archiveIds.stream()
            .map(archiveId -> TenantHelper.dynamic(tenantId, () -> archiveService.getDetail(archiveId)))
            .map(detail -> toPhotoArchive(detail, fieldId))
            .filter(Objects::nonNull)
            .toList();
        ScreenDashboardVos.PhotoArchives result = new ScreenDashboardVos.PhotoArchives();
        result.setFieldId(fieldId);
        result.setFieldName(field.getFieldName());
        result.setArchives(archives);
        return result;
    }

    @Override
    public ScreenDashboardVos.FarmArchiveDates farmArchiveDates(Long fieldId) {
        String tenantId = requireTenantId();
        SfFieldVo field = requireGreenhouse(fieldId);
        List<SfStaskWorkOrder> sourceOrders = workOrderMapper.selectScreenAcceptedFarmOrders(
            tenantId, fieldId, null, null);
        List<SfStaskWorkOrder> orders = sourceOrders == null ? List.of() : sourceOrders;
        Map<LocalDate, Long> countsByDate = new TreeMap<>(Comparator.reverseOrder());
        for (SfStaskWorkOrder order : orders) {
            LocalDate archiveDate = toShanghaiDate(order.getPlanDate());
            if (archiveDate != null) {
                countsByDate.merge(archiveDate, 1L, Long::sum);
            }
        }
        List<ScreenDashboardVos.FarmArchiveDate> dates = new ArrayList<>(countsByDate.size());
        countsByDate.forEach((archiveDate, taskCount) -> {
            ScreenDashboardVos.FarmArchiveDate item = new ScreenDashboardVos.FarmArchiveDate();
            item.setArchiveDate(archiveDate);
            item.setTaskCount(taskCount);
            dates.add(item);
        });
        ScreenDashboardVos.FarmArchiveDates result = new ScreenDashboardVos.FarmArchiveDates();
        result.setFieldId(fieldId);
        result.setFieldName(field.getFieldName());
        result.setDates(dates);
        return result;
    }

    @Override
    public ScreenDashboardVos.FarmArchiveTasks farmArchiveTasks(Long fieldId, LocalDate archiveDate) {
        String tenantId = requireTenantId();
        SfFieldVo field = requireGreenhouse(fieldId);
        Date[] range = dayRange(archiveDate);
        List<SfStaskWorkOrder> sourceOrders = workOrderMapper.selectScreenAcceptedFarmOrders(
            tenantId, fieldId, range[0], range[1]);
        List<SfStaskWorkOrder> orders = new ArrayList<>(sourceOrders == null ? List.of() : sourceOrders);
        Map<Long, SfFarmWorkDict> dictById = farmWorkDicts(tenantId, orders);
        orders.sort(farmOrderComparator(dictById));

        List<Long> orderIds = orders.stream().map(SfStaskWorkOrder::getOrderId)
            .filter(Objects::nonNull).toList();
        Map<Long, SfStaskCompletion> completionByOrderId = latestCompletions(
            completionMapper.selectByOrderIds(tenantId, orderIds));
        Map<Long, SfStaskAcceptance> acceptanceByOrderId = latestAcceptances(
            acceptanceMapper.selectByOrderIds(tenantId, orderIds));
        Map<Long, String> leaderNames = leaderNames(orders);
        List<List<FarmPhotoReference>> photoReferences = new ArrayList<>(orders.size());
        Set<Long> ossIds = new LinkedHashSet<>();
        for (SfStaskWorkOrder order : orders) {
            // 同一任务先放组长提交的完工照片，再放验收照片，保持大屏任务卡片和预览顺序一致。
            List<FarmPhotoReference> references = farmPhotoReferences(
                completionByOrderId.get(order.getOrderId()), acceptanceByOrderId.get(order.getOrderId()));
            photoReferences.add(references);
            references.stream().map(FarmPhotoReference::ossId).filter(Objects::nonNull).forEach(ossIds::add);
        }
        List<RemoteFile> ossRows = ossAccessor.listByIds(ossIds);
        Map<Long, RemoteFile> ossById = (ossRows == null ? List.<RemoteFile>of() : ossRows).stream()
            .filter(Objects::nonNull)
            .filter(item -> item.getOssId() != null)
            .collect(java.util.stream.Collectors.toMap(RemoteFile::getOssId, item -> item, (left, right) -> left));

        List<ScreenDashboardVos.FarmArchiveTask> tasks = new ArrayList<>(orders.size());
        for (int index = 0; index < orders.size(); index++) {
            SfStaskWorkOrder order = orders.get(index);
            ScreenDashboardVos.FarmArchiveTask task = new ScreenDashboardVos.FarmArchiveTask();
            task.setWorkItemName(defaultText(order.getWorkItemNameSnapshot(), "未命名农事"));
            task.setLeaderName(leaderNames.get(order.getLeaderId()));
            task.setPhotos(toFarmPhotos(photoReferences.get(index), ossById));
            tasks.add(task);
        }
        ScreenDashboardVos.FarmArchiveTasks result = new ScreenDashboardVos.FarmArchiveTasks();
        result.setFieldId(fieldId);
        result.setFieldName(field.getFieldName());
        result.setArchiveDate(archiveDate);
        result.setTasks(tasks);
        return result;
    }

    private SfFieldVo requireGreenhouse(Long fieldId) {
        SfFieldVo field = fieldService.queryById(fieldId);
        if (field == null || !"GREENHOUSE".equals(field.getFieldType())) {
            throw new ServiceException("大棚不存在或无权访问");
        }
        return field;
    }

    private Date[] dayRange(LocalDate date) {
        return new Date[]{
            Date.from(date.atStartOfDay(SHANGHAI_ZONE).toInstant()),
            Date.from(date.plusDays(1).atStartOfDay(SHANGHAI_ZONE).toInstant())
        };
    }

    private LocalDate toShanghaiDate(Date value) {
        return value == null ? null : value.toInstant().atZone(SHANGHAI_ZONE).toLocalDate();
    }

    private Map<Long, SfFarmWorkDict> farmWorkDicts(String tenantId, List<SfStaskWorkOrder> orders) {
        Set<Long> dictIds = new LinkedHashSet<>();
        for (SfStaskWorkOrder order : orders) {
            if (order.getCategoryIdSnapshot() != null) {
                dictIds.add(order.getCategoryIdSnapshot());
            }
            if (order.getWorkItemId() != null) {
                dictIds.add(order.getWorkItemId());
            }
        }
        List<SfFarmWorkDict> rows = farmWorkDictMapper.selectNormalByIds(tenantId, dictIds);
        Map<Long, SfFarmWorkDict> result = new HashMap<>();
        if (rows != null) {
            rows.stream().filter(Objects::nonNull).filter(item -> item.getDictId() != null)
                .forEach(item -> result.put(item.getDictId(), item));
        }
        return result;
    }

    private Comparator<SfStaskWorkOrder> farmOrderComparator(Map<Long, SfFarmWorkDict> dictById) {
        return Comparator
            .comparingInt((SfStaskWorkOrder order) -> dictSortOrder(order.getCategoryIdSnapshot(), dictById))
            .thenComparingLong(order -> dictSortId(order.getCategoryIdSnapshot()))
            .thenComparingInt(order -> dictSortOrder(order.getWorkItemId(), dictById))
            .thenComparingLong(order -> dictSortId(order.getWorkItemId()))
            .thenComparingLong(order -> order.getOrderId() == null ? Long.MAX_VALUE : order.getOrderId());
    }

    private int dictSortOrder(Long dictId, Map<Long, SfFarmWorkDict> dictById) {
        SfFarmWorkDict dict = dictId == null ? null : dictById.get(dictId);
        return dict == null || dict.getSortOrder() == null ? Integer.MAX_VALUE : dict.getSortOrder();
    }

    private long dictSortId(Long dictId) {
        return dictId == null ? Long.MAX_VALUE : dictId;
    }

    private Map<Long, SfStaskAcceptance> latestAcceptances(List<SfStaskAcceptance> rows) {
        Map<Long, SfStaskAcceptance> result = new HashMap<>();
        if (rows == null) {
            return result;
        }
        for (SfStaskAcceptance row : rows) {
            if (row == null || row.getOrderId() == null) {
                continue;
            }
            result.merge(row.getOrderId(), row, (existing, candidate) ->
                later(candidate.getAcceptedAt(), existing.getAcceptedAt()) ? candidate : existing);
        }
        return result;
    }

    private Map<Long, SfStaskCompletion> latestCompletions(List<SfStaskCompletion> rows) {
        Map<Long, SfStaskCompletion> result = new HashMap<>();
        if (rows == null) {
            return result;
        }
        for (SfStaskCompletion row : rows) {
            if (row == null || row.getOrderId() == null) {
                continue;
            }
            result.merge(row.getOrderId(), row, (existing, candidate) ->
                later(candidate.getCompletedAt(), existing.getCompletedAt()) ? candidate : existing);
        }
        return result;
    }

    private Map<Long, String> leaderNames(List<SfStaskWorkOrder> orders) {
        Set<Long> leaderIds = new LinkedHashSet<>();
        orders.stream().map(SfStaskWorkOrder::getLeaderId).filter(Objects::nonNull).forEach(leaderIds::add);
        if (leaderIds.isEmpty()) {
            return Map.of();
        }
        List<SysEmployeeVo> rows = employeeAccessor.queryBasicByIds(leaderIds);
        Map<Long, String> result = new HashMap<>();
        if (rows != null) {
            rows.stream().filter(Objects::nonNull).filter(item -> item.getEmployeeId() != null)
                .filter(item -> hasText(item.getName()))
                .forEach(item -> result.put(item.getEmployeeId(), item.getName().trim()));
        }
        return result;
    }

    /**
     * 按产品约定拼接同一任务的照片：组长完成作业照片在前，验收照片在后。
     *
     * <p>两类照片均来自各自最新记录；记录缺失、JSON 无效或 OSS 文件失效时只丢弃对应照片，任务卡片仍然保留。</p>
     */
    private List<FarmPhotoReference> farmPhotoReferences(SfStaskCompletion completion,
        SfStaskAcceptance acceptance) {
        List<FarmPhotoReference> result = new ArrayList<>();
        result.addAll(photoReferences(completion == null ? null : completion.getWorkPhotos(), "作业完成照片"));
        result.addAll(photoReferences(acceptance == null ? null : acceptance.getAcceptancePhotos(), "验收照片"));
        return result;
    }

    private List<FarmPhotoReference> photoReferences(String photoJson, String fallbackName) {
        if (!hasText(photoJson)) {
            return List.of();
        }
        Object parsed;
        try {
            parsed = JSON.parse(photoJson);
        } catch (RuntimeException exception) {
            return List.of();
        }
        List<FarmPhotoReference> result = new ArrayList<>();
        if (parsed instanceof JSONArray array) {
            array.forEach(item -> addPhotoReference(result, item, fallbackName));
        } else {
            addPhotoReference(result, parsed, fallbackName);
        }
        return result;
    }

    private void addPhotoReference(List<FarmPhotoReference> target, Object source, String fallbackName) {
        if (source instanceof JSONObject object) {
            Long ossId = firstLong(object, "ossId", "id", "fileId", "photoId");
            String originalName = firstText(object, "originalName", "name", "fileName");
            String url = firstText(object, "url", "fileUrl");
            if (ossId != null) {
                target.add(new FarmPhotoReference(ossId, originalName, url, fallbackName));
            }
            return;
        }
        Long ossId = toLong(source);
        if (ossId != null) {
            target.add(new FarmPhotoReference(ossId, null, null, fallbackName));
        }
    }

    private Long firstLong(JSONObject object, String... names) {
        for (String name : names) {
            Long value = toLong(object.get(name));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstText(JSONObject object, String... names) {
        for (String name : names) {
            Object value = object.get(name);
            if (value != null && hasText(String.valueOf(value))) {
                return String.valueOf(value).trim();
            }
        }
        return null;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || !hasText(String.valueOf(value))) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private List<ScreenDashboardVos.FarmPhoto> toFarmPhotos(List<FarmPhotoReference> references,
        Map<Long, RemoteFile> ossById) {
        List<ScreenDashboardVos.FarmPhoto> result = new ArrayList<>();
        if (references == null) {
            return result;
        }
        for (FarmPhotoReference reference : references) {
            RemoteFile oss = ossById.get(reference.ossId());
            if (oss == null || !hasText(oss.getUrl())) {
                continue;
            }
            ScreenDashboardVos.FarmPhoto photo = new ScreenDashboardVos.FarmPhoto();
            photo.setPhotoId(reference.ossId());
            photo.setOriginalName(defaultText(oss.getOriginalName(),
                defaultText(reference.originalName(), reference.fallbackName())));
            photo.setUrl(oss.getUrl());
            result.add(photo);
        }
        return result;
    }

    private boolean later(Date candidate, Date current) {
        return candidate != null && (current == null || candidate.after(current));
    }

    private String defaultText(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record FarmPhotoReference(Long ossId, String originalName, String url, String fallbackName) {
    }

    @Override
    public ScreenPageVo<ScreenDashboardVos.Sop> sops(int pageNum, int pageSize) {
        PageResult<SfStaskSopVo> page = sopService.page(new SfStaskSopQueryBo(), pageQuery(pageNum, pageSize));
        List<ScreenDashboardVos.Sop> rows = page.getRows().stream().map(this::toSop).toList();
        return ScreenPageVo.of(page.getTotal(), rows);
    }

    @Override
    public ScreenDashboardVos.SopDetail sopDetail(Long sopId) {
        SfStaskSopVo sop = sopService.getById(sopId);
        ScreenDashboardVos.SopDetail detail = new ScreenDashboardVos.SopDetail();
        copySop(sop, detail);
        detail.setContentBlocks(sop.getContentBlocks());
        return detail;
    }

    @Override
    public ScreenPageVo<ScreenDashboardVos.Material> materials(int pageNum, int pageSize) {
        PageResult<MaterialVo> page = materialService.pageMaterials(
            new MaterialQuery(), pageQuery(pageNum, pageSize));
        List<ScreenDashboardVos.Material> rows = page.getRows().stream().map(this::toMaterial).toList();
        return ScreenPageVo.of(page.getTotal(), rows);
    }

    @Override
    public ScreenDashboardVos.WorkOrderSummary workOrderSummary() {
        String tenantId = requireTenantId();
        Date[] today = todayRange();
        List<SfStaskWorkOrderStatusCountVo> statusCounts = workOrderMapper.selectStatusCountsByPlanDate(
            tenantId, today[0], today[1], WORK_ORDER_SUMMARY_STATUSES);
        long pending = 0L;
        long processing = 0L;
        long pendingAcceptance = 0L;
        long completedToday = 0L;
        List<SfStaskWorkOrderStatusCountVo> rows = statusCounts == null ? List.of() : statusCounts;
        for (SfStaskWorkOrderStatusCountVo statusCount : rows) {
            if (statusCount == null || statusCount.getStatus() == null) {
                continue;
            }
            if (WORK_ORDER_SUMMARY_PENDING_STATUSES.contains(statusCount.getStatus())) {
                pending += statusCount.getOrderCount();
            } else if (WORK_ORDER_SUMMARY_PROCESSING_STATUSES.contains(statusCount.getStatus())) {
                processing += statusCount.getOrderCount();
            } else if (WORK_ORDER_SUMMARY_PENDING_ACCEPTANCE_STATUSES.contains(statusCount.getStatus())) {
                pendingAcceptance += statusCount.getOrderCount();
            } else if (WORK_ORDER_SUMMARY_COMPLETED_STATUSES.contains(statusCount.getStatus())) {
                completedToday += statusCount.getOrderCount();
            }
        }
        ScreenDashboardVos.WorkOrderSummary vo = new ScreenDashboardVos.WorkOrderSummary();
        vo.setPendingCount(pending);
        vo.setPendingAcceptanceCount(pendingAcceptance);
        vo.setProcessingCount(processing);
        vo.setCompletedTodayCount(completedToday);
        return vo;
    }

    @Override
    public ScreenDashboardVos.WorkOrderCategoryRanking workOrderCategoryRanking(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 10);
        List<SfStaskWorkOrderCategoryCountVo> source = workOrderMapper.selectCategoryRanking(
            requireTenantId(), OPERATING_RANKING_STATUSES, safeLimit);
        List<ScreenDashboardVos.WorkOrderCategoryRank> rows = new ArrayList<>(source.size());
        for (int index = 0; index < source.size(); index++) {
            SfStaskWorkOrderCategoryCountVo item = source.get(index);
            ScreenDashboardVos.WorkOrderCategoryRank row = new ScreenDashboardVos.WorkOrderCategoryRank();
            row.setRank(index + 1);
            row.setCategoryName(item.getCategoryName());
            row.setOrderCount(item.getOrderCount());
            rows.add(row);
        }
        ScreenDashboardVos.WorkOrderCategoryRanking result =
            new ScreenDashboardVos.WorkOrderCategoryRanking();
        result.setRows(rows);
        result.setCaliber("有效运营拆分工单，按分类名称快照汇总");
        return result;
    }

    @Override
    public ScreenDashboardVos.WorkOrderWorkItemRanking workOrderWorkItemRanking(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 10);
        List<SfStaskWorkOrderWorkItemCountVo> source = workOrderMapper.selectWorkItemRanking(
            requireTenantId(), OPERATING_RANKING_STATUSES, safeLimit);
        List<ScreenDashboardVos.WorkOrderWorkItemRank> rows = new ArrayList<>(source.size());
        for (int index = 0; index < source.size(); index++) {
            SfStaskWorkOrderWorkItemCountVo item = source.get(index);
            ScreenDashboardVos.WorkOrderWorkItemRank row = new ScreenDashboardVos.WorkOrderWorkItemRank();
            row.setRank(index + 1);
            row.setWorkItemName(item.getWorkItemName());
            row.setOrderCount(item.getOrderCount());
            rows.add(row);
        }
        ScreenDashboardVos.WorkOrderWorkItemRanking result =
            new ScreenDashboardVos.WorkOrderWorkItemRanking();
        result.setRows(rows);
        result.setCaliber("有效运营拆分工单，按农事项目名称快照汇总");
        return result;
    }

    @Override
    public ScreenDashboardVos.TodayLabor todayLabor() {
        LocalDate workDate = LocalDate.now(SHANGHAI_ZONE);
        BigDecimal workerCount = laborRecordMapper.sumByPlanDate(requireTenantId(), workDate);
        ScreenDashboardVos.TodayLabor vo = new ScreenDashboardVos.TodayLabor();
        vo.setWorkDate(workDate);
        vo.setWorkerCount((workerCount == null ? BigDecimal.ZERO : workerCount).setScale(2));
        vo.setUnit("人");
        vo.setCaliber("当日组长计划日用工记录汇总，不含组长");
        return vo;
    }

    @Override
    public ScreenDashboardVos.CumulativeLabor cumulativeLabor() {
        BigDecimal workerCount = laborRecordMapper.sumAll(requireTenantId());
        ScreenDashboardVos.CumulativeLabor vo = new ScreenDashboardVos.CumulativeLabor();
        vo.setWorkerCount((workerCount == null ? BigDecimal.ZERO : workerCount).setScale(2));
        vo.setUnit("人");
        vo.setCaliber("全部组长计划日用工记录汇总，不含组长");
        return vo;
    }

    @Override
    public ScreenDashboardVos.AnnualYield annualYield() {
        LocalDate today = LocalDate.now(SHANGHAI_ZONE);
        LocalDate startDate = today.withDayOfYear(1);
        List<SfStaskAnnualYieldRowVo> source =
            yieldRecordService.annualYield(startDate, today);
        ScreenDashboardVos.AnnualYield vo = new ScreenDashboardVos.AnnualYield();
        vo.setYear(today.getYear());
        vo.setStartDate(startDate);
        vo.setEndDate(today);
        vo.setUnit("公斤");
        vo.setRows(source.stream().map(item -> {
            ScreenDashboardVos.AnnualYieldRow row =
                new ScreenDashboardVos.AnnualYieldRow();
            row.setVarietyId(item.getVarietyId());
            row.setVarietyName(item.getVarietyName());
            row.setYieldKg(item.getYieldKg());
            return row;
        }).toList());
        return vo;
    }

    private ScreenDashboardVos.PhotoArchive toPhotoArchive(SfInspectionPhotoArchiveDetailVo detail, Long fieldId) {
        SfInspectionPhotoArchiveFieldVo archiveField = detail.getFields().stream()
            .filter(field -> Objects.equals(field.getFieldId(), fieldId))
            .findFirst()
            .orElse(null);
        if (archiveField == null || archiveField.getPhotos() == null || archiveField.getPhotos().isEmpty()) {
            return null;
        }
        ScreenDashboardVos.PhotoArchive archive = new ScreenDashboardVos.PhotoArchive();
        archive.setArchiveId(detail.getArchiveId());
        archive.setArchiveDate(detail.getArchiveDate());
        archive.setPhotos(archiveField.getPhotos().stream().map(photo -> {
            ScreenDashboardVos.Photo target = new ScreenDashboardVos.Photo();
            target.setPhotoId(photo.getPhotoId());
            target.setOriginalName(photo.getOriginalName());
            target.setUrl(photo.getUrl());
            target.setUploadedAt(photo.getCreateTime());
            return target;
        }).toList());
        return archive;
    }

    private ScreenDashboardVos.Sop toSop(SfStaskSopVo source) {
        ScreenDashboardVos.Sop target = new ScreenDashboardVos.Sop();
        copySop(source, target);
        return target;
    }

    private void copySop(SfStaskSopVo source, ScreenDashboardVos.Sop target) {
        target.setSopId(source.getSopId());
        target.setWorkItemName(source.getWorkItemName());
        target.setCropScope(source.getCropScope());
        target.setCropSpeciesName(source.getCropSpeciesName());
        target.setLanguage(source.getLanguage());
        target.setUpdateTime(source.getUpdateTime());
    }

    private ScreenDashboardVos.Material toMaterial(MaterialVo source) {
        ScreenDashboardVos.Material target = new ScreenDashboardVos.Material();
        target.setMaterialId(source.getId());
        target.setMaterialCode(source.getMaterialCode());
        target.setMaterialName(source.getMaterialName());
        target.setCategory(source.getCategory());
        target.setStockQuantity(source.getQuantity());
        target.setUnit(source.getUnit());
        target.setUpdateTime(source.getUpdateTime());
        return target;
    }

    private PageQuery pageQuery(int pageNum, int pageSize) {
        return new PageQuery(Math.min(Math.max(pageSize, 1), 200), Math.max(pageNum, 1));
    }

    private Date[] todayRange() {
        LocalDate today = LocalDate.now(SHANGHAI_ZONE);
        return new Date[]{Date.from(today.atStartOfDay(SHANGHAI_ZONE).toInstant()),
            Date.from(today.plusDays(1).atStartOfDay(SHANGHAI_ZONE).toInstant())};
    }

    private String requireTenantId() {
        String tenantId = TenantHelper.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new ServiceException("缺少租户上下文");
        }
        return tenantId;
    }
}
