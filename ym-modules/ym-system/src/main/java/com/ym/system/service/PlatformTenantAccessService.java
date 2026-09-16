package com.ym.system.service;

import com.baomidou.lock.annotation.Lock4j;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.system.api.domain.vo.RemoteTenantUserVo;
import com.ym.system.domain.SysGlobalUser;
import com.ym.system.domain.SysUser;
import com.ym.system.domain.bo.SysTenantBo;
import com.ym.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** 平台管理员跨租户入口；普通成员不能调用此通道建立成员关系。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformTenantAccessService {
    private final ISysTenantService tenantService;
    private final SysUserMapper userMapper;

    /** 查询所有有效租户，列表查询不创建成员或授权关系。 */
    public List<RemoteTenantUserVo> listTenants(SysGlobalUser user) {
        checkAdministrator(user);
        List<RemoteTenantUserVo> result = new ArrayList<>();
        for (var tenant : tenantService.queryList(new SysTenantBo())) {
            try {
                tenantService.checkTenantAvailable(tenant.getTenantId());
                RemoteTenantUserVo item = new RemoteTenantUserVo();
                item.setTenantId(tenant.getTenantId());
                item.setTenantName(tenant.getCompanyName());
                item.setLogoUrl(tenant.getLogoUrl());
                // 未进入过的租户没有本地成员，首次切换后返回实际成员 ID。
                result.add(item);
            } catch (ServiceException ignored) {
                // 停用、过期和删除租户不能建立会话。
            }
        }
        return result;
    }

    /** 为审计保留真实本地成员，不借用其他人的用户 ID，不写角色授权表。 */
    @Lock4j(name = "platform-tenant-member", keys = {"#user.globalUserId", "#tenantId"})
    @Transactional(rollbackFor = Exception.class)
    public SysUser enterTenant(SysGlobalUser user, String tenantId) {
        checkAdministrator(user);
        tenantService.checkTenantAvailable(tenantId);
        return TenantHelper.dynamic(tenantId, () -> {
            SysUser member = userMapper.lambda().eq(SysUser::getGlobalUserId, user.getGlobalUserId()).one();
            if (member == null) {
                member = new SysUser();
                member.setGlobalUserId(user.getGlobalUserId());
                member.setTenantId(tenantId);
                member.setNickName(user.getNickName());
                member.setUserType(user.getUserType());
                member.setStatus(SystemConstants.NORMAL);
                member.setDelFlag("0");
                member.setRemark("平台超级管理员跨租户操作成员；权限由全局身份计算");
                userMapper.insert(member);
            }
            if (!SystemConstants.NORMAL.equals(member.getStatus())) {
                throw new ServiceException("平台管理员在目标租户的成员已停用，请先恢复成员状态");
            }
            log.info("平台超级管理员进入租户: globalUserId={}, tenantId={}, memberId={}",
                user.getGlobalUserId(), tenantId, member.getUserId());
            return member;
        });
    }

    static void checkAdministrator(SysGlobalUser user) {
        if (user == null || !SystemConstants.SUPER_ADMIN_GLOBAL_USER_ID.equals(user.getGlobalUserId())
            || !SystemConstants.NORMAL.equals(user.getStatus()) || !"0".equals(user.getDelFlag())) {
            throw new ServiceException("仅有效的平台超级管理员可访问所有租户");
        }
    }
}
