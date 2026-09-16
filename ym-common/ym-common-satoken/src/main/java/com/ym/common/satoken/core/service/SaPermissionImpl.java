package com.ym.common.satoken.core.service;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.service.PermissionService;
import com.ym.common.core.utils.SpringUtils;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.system.api.model.LoginUser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * sa-token 权限管理实现类
 *
 * @author Lion Li
 */
public class SaPermissionImpl implements StpInterface {

    /**
     * 获取菜单权限列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return resolvePermissionList(loginId, LoginUser::getMenuPermission, PermissionService::getMenuPermission);
    }

    /**
     * 获取角色权限列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return resolvePermissionList(loginId, LoginUser::getRolePermission, PermissionService::getRolePermission);
    }

    /**
     * 解析当前登录对象的权限列表。
     *
     * @param loginId                   登录ID
     * @param localPermissionExtractor  当前登录用户权限提取器
     * @param remotePermissionExtractor 远程权限提取器
     * @return 权限列表
     */
    private List<String> resolvePermissionList(Object loginId,
                                               Function<LoginUser, Collection<String>> localPermissionExtractor,
                                               BiFunction<PermissionService, Long, Collection<String>> remotePermissionExtractor) {
        LoginUser loginUser = LoginHelper.getLoginUser();
        // 登录 ID 在首次登录时固定。租户切换仅替换当前 token session 内的
        // LoginUser，因此不能再以初始登录 ID 判断会话身份。
        if (ObjectUtil.isNull(loginUser)) {
            PermissionService permissionService = getPermissionService();
            if (ObjectUtil.isNotNull(permissionService)) {
                return new ArrayList<>(remotePermissionExtractor.apply(permissionService, resolveUserId(loginId)));
            }
            throw new ServiceException("PermissionService 实现类不存在");
        }
        Collection<String> permissionList = localPermissionExtractor.apply(loginUser);
        if (CollUtil.isNotEmpty(permissionList)) {
            return new ArrayList<>(permissionList);
        }
        return new ArrayList<>();
    }

    private PermissionService getPermissionService() {
        try {
            return SpringUtils.getBean(PermissionService.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从登录ID中提取用户ID。
     *
     * @param loginId 登录ID
     * @return 用户ID
     */
    private Long resolveUserId(Object loginId) {
        String loginIdStr = loginId.toString();
        int separatorIndex = loginIdStr.indexOf(':');
        if (separatorIndex < 0 || separatorIndex == loginIdStr.length() - 1) {
            throw new ServiceException("登录ID格式错误");
        }
        return Long.parseLong(loginIdStr.substring(separatorIndex + 1));
    }

}
