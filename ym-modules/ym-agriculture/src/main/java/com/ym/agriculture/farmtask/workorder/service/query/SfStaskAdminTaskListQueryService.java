package com.ym.agriculture.farmtask.workorder.service.query;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskAdminTaskListMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderGreenhouseMapper;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderItemMapper;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskAdminTaskListQueryBo;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrderGreenhouse;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrderItem;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskAdminTaskListRow;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskAdminTaskListVo;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeAccessor;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeQueryContext;
import com.ym.agriculture.farmtask.workorder.support.SfStaskRoleNameReader;
import com.ym.agriculture.farmtask.workorder.support.SfStaskWorkOrderAssembler;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 平台后台农事任务统一列表查询。
 *
 * <p>数据库负责任务包与拆分工单的联合排序分页，当前页关联数据在服务层按 ID 批量装配。</p>
 */
@RequiredArgsConstructor
@Service
public class SfStaskAdminTaskListQueryService {

    private static final String TASK_TYPE_PACKAGE = "PACKAGE";

    private final com.ym.agriculture.shared.i18n.StaskMessageResolver messages;
    private final SfStaskAdminTaskListMapper adminTaskListMapper;
    private final SfStaskWorkOrderItemMapper itemMapper;
    private final SfStaskWorkOrderGreenhouseMapper greenhouseMapper;
    private final SfStaskEmployeeAccessor employeeAccessor;
    private final SfStaskRoleNameReader roleNameReader;

    /**
     * 数据库联合分页查询平台后台农事任务。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 后台统一任务列表
     */
    public PageResult<SfStaskAdminTaskListVo> queryPage(
        SfStaskAdminTaskListQueryBo bo, PageQuery pageQuery) {
        String tenantId = requireTenantId();
        SfStaskAdminTaskListQueryBo query = normalizeQuery(bo);
        Page<SfStaskAdminTaskListRow> source = adminTaskListMapper.selectAdminPage(
            pageQuery.build(), tenantId, query);
        Page<SfStaskAdminTaskListVo> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        List<SfStaskAdminTaskListRow> rows = source.getRecords() == null ? List.of() : source.getRecords();
        if (rows.isEmpty()) {
            result.setRecords(List.of());
            return com.ym.agriculture.shared.common.AgriculturePageResults.build(result);
        }

        List<Long> packageIds = rows.stream()
            .filter(this::isPackageRow)
            .map(SfStaskAdminTaskListRow::getPackageId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        Map<Long, List<SfStaskWorkOrderItem>> itemGroups = loadItemGroups(tenantId, packageIds);
        Map<Long, List<SfStaskWorkOrderGreenhouse>> greenhouseGroups = loadGreenhouseGroups(tenantId, packageIds);
        SfStaskEmployeeQueryContext employees = employeeContext();
        employees.preload(rows.stream()
            .flatMap(row -> Stream.of(row.getCreatorEmployeeId(), row.getLeaderId()))
            .filter(Objects::nonNull)
            .distinct()
            .toList());
        Map<String, String> roleNames = roleNameReader.load(rows.stream()
            .map(SfStaskAdminTaskListRow::getCreatorRoleCode)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .toList());

        result.setRecords(rows.stream()
            .map(row -> toVo(row, itemGroups, greenhouseGroups, employees, roleNames))
            .toList());
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(result);
    }

    private SfStaskAdminTaskListVo toVo(SfStaskAdminTaskListRow row,
        Map<Long, List<SfStaskWorkOrderItem>> itemGroups,
        Map<Long, List<SfStaskWorkOrderGreenhouse>> greenhouseGroups,
        SfStaskEmployeeQueryContext employees,
        Map<String, String> roleNames) {
        SfStaskAdminTaskListVo vo = new SfStaskAdminTaskListVo();
        vo.setRecordId(row.getRecordId());
        vo.setTaskType(row.getTaskType());
        vo.setTaskTypeLabel(isPackageRow(row) ? "任务包" : "拆分任务");
        vo.setPackageId(row.getPackageId());
        vo.setPackageNo(row.getPackageNo());
        vo.setOrderId(row.getOrderId());
        vo.setOrderNo(row.getOrderNo());
        vo.setPlanDate(row.getPlanDate());
        vo.setStatus(row.getStatus());
        vo.setCreatorEmployeeId(row.getCreatorEmployeeId());
        vo.setCreatorEmployeeName(employees.nameOf(row.getCreatorEmployeeId()));
        vo.setCreatorRoleCode(row.getCreatorRoleCode());
        vo.setCreatorRoleName(roleNameReader.resolve(roleNames, row.getCreatorRoleCode()));
        vo.setCreateTime(row.getCreateTime());

        if (isPackageRow(row)) {
            List<SfStaskWorkOrderItem> packageItems = itemGroups.getOrDefault(row.getPackageId(), List.of());
            List<SfStaskWorkOrderGreenhouse> packageGreenhouses = greenhouseGroups.getOrDefault(
                row.getPackageId(), List.of());
            vo.setWorkItemDisplay(packageWorkItemDisplay(packageItems));
            vo.setGreenhouseDisplay(packageGreenhouseDisplay(packageGreenhouses));
            vo.setStatusLabel(SfStaskWorkOrderAssembler.packageStatusLabel(row.getStatus(), messages));
            return vo;
        }

        vo.setWorkItemId(row.getWorkItemId());
        vo.setWorkItemDisplay(row.getWorkItemNameSnapshot());
        vo.setGreenhouseId(row.getGreenhouseId());
        vo.setGreenhouseDisplay(row.getGreenhouseNameSnapshot());
        vo.setLeaderId(row.getLeaderId());
        vo.setLeaderName(employees.nameOf(row.getLeaderId()));
        if (row.getLeaderId() != null) {
            vo.setWorkerCount(row.getRequiredWorkerCount());
        }
        vo.setStatusLabel(SfStaskWorkOrderAssembler.splitStatusLabel(row.getStatus(), messages));
        return vo;
    }

    private Map<Long, List<SfStaskWorkOrderItem>> loadItemGroups(String tenantId, Collection<Long> packageIds) {
        if (CollUtil.isEmpty(packageIds)) {
            return Map.of();
        }
        List<SfStaskWorkOrderItem> items = itemMapper.selectByPackageIds(tenantId, packageIds);
        if (CollUtil.isEmpty(items)) {
            return Map.of();
        }
        return items.stream()
            .collect(Collectors.groupingBy(SfStaskWorkOrderItem::getPackageId));
    }

    private Map<Long, List<SfStaskWorkOrderGreenhouse>> loadGreenhouseGroups(
        String tenantId, Collection<Long> packageIds) {
        if (CollUtil.isEmpty(packageIds)) {
            return Map.of();
        }
        List<SfStaskWorkOrderGreenhouse> greenhouses =
            greenhouseMapper.selectByPackageIds(tenantId, packageIds);
        if (CollUtil.isEmpty(greenhouses)) {
            return Map.of();
        }
        return greenhouses.stream()
            .collect(Collectors.groupingBy(SfStaskWorkOrderGreenhouse::getPackageId));
    }

    private static String packageWorkItemDisplay(List<SfStaskWorkOrderItem> items) {
        if (CollUtil.isEmpty(items)) {
            return "[包]-";
        }
        String firstName = items.get(0).getWorkItemNameSnapshot();
        String first = StringUtils.isBlank(firstName) ? "-" : firstName;
        return items.size() == 1 ? "[包]" + first : "[包]" + first + "等" + items.size() + "项";
    }

    private static String packageGreenhouseDisplay(List<SfStaskWorkOrderGreenhouse> greenhouses) {
        long count = greenhouses.stream()
            .map(SfStaskWorkOrderGreenhouse::getGreenhouseId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet())
            .size();
        return count + "个";
    }

    private boolean isPackageRow(SfStaskAdminTaskListRow row) {
        return TASK_TYPE_PACKAGE.equals(row.getTaskType());
    }

    private SfStaskAdminTaskListQueryBo normalizeQuery(SfStaskAdminTaskListQueryBo bo) {
        SfStaskAdminTaskListQueryBo query = bo == null ? new SfStaskAdminTaskListQueryBo() : bo;
        Date planStartDate = query.getPlanStartDate();
        Date planEndDate = query.getPlanEndDate();
        if (planStartDate != null && planEndDate != null && planStartDate.after(planEndDate)) {
            throw new ServiceException("计划开始日期不能晚于结束日期");
        }
        if (StringUtils.isNotBlank(query.getStatus())) {
            query.setStatus(query.getStatus().trim().toUpperCase(Locale.ROOT));
        }
        return query;
    }

    private SfStaskEmployeeQueryContext employeeContext() {
        return new SfStaskEmployeeQueryContext(ids -> {
            List<SysEmployeeVo> employees = employeeAccessor.queryBasicByIds(ids);
            return employees != null ? employees : employeeAccessor.queryByIds(ids);
        });
    }

    private String requireTenantId() {
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            throw messages.exception(
                com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_COMMON_TENANT_CONTEXT_MISSING);
        }
        return tenantId;
    }
}
