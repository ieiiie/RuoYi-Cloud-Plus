package org.dromara.system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.CacheNames;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.enums.UserType;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.common.redis.utils.CacheUtils;
import org.dromara.system.domain.SysGlobalUser;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.bo.SysGlobalUserBo;
import org.dromara.system.domain.bo.SysUserProfileBo;
import org.dromara.system.domain.vo.SysGlobalUserVo;
import org.dromara.system.mapper.SysGlobalUserMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.service.ISysGlobalUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局账号服务实现。
 */
@RequiredArgsConstructor
@Service
public class SysGlobalUserServiceImpl implements ISysGlobalUserService {

    private final SysGlobalUserMapper globalUserMapper;
    private final SysUserMapper userMapper;

    @Override
    public SysGlobalUser queryById(Long globalUserId) {
        if (ObjectUtil.isNull(globalUserId)) {
            return null;
        }
        return TenantHelper.ignore(() -> globalUserMapper.selectById(globalUserId));
    }

    @Override
    public SysGlobalUserVo queryVoById(Long globalUserId) {
        if (ObjectUtil.isNull(globalUserId)) {
            return null;
        }
        return TenantHelper.ignore(() -> globalUserMapper.selectVoById(globalUserId));
    }

    @Override
    public SysGlobalUser queryByIdentifier(String identifier) {
        if (StringUtils.isBlank(identifier)) {
            return null;
        }
        return TenantHelper.ignore(() -> {
            List<SysGlobalUser> users = globalUserMapper.lambda()
                .and(w -> w.eq(SysGlobalUser::getUserName, identifier)
                    .or().eq(SysGlobalUser::getPhoneNumber, identifier)
                    .or().eq(SysGlobalUser::getEmail, identifier))
                .list();
            if (users.isEmpty()) {
                return null;
            }
            if (users.size() > 1) {
                throw new ServiceException("全局账号标识命中多个账号，请联系平台管理员处理");
            }
            return users.getFirst();
        });
    }

    @Override
    public List<SysGlobalUserVo> queryList(SysGlobalUserBo bo) {
        return TenantHelper.ignore(() -> globalUserMapper.lambda()
            .likeIfText(SysGlobalUser::getUserName, bo.getUserName())
            .likeIfText(SysGlobalUser::getNickName, bo.getNickName())
            .eqIfText(SysGlobalUser::getPhoneNumber, bo.getPhoneNumber())
            .eqIfText(SysGlobalUser::getEmail, bo.getEmail())
            .eqIfText(SysGlobalUser::getStatus, bo.getStatus())
            .orderByAsc(SysGlobalUser::getGlobalUserId)
            .voList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysGlobalUser resolveOrCreate(SysGlobalUser candidate) {
        List<SysGlobalUser> matches = findMatches(candidate);
        if (matches.isEmpty()) {
            if (StringUtils.isBlank(candidate.getPassword())) {
                throw new ServiceException("新增全局账号时密码不能为空");
            }
            SysGlobalUser globalUser = new SysGlobalUser();
            globalUser.setUserName(candidate.getUserName());
            globalUser.setNickName(StringUtils.blankToDefault(candidate.getNickName(), candidate.getUserName()));
            globalUser.setUserType(StringUtils.blankToDefault(candidate.getUserType(), UserType.SYS_USER.getUserType()));
            globalUser.setEmail(blankToNull(candidate.getEmail()));
            globalUser.setPhoneNumber(blankToNull(candidate.getPhoneNumber()));
            globalUser.setGender(candidate.getGender());
            globalUser.setAvatar(candidate.getAvatar());
            globalUser.setPassword(candidate.getPassword());
            globalUser.setStatus(SystemConstants.NORMAL);
            if (globalUserMapper.insert(globalUser) < 1) {
                throw new ServiceException("创建全局账号失败");
            }
            return globalUser;
        }
        if (matches.size() > 1) {
            throw new ServiceException("用户名、手机号或邮箱属于不同的全局账号");
        }
        SysGlobalUser globalUser = matches.getFirst();
        validateExistingProfile(globalUser, candidate);
        return globalUser;
    }

    @Override
    public void applyToTenantUser(SysGlobalUser globalUser, SysUser tenantUser) {
        tenantUser.setGlobalUserId(globalUser.getGlobalUserId());
        tenantUser.setNickName(globalUser.getNickName());
        tenantUser.setUserType(globalUser.getUserType());
        tenantUser.setEmail(globalUser.getEmail());
        tenantUser.setGender(globalUser.getGender());
        tenantUser.setAvatar(globalUser.getAvatar());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateGlobalUser(SysGlobalUserBo bo) {
        SysGlobalUser current = requireUser(bo.getGlobalUserId());
        validateUpdateIdentifiers(current, bo);
        SysGlobalUser update = MapstructUtils.convert(bo, SysGlobalUser.class);
        update.setGlobalUserId(current.getGlobalUserId());
        int rows = TenantHelper.ignore(() -> globalUserMapper.updateById(update));
        if (rows > 0) {
            synchronizeTenantUsers(requireUser(current.getGlobalUserId()));
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateProfile(Long globalUserId, SysUserProfileBo profile) {
        SysGlobalUser current = requireUser(globalUserId);
        if (StringUtils.isNotBlank(profile.getUserName())
            && !StringUtils.equals(profile.getUserName(), current.getUserName())) {
            ensureIdentifierAvailable(profile.getUserName(), current.getGlobalUserId(), "用户名");
        }
        if (StringUtils.isNotBlank(profile.getPhoneNumber())
            && !StringUtils.equals(profile.getPhoneNumber(), current.getPhoneNumber())) {
            ensureIdentifierAvailable(profile.getPhoneNumber(), current.getGlobalUserId(), "手机号");
        }
        if (StringUtils.isNotBlank(profile.getEmail())
            && !StringUtils.equals(profile.getEmail(), current.getEmail())) {
            ensureIdentifierAvailable(profile.getEmail(), current.getGlobalUserId(), "邮箱");
        }
        SysGlobalUser update = new SysGlobalUser();
        update.setGlobalUserId(globalUserId);
        update.setUserName(profile.getUserName());
        update.setNickName(profile.getNickName());
        update.setPhoneNumber(blankToNull(profile.getPhoneNumber()));
        update.setEmail(blankToNull(profile.getEmail()));
        update.setGender(profile.getGender());
        update.setAvatar(profile.getAvatar());
        int rows = TenantHelper.ignore(() -> globalUserMapper.updateById(update));
        if (rows > 0) {
            synchronizeTenantUsers(requireUser(globalUserId));
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int resetPassword(Long globalUserId, String password) {
        requireUser(globalUserId);
        SysGlobalUser update = new SysGlobalUser();
        update.setGlobalUserId(globalUserId);
        update.setPassword(password);
        int rows = TenantHelper.ignore(() -> globalUserMapper.updateById(update));
        if (rows > 0) {
            synchronizeTenantUsers(requireUser(globalUserId));
        }
        return rows;
    }

    private List<SysGlobalUser> findMatches(SysGlobalUser candidate) {
        return TenantHelper.ignore(() -> {
            Map<Long, SysGlobalUser> matches = new LinkedHashMap<>();
            collectMatch(matches, candidate.getUserName());
            collectMatch(matches, candidate.getPhoneNumber());
            collectMatch(matches, candidate.getEmail());
            return List.copyOf(matches.values());
        });
    }

    private void collectMatch(Map<Long, SysGlobalUser> matches, String identifier) {
        if (StringUtils.isBlank(identifier)) {
            return;
        }
        List<SysGlobalUser> users = globalUserMapper.lambda()
            .and(w -> w.eq(SysGlobalUser::getUserName, identifier)
                .or().eq(SysGlobalUser::getPhoneNumber, identifier)
                .or().eq(SysGlobalUser::getEmail, identifier))
            .list();
        for (SysGlobalUser user : users) {
            matches.put(user.getGlobalUserId(), user);
        }
    }

    private void validateExistingProfile(SysGlobalUser globalUser, SysGlobalUser candidate) {
        assertSameWhenPresent("用户名", candidate.getUserName(), globalUser.getUserName());
        assertSameWhenPresent("手机号", candidate.getPhoneNumber(), globalUser.getPhoneNumber());
        assertSameWhenPresent("邮箱", candidate.getEmail(), globalUser.getEmail());
        assertSameWhenPresent("昵称", candidate.getNickName(), globalUser.getNickName());
    }

    private void validateUpdateIdentifiers(SysGlobalUser current, SysGlobalUserBo bo) {
        if (StringUtils.isNotBlank(bo.getUserName()) && !StringUtils.equals(bo.getUserName(), current.getUserName())) {
            ensureIdentifierAvailable(bo.getUserName(), current.getGlobalUserId(), "用户名");
        }
        if (StringUtils.isNotBlank(bo.getPhoneNumber()) && !StringUtils.equals(bo.getPhoneNumber(), current.getPhoneNumber())) {
            ensureIdentifierAvailable(bo.getPhoneNumber(), current.getGlobalUserId(), "手机号");
        }
        if (StringUtils.isNotBlank(bo.getEmail()) && !StringUtils.equals(bo.getEmail(), current.getEmail())) {
            ensureIdentifierAvailable(bo.getEmail(), current.getGlobalUserId(), "邮箱");
        }
    }

    private void ensureIdentifierAvailable(String identifier, Long currentId, String name) {
        SysGlobalUser exists = queryByIdentifier(identifier);
        if (ObjectUtil.isNotNull(exists) && !ObjectUtil.equals(exists.getGlobalUserId(), currentId)) {
            throw new ServiceException(name + "已被其他全局账号使用");
        }
    }

    private void assertSameWhenPresent(String fieldName, String candidate, String actual) {
        if (StringUtils.isNotBlank(candidate) && !StringUtils.equals(candidate, actual)) {
            throw new ServiceException(fieldName + "与已存在的全局账号资料不一致");
        }
    }

    private SysGlobalUser requireUser(Long globalUserId) {
        SysGlobalUser globalUser = queryById(globalUserId);
        if (ObjectUtil.isNull(globalUser)) {
            throw new ServiceException("全局账号不存在");
        }
        return globalUser;
    }

    private String blankToNull(String value) {
        return StringUtils.isBlank(value) ? null : value;
    }

    /**
     * 可展示的全局资料是本地用户的只读镜像，更新时必须跨租户同步。
     * 用户名、手机号和密码不落入 sys_user，始终从 sys_global_user 读取。
     */
    private void synchronizeTenantUsers(SysGlobalUser globalUser) {
        List<SysUser> tenantUsers = TenantHelper.ignore(() -> userMapper.lambda()
            .select(SysUser::getUserId, SysUser::getTenantId)
            .eq(SysUser::getGlobalUserId, globalUser.getGlobalUserId())
            .list());
        TenantHelper.ignore(() -> userMapper.lambda()
            .set(SysUser::getNickName, globalUser.getNickName())
            .set(SysUser::getUserType, globalUser.getUserType())
            .set(SysUser::getEmail, globalUser.getEmail())
            .set(SysUser::getGender, globalUser.getGender())
            .set(SysUser::getAvatar, globalUser.getAvatar())
            .eq(SysUser::getGlobalUserId, globalUser.getGlobalUserId())
            .update());
        // 用户名、昵称缓存按租户前缀保存，需逐个成员租户清理，避免同步后仍显示旧资料。
        for (SysUser tenantUser : tenantUsers) {
            TenantHelper.dynamic(tenantUser.getTenantId(), () -> {
                CacheUtils.evict(CacheNames.SYS_USER_NAME, tenantUser.getUserId());
                CacheUtils.evict(CacheNames.SYS_NICKNAME, tenantUser.getUserId());
            });
        }
    }
}
