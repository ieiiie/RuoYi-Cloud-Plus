package com.ym.agriculture.farmtask.workorder.service.query;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskAdminReadMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskTaskPackageMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderGreenhouseMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderItemMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderMapper;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskAdminPackageQueryBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskAdminTaskQueryBo;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskTaskPackage;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrderGreenhouse;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrderItem;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskAdminPackageDetailVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskAdminPackageListRow;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskAdminPackageListVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskAdminTaskDetailVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskAdminTaskReadListRow;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskAdminTaskReadListVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkOrderDetailVo;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeAccessor;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeQueryContext;
import com.ym.agriculture.farmtask.workorder.support.SfStaskRoleNameReader;
import com.ym.agriculture.farmtask.workorder.support.SfStaskWorkOrderAssembler;
import com.ym.agriculture.farmtask.workorder.support.StaskGreenhousePlantingBatchHelper;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** 后台任务查看页查询服务。 */
@RequiredArgsConstructor
@Service
public class SfStaskAdminReadQueryService {
    private final com.ym.agriculture.shared.i18n.StaskMessageResolver messages;
    private final SfStaskAdminReadMapper adminReadMapper;
    private final SfStaskTaskPackageMapper taskPackageMapper;
    private final SfStaskWorkOrderMapper workOrderMapper;
    private final SfStaskWorkOrderItemMapper itemMapper;
    private final SfStaskWorkOrderGreenhouseMapper greenhouseMapper;
    private final SfStaskEmployeeAccessor employeeAccessor;
    private final SfStaskRoleNameReader roleNameReader;
    private final SfStaskOrderDetailBuilder detailBuilder;
    private final StaskGreenhousePlantingBatchHelper plantingBatchHelper;

    public PageResult<SfStaskAdminTaskReadListVo> queryTasks(SfStaskAdminTaskQueryBo bo, PageQuery pageQuery) {
        String tenantId = requireTenantId();
        SfStaskAdminTaskQueryBo query = normalize(bo);
        Page<SfStaskAdminTaskReadListRow> source = adminReadMapper.selectAdminTasks(pageQuery.build(), tenantId, query);
        Page<SfStaskAdminTaskReadListVo> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        List<SfStaskAdminTaskReadListRow> rows = CollUtil.emptyIfNull(source.getRecords());
        if (rows.isEmpty()) { result.setRecords(List.of()); return com.ym.agriculture.shared.common.AgriculturePageResults.build(result); }
        SfStaskEmployeeQueryContext employees = employeeContext();
        employees.preload(rows.stream().flatMap(row -> Stream.of(row.getCreatorEmployeeId(), row.getLeaderId()))
            .filter(Objects::nonNull).distinct().toList());
        Map<String, String> roleNames = roleNameReader.load(rows.stream()
            .map(SfStaskAdminTaskReadListRow::getCreatorRoleCode).filter(StringUtils::isNotBlank).distinct().toList());
        result.setRecords(rows.stream().map(row -> toTaskVo(row, employees, roleNames)).toList());
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(result);
    }

    public PageResult<SfStaskAdminPackageListVo> queryPackages(SfStaskAdminPackageQueryBo bo, PageQuery pageQuery) {
        String tenantId = requireTenantId();
        SfStaskAdminPackageQueryBo query = normalize(bo);
        Page<SfStaskAdminPackageListRow> source = adminReadMapper.selectAdminPackages(pageQuery.build(), tenantId, query);
        Page<SfStaskAdminPackageListVo> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        List<SfStaskAdminPackageListRow> rows = CollUtil.emptyIfNull(source.getRecords());
        if (rows.isEmpty()) { result.setRecords(List.of()); return com.ym.agriculture.shared.common.AgriculturePageResults.build(result); }
        List<Long> ids = rows.stream().map(SfStaskAdminPackageListRow::getPackageId).filter(Objects::nonNull).toList();
        Map<Long, List<SfStaskWorkOrderItem>> items = CollUtil.emptyIfNull(itemMapper.selectByPackageIds(tenantId, ids)).stream()
            .collect(Collectors.groupingBy(SfStaskWorkOrderItem::getPackageId));
        Map<Long, List<SfStaskWorkOrderGreenhouse>> greenhouses = CollUtil.emptyIfNull(
            greenhouseMapper.selectByPackageIds(tenantId, ids)).stream()
            .collect(Collectors.groupingBy(SfStaskWorkOrderGreenhouse::getPackageId));
        SfStaskEmployeeQueryContext employees = employeeContext();
        employees.preload(rows.stream().map(SfStaskAdminPackageListRow::getCreatorEmployeeId)
            .filter(Objects::nonNull).distinct().toList());
        Map<String, String> roleNames = roleNameReader.load(rows.stream()
            .map(SfStaskAdminPackageListRow::getCreatorRoleCode).filter(StringUtils::isNotBlank).distinct().toList());
        result.setRecords(rows.stream().map(row -> toPackageVo(row, items, greenhouses, employees, roleNames)).toList());
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(result);
    }

    public SfStaskAdminTaskDetailVo taskDetail(Long orderId) {
        requireTenantId();
        SfStaskWorkOrder order = workOrderMapper.selectById(orderId);
        ensureTenant(order == null ? null : order.getTenantId(), "任务不存在");
        SfStaskWorkOrderDetailVo source = detailBuilder.buildAdminOrderDetail(order, employeeContext());
        plantingBatchHelper.enrichWorkOrderDetailVo(source);
        SfStaskAdminTaskDetailVo target = new SfStaskAdminTaskDetailVo();
        BeanUtil.copyProperties(source, target);
        target.setStatusLabel(SfStaskWorkOrderAssembler.splitStatusLabel(source.getStatus(), messages));
        return target;
    }

    public SfStaskAdminPackageDetailVo packageDetail(Long packageId) {
        String tenantId = requireTenantId();
        SfStaskTaskPackage taskPackage = taskPackageMapper.selectById(packageId);
        ensureTenant(taskPackage == null ? null : taskPackage.getTenantId(), "任务包不存在");
        List<SfStaskWorkOrder> splits = taskPackage == null ? List.of() : CollUtil.emptyIfNull(
            workOrderMapper.selectByPackageId(tenantId, packageId));
        SfStaskWorkOrderDetailVo source = detailBuilder.buildPackageDetail(taskPackage, employeeContext(), splits);
        SfStaskAdminPackageDetailVo target = new SfStaskAdminPackageDetailVo();
        BeanUtil.copyProperties(source, target);
        target.setPackageNo(taskPackage.getPackageNo());
        target.setStatusLabel(adminPackageStatusLabel(source.getStatus()));
        target.setTaskCount((long) splits.size());
        return target;
    }

    private SfStaskAdminTaskReadListVo toTaskVo(SfStaskAdminTaskReadListRow row,
        SfStaskEmployeeQueryContext employees, Map<String, String> roleNames) {
        SfStaskAdminTaskReadListVo vo = new SfStaskAdminTaskReadListVo();
        vo.setOrderId(row.getOrderId()); vo.setOrderNo(row.getOrderNo()); vo.setPackageId(row.getPackageId());
        vo.setPackageNo(row.getPackageNo()); vo.setWorkItemId(row.getWorkItemId());
        vo.setWorkItemDisplay(row.getWorkItemNameSnapshot()); vo.setGreenhouseId(row.getGreenhouseId());
        vo.setGreenhouseDisplay(row.getGreenhouseNameSnapshot()); vo.setPlanDate(row.getPlanDate());
        vo.setStatus(row.getStatus()); vo.setStatusLabel(SfStaskWorkOrderAssembler.splitStatusLabel(row.getStatus(), messages));
        vo.setLeaderId(row.getLeaderId()); vo.setLeaderName(employees.nameOf(row.getLeaderId()));
        vo.setWorkerCount(row.getRequiredWorkerCount()); vo.setAcceptedWorkerCount(row.getAcceptedWorkerCount());
        vo.setCreatorEmployeeId(row.getCreatorEmployeeId()); vo.setCreatorEmployeeName(employees.nameOf(row.getCreatorEmployeeId()));
        vo.setCreatorRoleCode(row.getCreatorRoleCode()); vo.setCreatorRoleName(roleNameReader.resolve(roleNames, row.getCreatorRoleCode()));
        vo.setCreateTime(row.getCreateTime()); return vo;
    }

    private SfStaskAdminPackageListVo toPackageVo(SfStaskAdminPackageListRow row,
        Map<Long, List<SfStaskWorkOrderItem>> items, Map<Long, List<SfStaskWorkOrderGreenhouse>> greenhouses,
        SfStaskEmployeeQueryContext employees, Map<String, String> roleNames) {
        SfStaskAdminPackageListVo vo = new SfStaskAdminPackageListVo();
        vo.setPackageId(row.getPackageId()); vo.setPackageNo(row.getPackageNo()); vo.setPlanDate(row.getPlanDate());
        vo.setStatus(row.getStatus()); vo.setStatusLabel(adminPackageStatusLabel(row.getStatus()));
        vo.setTaskCount(row.getTaskCount()); vo.setWorkItemDisplay(packageWorkItemDisplay(items.getOrDefault(row.getPackageId(), List.of())));
        vo.setGreenhouseDisplay(packageGreenhouseDisplay(greenhouses.getOrDefault(row.getPackageId(), List.of())));
        vo.setCreatorEmployeeId(row.getCreatorEmployeeId()); vo.setCreatorEmployeeName(employees.nameOf(row.getCreatorEmployeeId()));
        vo.setCreatorRoleCode(row.getCreatorRoleCode()); vo.setCreatorRoleName(roleNameReader.resolve(roleNames, row.getCreatorRoleCode()));
        vo.setCreateTime(row.getCreateTime()); return vo;
    }

    private static String packageWorkItemDisplay(List<SfStaskWorkOrderItem> items) {
        if (CollUtil.isEmpty(items)) return "-";
        String first = StringUtils.isBlank(items.get(0).getWorkItemNameSnapshot()) ? "-" : items.get(0).getWorkItemNameSnapshot();
        return items.size() == 1 ? first : first + "等" + items.size() + "项";
    }

    private static String packageGreenhouseDisplay(List<SfStaskWorkOrderGreenhouse> rows) {
        return rows.stream().map(SfStaskWorkOrderGreenhouse::getGreenhouseId).filter(Objects::nonNull)
            .collect(Collectors.toSet()).size() + "个";
    }

    /**
     * 后台任务包的“待组长接单”表示包已完成下发；具体任务仍沿用原状态文案。
     */
    private String adminPackageStatusLabel(String status) {
        if (StaskOrderStatus.PENDING_LEADER_ACCEPT.equals(status)) {
            return "已下发";
        }
        return SfStaskWorkOrderAssembler.packageStatusLabel(status, messages);
    }

    private SfStaskAdminTaskQueryBo normalize(SfStaskAdminTaskQueryBo source) {
        SfStaskAdminTaskQueryBo query = source == null ? new SfStaskAdminTaskQueryBo() : source;
        validateDates(query.getPlanStartDate(), query.getPlanEndDate()); query.setKeyword(trim(query.getKeyword()));
        query.setStatus(upper(query.getStatus())); return query;
    }
    private SfStaskAdminPackageQueryBo normalize(SfStaskAdminPackageQueryBo source) {
        SfStaskAdminPackageQueryBo query = source == null ? new SfStaskAdminPackageQueryBo() : source;
        validateDates(query.getPlanStartDate(), query.getPlanEndDate()); query.setKeyword(trim(query.getKeyword()));
        query.setStatus(upper(query.getStatus())); query.setCreatorRoleCode(trim(query.getCreatorRoleCode())); return query;
    }
    private static void validateDates(Date start, Date end) {
        if (start != null && end != null && start.after(end)) throw new ServiceException("计划开始日期不能晚于结束日期");
    }
    private static String trim(String value) { return StringUtils.isBlank(value) ? null : value.trim(); }
    private static String upper(String value) { String text = trim(value); return text == null ? null : text.toUpperCase(Locale.ROOT); }
    private SfStaskEmployeeQueryContext employeeContext() {
        return new SfStaskEmployeeQueryContext(ids -> {
            List<SysEmployeeVo> employees = employeeAccessor.queryBasicByIds(ids);
            return employees != null ? employees : employeeAccessor.queryByIds(ids);
        });
    }
    private String requireTenantId() {
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isBlank(tenantId)) throw messages.exception(
            com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_COMMON_TENANT_CONTEXT_MISSING);
        return tenantId;
    }
    private static void ensureTenant(String entityTenantId, String message) {
        String tenantId = TenantHelper.getTenantId();
        if (entityTenantId == null || (StringUtils.isNotBlank(tenantId) && !Objects.equals(entityTenantId, tenantId))) {
            throw new ServiceException(message);
        }
    }
}
