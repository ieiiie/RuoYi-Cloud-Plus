package com.ym.agriculture.farmtask.employee.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.farmtask.employee.config.EmployeeAppRoleProperties;
import com.ym.agriculture.farmtask.employee.model.vo.EmployeeAppRoleVo;
import com.ym.agriculture.farmtask.employee.service.IEmployeeAppRoleService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 人员应用角色配置服务实现。
 */
@RequiredArgsConstructor
@Service
public class EmployeeAppRoleServiceImpl implements IEmployeeAppRoleService {

    private final EmployeeAppRoleProperties properties;

    private Map<String, String> roleNameByCode = Map.of();

    @PostConstruct
    void initRoleMap() {
        if (CollUtil.isEmpty(properties.getAppRoles())) {
            roleNameByCode = Map.of();
            return;
        }
        Map<String, String> map = new HashMap<>();
        Set<String> duplicateCodes = new HashSet<>();
        for (EmployeeAppRoleProperties.AppRoleItem item : properties.getAppRoles()) {
            if (item == null || StringUtils.isBlank(item.getCode())) {
                continue;
            }
            if (!map.containsKey(item.getCode())) {
                map.put(item.getCode(), item.getName());
            } else {
                duplicateCodes.add(item.getCode());
            }
        }
        if (CollUtil.isNotEmpty(duplicateCodes)) {
            throw new IllegalStateException("人员应用角色配置存在重复 code：" + duplicateCodes);
        }
        roleNameByCode = Collections.unmodifiableMap(map);
    }

    @Override
    public String getRoleName(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        return roleNameByCode.get(code);
    }

    @Override
    public void requireValidRoleCode(String code) {
        if (StringUtils.isBlank(code) || !roleNameByCode.containsKey(code)) {
            throw new ServiceException("未找到小程序角色：" + code);
        }
    }

    @Override
    public void requireInviteSelectableRoleCode(String code) {
        requireValidRoleCode(code);
        boolean selectable = properties.getAppRoles().stream()
            .filter(item -> item != null && StringUtils.equals(item.getCode(), code))
            .findFirst()
            .map(EmployeeAppRoleProperties.AppRoleItem::isInviteSelectable)
            .orElse(false);
        if (!selectable) {
            throw new ServiceException("该岗位只能由后台授予，不能通过小程序注册或岗位邀请码选择");
        }
    }

    @Override
    public List<EmployeeAppRoleVo> listRoles() {
        return properties.getAppRoles().stream()
            .filter(item -> item != null && StringUtils.isNotBlank(item.getCode()))
            .map(item -> {
                EmployeeAppRoleVo vo = new EmployeeAppRoleVo();
                vo.setCode(item.getCode());
                vo.setName(item.getName());
                vo.setInviteSelectable(item.isInviteSelectable());
                return vo;
            })
            .collect(Collectors.toList());
    }

    @Override
    public Map<String, String> getRoleNameMap(Collection<String> codes) {
        if (CollUtil.isEmpty(codes)) {
            return Map.of();
        }
        Map<String, String> result = new HashMap<>();
        for (String code : codes) {
            if (StringUtils.isBlank(code)) {
                continue;
            }
            String name = roleNameByCode.get(code);
            if (name != null) {
                result.put(code, name);
            }
        }
        return result;
    }
}
