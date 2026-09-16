package com.ym.agriculture.farmtask.i18n;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.shared.i18n.model.I18nTextSource;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;
import com.ym.agriculture.farmtask.employee.dao.SysEmployeeMapper;
import com.ym.agriculture.farmtask.employee.model.entity.SysEmployee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 在主数据源中分批读取 stask 人员姓名，供全租户翻译预处理使用。
 */
@Component
@RequiredArgsConstructor
public class StaskEmployeeI18nPretranslateReader {

    private static final Set<String> STASK_ROLES = Set.of(
        EmployeeConstants.APP_ROLE_STASK_PRODUCTION_ADMIN,
        EmployeeConstants.APP_ROLE_STASK_EXPERT,
        EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER,
        EmployeeConstants.APP_ROLE_STASK_LEADER,
        EmployeeConstants.APP_ROLE_STASK_WORKER
    );

    private final SysEmployeeMapper employeeMapper;

    /**
     * 按主键游标读取指定租户的 stask 人员姓名。
     *
     * @param tenantId 租户编号
     * @param cursor 上一批最后一个员工主键
     * @param batchSize 批次大小
     * @return 人员翻译源批次
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public EmployeeI18nBatch readBatch(String tenantId, long cursor, int batchSize) {
        // 外层全租户预热已设置动态租户。TenantHelper.dynamic 不是可重入栈，
        // 此处再次嵌套会在返回时清掉外层租户，导致后续翻译库查询回落到登录租户。
        // 查询已显式限定 tenant_id，因此忽略租户插件即可安全读取目标租户。
        List<SysEmployee> rows = TenantHelper.ignore(() -> employeeMapper.selectList(
            Wrappers.<SysEmployee>lambdaQuery()
                .eq(SysEmployee::getTenantId, tenantId)
                .eq(SysEmployee::getDelFlag, SystemConstants.NORMAL)
                .in(SysEmployee::getAppRoleCode, STASK_ROLES)
                .gt(SysEmployee::getEmployeeId, cursor)
                .orderByAsc(SysEmployee::getEmployeeId)
                .last("limit " + batchSize)));
        List<I18nTextSource> sources = rows.stream()
            .filter(row -> row.getEmployeeId() != null && StringUtils.isNotBlank(row.getName()))
            .map(row -> new I18nTextSource(I18nResourceType.SYSTEM_EMPLOYEE, row.getEmployeeId(),
                "name", row.getName()))
            .toList();
        long nextCursor = rows.isEmpty() ? cursor : rows.get(rows.size() - 1).getEmployeeId();
        return new EmployeeI18nBatch(rows.size(), nextCursor, sources);
    }

    /**
     * 人员姓名扫描批次。
     *
     * @param resourceCount 扫描人员数
     * @param nextCursor 下一批游标
     * @param sources 非空姓名翻译源
     */
    public record EmployeeI18nBatch(int resourceCount, long nextCursor, List<I18nTextSource> sources) {
    }
}
