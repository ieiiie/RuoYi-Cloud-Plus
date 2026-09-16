package com.ym.system.saas.service;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.RequiredArgsConstructor;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.mybatis.core.query.QueryBuilder;
import com.ym.common.mybatis.utils.IdGeneratorUtil;
import com.ym.system.saas.domain.*;
import com.ym.system.saas.domain.bo.SaasTenantBo;
import com.ym.system.saas.mapper.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 新租户初始化、套餐收敛和角色模板下发。
 * 所有 Mapper 都绑定 saas 数据源，不访问 ry_dbo，也不跨库 JOIN。
 */
@Service
@DS("saas")
@RequiredArgsConstructor
public class SaasTenantProvisionService {
    public static final long TENANT_ADMIN_TEMPLATE_ID = 1762200000000000001L;

    private final SaasTenantMapper tenantMapper;
    private final SaasTenantPackageMapper packageMapper;
    private final SaasOssConfigMapper ossConfigMapper;
    private final SaasAppMapper appMapper;
    private final SaasMenuMapper menuMapper;
    private final SaasConfigDefinitionMapper definitionMapper;
    private final SaasRoleTemplateMapper templateMapper;
    private final SaasPackageAppMapper packageAppMapper;
    private final SaasPackageMenuMapper packageMenuMapper;
    private final SaasRoleTemplateMenuMapper templateMenuMapper;
    private final SaasRoleMapper roleMapper;
    private final SaasRoleMenuMapper roleMenuMapper;
    private final SaasRoleDeptMapper roleDeptMapper;
    private final SaasUserRoleMapper userRoleMapper;
    private final SaasTenantConfigMapper configMapper;
    private final SaasTenantDictDefaultDataMapper tenantDictDefaultDataMapper;
    private final SaasTenantDictDataMapper tenantDictDataMapper;
    private final SaasGlobalUserMapper globalUserMapper;
    private final SaasTenantUserMapper userMapper;
    private final SaasDeptMapper deptMapper;

    /**
     * 创建租户及完整的管理员、参数和内置角色数据。
     */
    public Long createTenant(SaasTenantBo bo) {
        SaasTenantPackage tenantPackage = packageMapper.selectById(bo.getPackageId());
        if (tenantPackage == null || !"0".equals(tenantPackage.getStatus())) {
            throw new ServiceException("套餐不存在或已停用");
        }
        if (ossConfigMapper.selectById(bo.getOssConfigId()) == null) {
            throw new ServiceException("OSS配置不存在");
        }
        String tenantId = generateTenantId();
        long tenantPk = IdGeneratorUtil.nextLongId();
        long deptId = IdGeneratorUtil.nextLongId();
        long roleId = IdGeneratorUtil.nextLongId();
        long userId = IdGeneratorUtil.nextLongId();

        SaasTenant tenant = new SaasTenant();
        tenant.setId(tenantPk);
        tenant.setTenantId(tenantId);
        tenant.setContactUserName(bo.getContactUserName());
        tenant.setContactPhone(bo.getContactPhone());
        tenant.setCompanyName(bo.getCompanyName());
        tenant.setLogoUrl(bo.getLogoUrl());
        tenant.setLicenseNumber(bo.getLicenseNumber());
        tenant.setAddress(bo.getAddress());
        tenant.setProvinceCode(bo.getProvinceCode());
        tenant.setCityCode(bo.getCityCode());
        tenant.setDistrictCode(bo.getDistrictCode());
        tenant.setRegionName(bo.getRegionName());
        tenant.setDomain(bo.getDomain());
        tenant.setIntro(bo.getIntro());
        tenant.setPackageId(bo.getPackageId());
        tenant.setOssConfigId(bo.getOssConfigId());
        tenant.setExpireTime(bo.getExpireTime());
        tenant.setAccountCount(bo.getAccountCount() == null ? -1L : bo.getAccountCount());
        tenant.setStatus(bo.getStatus() == null ? "0" : bo.getStatus());
        tenant.setDelFlag("0");
        tenant.setRemark(bo.getRemark());
        tenantMapper.insert(tenant);

        SaasDept dept = new SaasDept();
        dept.setDeptId(deptId);
        dept.setTenantId(tenantId);
        dept.setParentId(0L);
        dept.setAncestors("0");
        dept.setDeptName(bo.getCompanyName());
        dept.setOrderNum(0);
        dept.setPhone(bo.getContactPhone());
        dept.setStatus("0");
        dept.setDelFlag("0");
        deptMapper.insert(dept);

        SaasGlobalUser globalUser = findOrCreateGlobalUser(bo);
        SaasTenantUser user = new SaasTenantUser();
        user.setUserId(userId);
        user.setTenantId(tenantId);
        user.setGlobalUserId(globalUser.getGlobalUserId());
        user.setDeptId(deptId);
        user.setNickName(globalUser.getNickName());
        user.setUserType(globalUser.getUserType());
        user.setEmail(globalUser.getEmail() == null ? "" : globalUser.getEmail());
        user.setGender(globalUser.getGender());
        user.setStatus("0");
        user.setDelFlag("0");
        user.setCreateDept(deptId);
        user.setRemark("租户管理员");
        userMapper.insert(user);
        dept.setLeader(userId);
        deptMapper.updateById(dept);

        SaasRole role = new SaasRole();
        role.setRoleId(roleId);
        role.setTenantId(tenantId);
        role.setTemplateId(TENANT_ADMIN_TEMPLATE_ID);
        role.setTemplateVersion(1);
        role.setIsBuiltin(true);
        role.setTenantDeletable(false);
        role.setRoleName("租户管理员");
        role.setRoleKey("tenant_admin");
        role.setRoleSort(1);
        role.setDataScope("1");
        role.setMenuCheckStrictly(true);
        role.setDeptCheckStrictly(true);
        role.setStatus("0");
        role.setDelFlag("0");
        role.setCreateDept(deptId);
        role.setRemark("租户管理员内置角色");
        roleMapper.insert(role);
        userRoleMapper.insert(new SaasUserRole(userId, roleId));
        roleDeptMapper.insert(new SaasRoleDept(roleId, deptId));
        replaceRoleMenus(roleId, packageMenuIds(bo.getPackageId()));

        issueDefinitionsForTenant(tenantId, bo.getPackageId(), deptId, userId);
        issueTenantDictDefaults(tenantId, deptId, userId);
        for (Long templateId : enabledPackageTemplateIds(bo.getPackageId())) {
            syncTemplateToTenant(requireTemplate(templateId), tenantId, false);
        }
        return tenantPk;
    }

    /**
     * 单租户更换套餐后同步角色菜单；管理员获得套餐边界，普通角色只收缩已有授权。
     * 由租户更新事务调用，不下发角色模板，避免给普通角色自动增加权限。
     */
    public void synchronizeTenantRoleMenus(String tenantId, Long packageId) {
        if (tenantId == null || tenantId.isBlank() || packageId == null) {
            throw new ServiceException("租户和套餐不能为空");
        }
        List<Long> allowedMenus = packageMenuIds(packageId);
        Set<Long> allowedMenuSet = new HashSet<>(allowedMenus);
        List<SaasRole> roles = roleMapper.selectList(QueryBuilder.lambda(SaasRole.class)
            .eq(SaasRole::getTenantId, tenantId).build());
        for (SaasRole role : roles) {
            if ("tenant_admin".equals(role.getRoleKey())
                || Objects.equals(role.getTemplateId(), TENANT_ADMIN_TEMPLATE_ID)) {
                replaceRoleMenus(role.getRoleId(), allowedMenus);
            } else {
                pruneRoleMenus(role.getRoleId(), allowedMenuSet);
            }
        }
    }

    /**
     * 套餐修改后收敛其全部存量租户。
     */
    public List<String> reconcilePackage(Long packageId, Collection<Long> currentApps, Collection<Long> oldApps) {
        List<SaasTenant> tenants = tenantMapper.lambda().eq(SaasTenant::getPackageId, packageId).list();
        if (tenants.isEmpty()) return List.of();
        Set<Long> removedApps = new HashSet<>(oldApps);
        removedApps.removeAll(currentApps);
        Set<Long> removedTemplateIds = removedApps.isEmpty() ? Set.of() : templateMapper.lambda()
            .in(SaasRoleTemplate::getAppId, removedApps).list().stream().map(SaasRoleTemplate::getTemplateId).collect(Collectors.toSet());
        List<Long> allowedMenus = packageMenuIds(packageId);
        Set<Long> allowedMenuSet = new HashSet<>(allowedMenus);
        List<Long> enabledTemplates = enabledPackageTemplateIds(packageId);
        List<String> tenantIds = new ArrayList<>();
        for (SaasTenant tenant : tenants) {
            tenantIds.add(tenant.getTenantId());
            issueDefinitionsForTenant(tenant.getTenantId(), packageId, null, null);
            List<SaasRole> roles = roleMapper.selectList(QueryBuilder.lambda(SaasRole.class)
                .eq(SaasRole::getTenantId, tenant.getTenantId()).build());
            for (SaasRole role : roles) {
                if (role.getTemplateId() != null
                    && removedTemplateIds.contains(role.getTemplateId())
                    && Boolean.TRUE.equals(role.getIsBuiltin())) {
                    role.setStatus("1");
                    roleMapper.updateById(role);
                }
                pruneRoleMenus(role.getRoleId(), allowedMenuSet);
                if (Objects.equals(role.getTemplateId(), TENANT_ADMIN_TEMPLATE_ID))
                    replaceRoleMenus(role.getRoleId(), allowedMenus);
            }
            for (Long templateId : enabledTemplates)
                syncTemplateToTenant(requireTemplate(templateId), tenant.getTenantId(), false);
        }
        return tenantIds;
    }

    /**
     * 新参数定义按应用授权范围补齐到租户。
     */
    public void issueDefinition(Long definitionId) {
        SaasConfigDefinition definition = definitionMapper.selectById(definitionId);
        if (definition == null) throw new ServiceException("参数定义不存在");
        // 停用定义不向租户发放，重新启用后再按当前范围补发。
        if (!"0".equals(definition.getStatus())) return;
        List<SaasTenant> tenants;
        if (definition.getAppId() == null) {
            tenants = tenantMapper.selectList();
        } else {
            List<Long> packageIds = packageMapper.selectList().stream()
                .map(SaasTenantPackage::getPackageId)
                .filter(packageId -> packageEffectiveAppIds(packageId).contains(definition.getAppId()))
                .toList();
            tenants = packageIds.isEmpty() ? List.of() : tenantMapper.lambda().in(SaasTenant::getPackageId, packageIds).list();
        }
        tenants.forEach(tenant -> insertConfigValue(tenant.getTenantId(), definition, null, null));
        definition.setIssuedFlag(true);
        definitionMapper.updateById(definition);
    }

    /**
     * 手动同步角色模板，保留角色名称与用户绑定。
     */
    public void syncRoleTemplate(Long templateId, Collection<String> tenantIds) {
        if (tenantIds == null || tenantIds.isEmpty()) throw new ServiceException("请选择需要同步的租户");
        SaasRoleTemplate template = requireTemplate(templateId);
        if (template.getAppId() != null) {
            for (String tenantId : tenantIds) {
                SaasTenant tenant = requireTenant(tenantId);
                boolean opened = packageAppMapper.selectCount(QueryBuilder.lambda(SaasPackageApp.class)
                    .eq(SaasPackageApp::getPackageId, tenant.getPackageId()).eq(SaasPackageApp::getAppId, template.getAppId()).build()) > 0;
                if (!opened) throw new ServiceException("租户" + tenantId + "尚未开通模板所属应用");
            }
        }
        tenantIds.forEach(tenantId -> syncTemplateToTenant(template, tenantId, true));
    }

    private void issueDefinitionsForTenant(String tenantId, Long packageId, Long deptId, Long userId) {
        Set<Long> appIds = packageEffectiveAppIds(packageId);
        List<SaasConfigDefinition> definitions = definitionMapper.lambda().eq(SaasConfigDefinition::getStatus, "0").list();
        definitions.stream().filter(d -> d.getAppId() == null || appIds.contains(d.getAppId()))
            .forEach(d -> insertConfigValue(tenantId, d, deptId, userId));
    }

    /** 套餐直接应用和组合应用实际使用的源微应用共同构成功能依赖。 */
    private Set<Long> packageEffectiveAppIds(Long packageId) {
        Set<Long> appIds = packageAppMapper.selectList(QueryBuilder.lambda(SaasPackageApp.class)
                .eq(SaasPackageApp::getPackageId, packageId).build()).stream()
            .map(SaasPackageApp::getAppId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        List<Long> menuIds = packageMenuIds(packageId);
        if (!menuIds.isEmpty()) {
            menuMapper.selectList(QueryBuilder.lambda(SaasMenu.class)
                    .inIfNotEmpty(SaasMenu::getMenuId, menuIds).build()).stream()
                .map(SaasMenu::getAppId)
                .forEach(appIds::add);
        }
        return appIds;
    }

    private void insertConfigValue(String tenantId, SaasConfigDefinition definition, Long deptId, Long userId) {
        long count = configMapper.selectCount(QueryBuilder.lambda(SaasTenantConfig.class)
            .eq(SaasTenantConfig::getTenantId, tenantId).eq(SaasTenantConfig::getDefinitionId, definition.getDefinitionId()).build());
        if (count > 0) return;
        SaasTenantConfig config = new SaasTenantConfig();
        config.setConfigId(IdGeneratorUtil.nextLongId());
        config.setTenantId(tenantId);
        config.setDefinitionId(definition.getDefinitionId());
        config.setConfigName(definition.getConfigName());
        config.setConfigKey(definition.getConfigKey());
        config.setConfigValue(definition.getDefaultValue());
        config.setConfigType("Y");
        config.setCreateDept(deptId);
        config.setCreateBy(userId);
        config.setRemark(definition.getRemark());
        configMapper.insert(config);
    }

    /** 将当前默认模板复制为新租户可独立维护的字典值。 */
    private void issueTenantDictDefaults(String tenantId, Long deptId, Long userId) {
        List<SaasTenantDictDefaultData> defaults = tenantDictDefaultDataMapper.selectList(
            QueryBuilder.lambda(SaasTenantDictDefaultData.class)
                .orderByAsc(SaasTenantDictDefaultData::getDictType)
                .orderByAsc(SaasTenantDictDefaultData::getDictSort)
                .orderByAsc(SaasTenantDictDefaultData::getDictCode).build());
        for (SaasTenantDictDefaultData template : defaults) {
            SaasTenantDictData data = new SaasTenantDictData();
            data.setDictCode(IdGeneratorUtil.nextLongId());
            data.setTenantId(tenantId);
            data.setDictSort(template.getDictSort());
            data.setDictLabel(template.getDictLabel());
            data.setDictValue(template.getDictValue());
            data.setDictType(template.getDictType());
            data.setCssClass(template.getCssClass());
            data.setListClass(template.getListClass());
            data.setIsDefault(template.getIsDefault());
            data.setCreateDept(deptId);
            data.setCreateBy(userId);
            data.setRemark(template.getRemark());
            tenantDictDataMapper.insert(data);
        }
    }

    private void syncTemplateToTenant(SaasRoleTemplate template, String tenantId, boolean restoreDeleted) {
        SaasRole role = roleMapper.selectTemplateRoleIncludingDeleted(tenantId, template.getTemplateId());
        if (role != null && "1".equals(role.getDelFlag()) && !restoreDeleted) {
            return;
        }
        if (role == null) {
            role = new SaasRole();
            role.setRoleId(IdGeneratorUtil.nextLongId());
            role.setTenantId(tenantId);
            role.setTemplateId(template.getTemplateId());
            role.setIsBuiltin(true);
            role.setRoleName(template.getTemplateName());
            role.setMenuCheckStrictly(true);
            role.setDeptCheckStrictly(true);
            role.setDelFlag("0");
            role.setRemark("由Dbo角色模板创建");
            applyTemplate(role, template);
            roleMapper.insert(role);
        } else if ("1".equals(role.getDelFlag())) {
            applyTemplate(role, template);
            roleMapper.restoreTemplateRole(role);
        } else {
            applyTemplate(role, template);
            roleMapper.updateById(role);
        }
        List<Long> menuIds = template.getAppId() == null && TENANT_ADMIN_TEMPLATE_ID == template.getTemplateId()
            ? packageMenuIds(requireTenant(tenantId).getPackageId())
            : templateMenuIds(template.getTemplateId());
        replaceRoleMenus(role.getRoleId(), menuIds);
    }

    private void applyTemplate(SaasRole role, SaasRoleTemplate template) {
        role.setRoleKey(template.getRoleKey());
        role.setRoleSort(template.getRoleSort());
        role.setDataScope(template.getDataScope());
        role.setStatus(template.getStatus());
        role.setTemplateVersion(template.getTemplateVersion());
        role.setTenantDeletable(Boolean.TRUE.equals(template.getTenantDeletable()));
    }

    private void replaceRoleMenus(Long roleId, Collection<Long> menuIds) {
        roleMenuMapper.delete(QueryBuilder.lambda(SaasRoleMenu.class).eq(SaasRoleMenu::getRoleId, roleId).build());
        menuIds.forEach(menuId -> roleMenuMapper.insert(new SaasRoleMenu(roleId, menuId)));
    }

    private void pruneRoleMenus(Long roleId, Set<Long> allowedMenuIds) {
        var query = QueryBuilder.lambda(SaasRoleMenu.class).eq(SaasRoleMenu::getRoleId, roleId);
        if (!allowedMenuIds.isEmpty()) {
            query.notIn(SaasRoleMenu::getMenuId, allowedMenuIds);
        }
        roleMenuMapper.delete(query.build());
    }

    private SaasGlobalUser findOrCreateGlobalUser(SaasTenantBo bo) {
        SaasGlobalUser existing = globalUserMapper.selectOne(QueryBuilder.lambda(SaasGlobalUser.class)
            .eq(SaasGlobalUser::getUserName, bo.getUsername()).build());
        if (existing != null) {
            if (!Objects.equals(existing.getPhoneNumber(), bo.getAdminPhone())) {
                throw new ServiceException("管理员账号已存在且手机号不一致");
            }
            return existing;
        }
        if (globalUserMapper.selectCount(QueryBuilder.lambda(SaasGlobalUser.class)
            .eq(SaasGlobalUser::getPhoneNumber, bo.getAdminPhone()).build()) > 0) {
            throw new ServiceException("手机号已属于其他全局账号");
        }
        SaasGlobalUser user = new SaasGlobalUser();
        user.setGlobalUserId(IdGeneratorUtil.nextLongId());
        user.setUserName(bo.getUsername());
        user.setNickName(bo.getContactUserName());
        user.setUserType("sys_user");
        user.setEmail("");
        user.setPhoneNumber(bo.getAdminPhone());
        user.setGender("0");
        user.setPassword(BCrypt.hashpw(bo.getPassword()));
        user.setStatus("0");
        user.setDelFlag("0");
        globalUserMapper.insert(user);
        return user;
    }

    private List<Long> packageMenuIds(Long packageId) {
        return packageMenuMapper.selectList(QueryBuilder.lambda(SaasPackageMenu.class)
            .eq(SaasPackageMenu::getPackageId, packageId).build()).stream().map(SaasPackageMenu::getMenuId).toList();
    }

    private List<Long> templateMenuIds(Long templateId) {
        return templateMenuMapper.selectList(QueryBuilder.lambda(SaasRoleTemplateMenu.class)
            .eq(SaasRoleTemplateMenu::getTemplateId, templateId).build()).stream().map(SaasRoleTemplateMenu::getMenuId).toList();
    }

    private List<Long> enabledPackageTemplateIds(Long packageId) {
        List<Long> appIds = packageAppMapper.selectList(QueryBuilder.lambda(SaasPackageApp.class)
            .eq(SaasPackageApp::getPackageId, packageId).build()).stream().map(SaasPackageApp::getAppId).toList();
        return appIds.isEmpty() ? List.of() : templateMapper.lambda().in(SaasRoleTemplate::getAppId, appIds)
            .eq(SaasRoleTemplate::getStatus, "0").list().stream().map(SaasRoleTemplate::getTemplateId).toList();
    }

    private SaasRoleTemplate requireTemplate(Long id) {
        SaasRoleTemplate value = templateMapper.selectById(id);
        if (value == null) throw new ServiceException("角色模板不存在");
        return value;
    }

    private SaasTenant requireTenant(String tenantId) {
        SaasTenant value = tenantMapper.lambda().eq(SaasTenant::getTenantId, tenantId).one();
        if (value == null) throw new ServiceException("租户不存在");
        return value;
    }

    private String generateTenantId() {
        for (int i = 0; i < 100; i++) {
            String tenantId = RandomUtil.randomNumbers(6);
            if (!tenantMapper.lambda().eq(SaasTenant::getTenantId, tenantId).exists()) return tenantId;
        }
        throw new ServiceException("租户编号生成失败");
    }
}
