package com.ym.agriculture.farmtask.workorder.service.query;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskTaskPackageMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderGreenhouseMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderMapper;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskWorkOrderPageBo;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskTaskPackage;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrderGreenhouse;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskGreenhouseBriefVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkOrderVo;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeAccessor;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeQueryContext;
import com.ym.agriculture.farmtask.workorder.support.SfStaskWorkOrderAssembler;
import com.ym.agriculture.farmtask.workorder.support.StaskGreenhousePlantingBatchHelper;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * stask 管理端任务包与拆分工单混合分页查询。
 */
@RequiredArgsConstructor
@Service
public class SfStaskWorkOrderPageQueryService {

    private final com.ym.agriculture.shared.i18n.StaskMessageResolver messages;
    private final SfStaskWorkOrderMapper workOrderMapper;
    private final SfStaskTaskPackageMapper taskPackageMapper;
    private final SfStaskWorkOrderGreenhouseMapper greenhouseMapper;
    private final SfStaskEmployeeAccessor employeeAccessor;
    private final StaskGreenhousePlantingBatchHelper plantingBatchHelper;

    /**
     * 分页查询任务包与拆分工单。
     *
     * @param bo 查询条件
     * @param pageQuery 分页条件
     * @return 工单分页数据
     */
    public PageResult<SfStaskWorkOrderVo> queryPage(SfStaskWorkOrderPageBo bo, PageQuery pageQuery) {
        String tenantId = requireTenantId();
        SfStaskEmployeeQueryContext employees = employeeContext();
        List<SfStaskTaskPackage> packages = includePackageRows(bo)
            ? taskPackageMapper.selectByPageFilters(tenantId, bo == null ? null : bo.getStatus(),
                bo == null ? null : bo.getPlanStartDate(), bo == null ? null : bo.getPlanEndDate())
            : List.of();
        List<SfStaskWorkOrder> splits = workOrderMapper.selectList(buildQuery(tenantId, bo));
        employees.preload(Stream.concat(packages.stream().map(SfStaskTaskPackage::getCreatorEmployeeId),
                splits.stream().flatMap(row -> Stream.of(row.getLeaderId(), row.getCreatorEmployeeId())))
            .filter(Objects::nonNull).distinct().toList());
        Map<Long, List<SfStaskGreenhouseBriefVo>> packageGreenhouses = loadPackageGreenhouses(
            tenantId, packages.stream().map(SfStaskTaskPackage::getPackageId).toList());
        Map<Long, List<SfStaskGreenhouseBriefVo>> orderGreenhouses = loadOrderGreenhouses(
            tenantId, splits.stream().map(SfStaskWorkOrder::getOrderId).toList());

        List<SfStaskWorkOrderVo> rows = new ArrayList<>();
        packages.forEach(row -> rows.add(SfStaskWorkOrderAssembler.toPackageVo(row, employees.employees(),
            packageGreenhouses.getOrDefault(row.getPackageId(), List.of()))));
        splits.forEach(row -> rows.add(SfStaskWorkOrderAssembler.toWorkOrderVo(
            row, employees.employees(), orderGreenhouses)));
        rows.sort(Comparator.comparing(SfStaskWorkOrderVo::getCreateTime,
            Comparator.nullsLast(Comparator.reverseOrder())));
        plantingBatchHelper.enrichWorkOrderVos(rows);
        Page<SfStaskWorkOrderVo> page = pageQuery.build();
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(rows, page);
    }

    private LambdaQueryWrapper<SfStaskWorkOrder> buildQuery(String tenantId, SfStaskWorkOrderPageBo bo) {
        SfStaskWorkOrderPageBo query = bo == null ? new SfStaskWorkOrderPageBo() : bo;
        return Wrappers.<SfStaskWorkOrder>lambdaQuery()
            .eq(SfStaskWorkOrder::getTenantId, tenantId)
            .eq(StringUtils.isNotBlank(query.getStatus()), SfStaskWorkOrder::getStatus, query.getStatus())
            .eq(ObjectUtil.isNotNull(query.getGreenhouseId()),
                SfStaskWorkOrder::getGreenhouseId, query.getGreenhouseId())
            .eq(ObjectUtil.isNotNull(query.getWorkItemId()),
                SfStaskWorkOrder::getWorkItemId, query.getWorkItemId())
            .eq(ObjectUtil.isNotNull(query.getLeaderId()),
                SfStaskWorkOrder::getLeaderId, query.getLeaderId())
            .ge(ObjectUtil.isNotNull(query.getPlanStartDate()),
                SfStaskWorkOrder::getPlanDate, query.getPlanStartDate())
            .le(ObjectUtil.isNotNull(query.getPlanEndDate()),
                SfStaskWorkOrder::getPlanDate, query.getPlanEndDate())
            .orderByDesc(SfStaskWorkOrder::getCreateTime);
    }

    private static boolean includePackageRows(SfStaskWorkOrderPageBo bo) {
        return bo == null || bo.getGreenhouseId() == null && bo.getWorkItemId() == null && bo.getLeaderId() == null;
    }

    private Map<Long, List<SfStaskGreenhouseBriefVo>> loadPackageGreenhouses(
        String tenantId, Collection<Long> packageIds) {
        Map<Long, List<SfStaskGreenhouseBriefVo>> result = new HashMap<>();
        if (CollUtil.isEmpty(packageIds)) {
            return result;
        }
        greenhouseMapper.selectByPackageIds(tenantId, packageIds).stream()
            .collect(java.util.stream.Collectors.groupingBy(SfStaskWorkOrderGreenhouse::getPackageId))
            .forEach((packageId, rows) -> result.put(
                packageId, SfStaskWorkOrderAssembler.toDistinctGreenhouseBriefs(rows)));
        return result;
    }

    private Map<Long, List<SfStaskGreenhouseBriefVo>> loadOrderGreenhouses(
        String tenantId, Collection<Long> orderIds) {
        Map<Long, List<SfStaskGreenhouseBriefVo>> result = new HashMap<>();
        if (CollUtil.isEmpty(orderIds)) {
            return result;
        }
        greenhouseMapper.selectByOrderIds(tenantId, orderIds).stream()
            .collect(java.util.stream.Collectors.groupingBy(SfStaskWorkOrderGreenhouse::getOrderId))
            .forEach((orderId, rows) -> result.put(
                orderId, SfStaskWorkOrderAssembler.toDistinctGreenhouseBriefs(rows)));
        return result;
    }

    private SfStaskEmployeeQueryContext employeeContext() {
        return new SfStaskEmployeeQueryContext(ids -> {
            List<SysEmployeeVo> rows = employeeAccessor.queryBasicByIds(ids);
            return rows != null ? rows : employeeAccessor.queryByIds(ids);
        });
    }

    private String requireTenantId() {
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_COMMON_TENANT_CONTEXT_MISSING);
        }
        return tenantId;
    }
}
