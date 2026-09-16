package com.ym.agriculture.farmtask.workorder.support;

import cn.hutool.core.collection.CollUtil;
import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.farmtask.employee.service.IEmployeeAppRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * stask 业务角色名称批量读取器。
 */
@Component
@RequiredArgsConstructor
public class SfStaskRoleNameReader {

    private final com.ym.agriculture.shared.i18n.StaskMessageResolver messages;
    private final IEmployeeAppRoleService employeeAppRoleService;

    /**
     * 一次读取结果集需要的角色名称。
     *
     * @param roleCodes 角色编码集合
     * @return 角色编码到显示名称的索引
     */
    public Map<String, String> load(Collection<String> roleCodes) {
        if (CollUtil.isEmpty(roleCodes)) {
            return Map.of();
        }
        Map<String, String> names = employeeAppRoleService.getRoleNameMap(roleCodes.stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList());
        return names == null ? Map.of() : names;
    }

    /**
     * 从批量索引解析角色名称，未配置时使用兼容文案。
     *
     * @param roleNames 角色名称索引
     * @param roleCode 角色编码
     * @return 角色显示名称
     */
    public String resolve(Map<String, String> roleNames, String roleCode) {
        if (StringUtils.isBlank(roleCode)) {
            return null;
        }
        return SfStaskWorkOrderAssembler.localizedRoleName(roleCode, roleNames.get(roleCode), messages);
    }
}
