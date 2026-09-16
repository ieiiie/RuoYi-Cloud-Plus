package com.ym.agriculture.farmtask.workorder.support.workbench;

import cn.hutool.core.collection.CollUtil;
import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.farmtask.workorder.support.SfStaskWorkOrderAssembler;
import com.ym.agriculture.farmtask.employee.service.IEmployeeAppRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * 批量读取工作台所需的业务角色显示名称。
 */
@Component
@RequiredArgsConstructor
public class SfStaskWorkbenchRoleNameReader {

    private final com.ym.agriculture.shared.i18n.StaskMessageResolver messages;
    private final IEmployeeAppRoleService employeeAppRoleService;

    /**
     * 一次读取当前结果集包含的角色名称。
     *
     * @param roleCodes 角色编码集合
     * @return 角色编码到名称的映射
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
     * 从已加载索引解析显示名称，未配置时使用固定兼容文案。
     *
     * @param roleNames 角色名称索引
     * @param roleCode  角色编码
     * @return 角色显示名称
     */
    public String resolve(Map<String, String> roleNames, String roleCode) {
        if (StringUtils.isBlank(roleCode)) {
            return null;
        }
        return SfStaskWorkOrderAssembler.localizedRoleName(roleCode, roleNames.get(roleCode), messages);
    }
}
