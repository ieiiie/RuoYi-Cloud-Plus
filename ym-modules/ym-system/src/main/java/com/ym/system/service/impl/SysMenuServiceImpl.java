package com.ym.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.ym.common.core.constant.Constants;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.core.utils.StreamUtils;
import com.ym.common.core.utils.TreeBuildUtils;
import com.ym.common.json.utils.JsonUtils;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.system.domain.SysApp;
import com.ym.system.domain.SysCompositeAppMenu;
import com.ym.system.helper.TenantPackageMenuScope;
import com.ym.system.mapper.SysCompositeAppMenuMapper;
import com.ym.system.domain.SysMenu;
import com.ym.system.domain.SysRole;
import com.ym.system.domain.SysRoleMenu;
import com.ym.system.domain.SysTenant;
import com.ym.system.domain.SysTenantPackageApp;
import com.ym.system.domain.SysTenantPackageMenu;
import com.ym.system.domain.bo.SysMenuBo;
import com.ym.system.domain.vo.MetaVo;
import com.ym.system.domain.vo.RouterVo;
import com.ym.system.domain.vo.SysMenuVo;
import com.ym.system.mapper.SysAppMapper;
import com.ym.system.mapper.SysMenuMapper;
import com.ym.system.mapper.SysRoleMapper;
import com.ym.system.mapper.SysRoleMenuMapper;
import com.ym.system.mapper.SysTenantMapper;
import com.ym.system.mapper.SysTenantPackageAppMapper;
import com.ym.system.mapper.SysTenantPackageMenuMapper;
import com.ym.system.service.ISysMenuService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 菜单 业务层处理
 *
 * @author Lion Li
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SysMenuServiceImpl implements ISysMenuService {

    private static final Pattern MICRO_APP_NAME_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    private final SysMenuMapper menuMapper;
    private final SysAppMapper appMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysTenantMapper tenantMapper;
    private final SysTenantPackageAppMapper packageAppMapper;
    private final SysTenantPackageMenuMapper packageMenuMapper;
    private final SysCompositeAppMenuMapper compositeAppMenuMapper;

    /**
     * 根据用户查询系统菜单列表
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    @Override
    public List<SysMenuVo> selectMenuList(Long userId) {
        return selectMenuList(new SysMenuBo(), userId);
    }

    /**
     * 查询系统菜单列表
     *
     * @param menu   菜单筛选条件
     * @param userId 当前查询的用户主键
     * @return 菜单列表
     */
    @Override
    public List<SysMenuVo> selectMenuList(SysMenuBo menu, Long userId) {
        Set<Long> packageMenuIds = selectCurrentTenantPackageMenuIds();
        if (CollUtil.isEmpty(packageMenuIds)) {
            return CollUtil.newArrayList();
        }
        // 管理员显示所有菜单信息 不是管理员 按用户id过滤菜单
        if (LoginHelper.isCurrentSuperAdmin(userId)) {
            return menuMapper.lambda()
                .in(SysMenu::getMenuId, packageMenuIds)
                .likeIfText(SysMenu::getMenuName, menu.getMenuName())
                .eqIfText(SysMenu::getVisible, menu.getVisible())
                .eqIfText(SysMenu::getStatus, menu.getStatus())
                .eqIfText(SysMenu::getMenuType, menu.getMenuType())
                .eqIfPresent(SysMenu::getParentId, menu.getParentId())
                .orderByAsc(SysMenu::getParentId)
                .orderByAsc(SysMenu::getOrderNum)
                .voList();
        }
        return menuMapper.selectMenuListByUserId(menu, userId, packageMenuIds);
    }

    @Override
    public List<SysMenuVo> selectCurrentTenantPackageMenuList() {
        Set<Long> menuIds = selectCurrentTenantPackageMenuIds();
        if (CollUtil.isEmpty(menuIds)) {
            return CollUtil.newArrayList();
        }
        return menuMapper.lambda()
            .in(SysMenu::getMenuId, menuIds)
            .orderByAsc(SysMenu::getParentId)
            .orderByAsc(SysMenu::getOrderNum)
            .voList();
    }

    @Override
    public Set<Long> selectCurrentTenantPackageMenuIds() {
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            log.warn("无法确定当前租户，拒绝返回套餐菜单");
            return Collections.emptySet();
        }
        if (LoginHelper.isSuperAdmin()) {
            Set<Long> appIds = StreamUtils.toSet(appMapper.lambda()
                .select(SysApp::getAppId).eq(SysApp::getStatus, SystemConstants.NORMAL).list(), SysApp::getAppId);
            return appIds.isEmpty() ? Collections.emptySet() : StreamUtils.toSet(menuMapper.lambda()
                .select(SysMenu::getMenuId).in(SysMenu::getAppId, appIds)
                .eq(SysMenu::getStatus, SystemConstants.NORMAL).list(), SysMenu::getMenuId);
        }
        SysTenant tenant = tenantMapper.lambda()
            .select(SysTenant::getPackageId)
            .eq(SysTenant::getTenantId, tenantId)
            .one();
        if (tenant == null || tenant.getPackageId() == null) {
            log.warn("当前租户未绑定套餐，tenantId={}", tenantId);
            return Collections.emptySet();
        }

        Set<Long> packageAppIds = StreamUtils.toSet(packageAppMapper.selectList(
            new LambdaQueryWrapper<SysTenantPackageApp>()
                .select(SysTenantPackageApp::getAppId)
                .eq(SysTenantPackageApp::getPackageId, tenant.getPackageId())), SysTenantPackageApp::getAppId);
        if (CollUtil.isEmpty(packageAppIds)) {
            return Collections.emptySet();
        }
        List<SysApp> enabledApps = appMapper.lambda()
            .in(SysApp::getAppId, packageAppIds)
            .eq(SysApp::getStatus, SystemConstants.NORMAL)
            .list();
        if (CollUtil.isEmpty(enabledApps)) {
            return Collections.emptySet();
        }

        Set<Long> packageMenuIds = StreamUtils.toSet(packageMenuMapper.selectList(
            new LambdaQueryWrapper<SysTenantPackageMenu>()
                .select(SysTenantPackageMenu::getMenuId)
                .eq(SysTenantPackageMenu::getPackageId, tenant.getPackageId())), SysTenantPackageMenu::getMenuId);
        if (CollUtil.isEmpty(packageMenuIds)) {
            return Collections.emptySet();
        }
        List<SysMenu> packageMenus = menuMapper.lambda()
            .in(SysMenu::getMenuId, packageMenuIds)
            .eq(SysMenu::getStatus, SystemConstants.NORMAL)
            .list();
        List<Long> compositeIds = enabledApps.stream()
            .filter(app -> "COMPOSITE".equals(app.getAppType())).map(SysApp::getAppId).toList();
        List<SysCompositeAppMenu> bindings = compositeIds.isEmpty() ? List.of()
            : compositeAppMenuMapper.selectList(new LambdaQueryWrapper<SysCompositeAppMenu>()
                .in(SysCompositeAppMenu::getAppId, compositeIds)
                .in(SysCompositeAppMenu::getMenuId, packageMenuIds));
        Set<Long> sourceIds = packageMenus.stream().map(SysMenu::getAppId)
            .filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        List<SysApp> sourceApps = sourceIds.isEmpty() || compositeIds.isEmpty() ? List.of()
            : appMapper.lambda().in(SysApp::getAppId, sourceIds)
                .eq(SysApp::getAppType, "MICRO").eq(SysApp::getStatus, SystemConstants.NORMAL).list();
        return TenantPackageMenuScope.resolve(enabledApps, packageMenus, bindings, sourceApps);
    }

    /**
     * 根据用户ID查询权限
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    @Override
    public Set<String> selectMenuPermsByUserId(Long userId) {
        Set<Long> menuIds = selectCurrentTenantPackageMenuIds();
        return CollUtil.isEmpty(menuIds) ? Collections.emptySet() : menuMapper.selectMenuPermsByUserId(userId, menuIds);
    }

    /**
     * 根据角色ID查询权限
     *
     * @param roleId 角色ID
     * @return 权限列表
     */
    @Override
    public Set<String> selectMenuPermsByRoleId(Long roleId) {
        Set<Long> menuIds = selectCurrentTenantPackageMenuIds();
        return CollUtil.isEmpty(menuIds) ? Collections.emptySet() : menuMapper.selectMenuPermsByRoleId(roleId, menuIds);
    }

    /**
     * 根据角色ID列表批量查询权限
     *
     * @param roleIds 角色ID列表
     * @return 角色权限映射
     */
    @Override
    public Map<Long, Set<String>> selectMenuPermsByRoleIds(Collection<Long> roleIds) {
        Set<Long> menuIds = selectCurrentTenantPackageMenuIds();
        return CollUtil.isEmpty(menuIds) ? Collections.emptyMap() : menuMapper.selectMenuPermsByRoleIds(roleIds, menuIds);
    }

    /**
     * 根据用户ID查询菜单树信息
     *
     * @param userId 用户ID
     * @return 按树结构组织的菜单列表
     */
    @Override
    public List<SysMenu> selectMenuTreeByUserId(Long userId) {
        Set<Long> packageMenuIds = selectCurrentTenantPackageMenuIds();
        if (CollUtil.isEmpty(packageMenuIds)) {
            return CollUtil.newArrayList();
        }
        List<SysMenu> menus;
        if (LoginHelper.isCurrentSuperAdmin(userId)) {
            menus = menuMapper.selectMenuTreeAll(packageMenuIds);
        } else {
            menus = menuMapper.selectMenuTreeByUserId(userId, packageMenuIds);
        }
        if (CollUtil.isEmpty(menus)) {
            return CollUtil.newArrayList();
        }

        List<SysMenu> menuTree = TreeBuildUtils.build(menus, Constants.TOP_PARENT_ID, SysMenu::getParentId, (menu, nodeTreeMaps) -> {
            // 将当前节点的菜单ID用作父节点ID
            Long menuParentId = menu.getMenuId();
            // 从动态规划表中取出子节点列表
            // 如果不存在子节点，则返回一个空的列表，确保数据在进行JSON序列化时该字段的类型和结构是正确的
            List<SysMenu> childMenus = nodeTreeMaps.getOrDefault(menuParentId, Collections.emptyList());
            // 设置子节点
            // 如果存在根节点指向尾节点的情况，则会出现环形依赖。但在菜单表中基本不会出现这种情况...
            menu.setChildren(childMenus);
        });
        return CollUtil.isEmpty(menuTree) ? CollUtil.newArrayList() : menuTree;
    }

    /**
     * 根据角色ID查询菜单树信息
     *
     * @param roleId 角色ID
     * @return 选中菜单列表
     */
    @Override
    public List<Long> selectMenuListByRoleId(Long roleId) {
        Set<Long> menuIds = selectCurrentTenantPackageMenuIds();
        if (CollUtil.isEmpty(menuIds)) {
            return CollUtil.newArrayList();
        }
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            return CollUtil.newArrayList();
        }
        return menuMapper.selectMenuListByRoleId(roleId, role.getMenuCheckStrictly(), menuIds);
    }

    /**
     * 构建前端路由所需要的菜单
     * 路由name命名规则 path首字母转大写 + id
     *
     * @param menus 菜单列表
     * @return 路由列表
     */
    @Override
    public List<RouterVo> buildMenus(List<SysMenu> menus) {
        if (CollUtil.isEmpty(menus)) {
            return CollUtil.newArrayList();
        }
        List<RouterVo> routers = new LinkedList<>();
        for (SysMenu menu : menus) {
            String name = menu.getRouteName() + menu.getMenuId();
            RouterVo router = new RouterVo();
            router.setHidden("1".equals(menu.getVisible()));
            router.setName(name);
            router.setPath(menu.getRouterPath());
            router.setComponent(menu.getComponentInfo());
            router.setQuery(menu.getQueryParam());
            router.setExt(buildRuntimeExt(menu));
            router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), StringUtils.equals(SystemConstants.NO, menu.getIsCache()), menu.getPath(), menu.getActiveMenu()));
            List<SysMenu> cMenus = menu.getChildren();
            if (CollUtil.isNotEmpty(cMenus) && SystemConstants.TYPE_DIR.equals(menu.getMenuType())) {
                router.setAlwaysShow(true);
                router.setRedirect("noRedirect");
                router.setChildren(buildMenus(cMenus));
            } else if (menu.isMenuFrame()) {
                String frameName = StringUtils.capitalize(menu.getPath()) + menu.getMenuId();
                router.setMeta(null);
                List<RouterVo> childrenList = new ArrayList<>();
                RouterVo children = new RouterVo();
                children.setPath(menu.getPath());
                children.setComponent(menu.getComponent());
                children.setName(frameName);
                children.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), StringUtils.equals(SystemConstants.NO, menu.getIsCache()), menu.getPath(), menu.getActiveMenu()));
                children.setQuery(menu.getQueryParam());
                children.setExt(buildRuntimeExt(menu));
                childrenList.add(children);
                router.setChildren(childrenList);
            } else if (menu.getParentId().equals(Constants.TOP_PARENT_ID) && menu.isInnerLink()) {
                router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon()));
                router.setPath("/");
                List<RouterVo> childrenList = new ArrayList<>();
                RouterVo children = new RouterVo();
                String routerPath = SysMenu.innerLinkReplaceEach(menu.getPath());
                String innerLinkName = StringUtils.capitalize(routerPath) + menu.getMenuId();
                children.setPath(routerPath);
                children.setComponent(SystemConstants.INNER_LINK);
                children.setName(innerLinkName);
                children.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), menu.getPath()));
                children.setExt(buildRuntimeExt(menu));
                childrenList.add(children);
                router.setChildren(childrenList);
            }
            routers.add(router);
        }
        return routers;
    }

    /**
     * 微应用配置以 sys_app 为唯一来源。运行时仍合成为前端已经使用的
     * ext.microApp.version=1 契约，避免应用注册信息重新散落到菜单扩展字段。
     */
    private String buildRuntimeExt(SysMenu menu) {
        if (!Constants.TOP_PARENT_ID.equals(menu.getParentId()) || menu.getAppId() == null) {
            return menu.getExt();
        }
        SysApp app = appMapper.selectById(menu.getAppId());
        if (app == null || !"MICRO".equals(app.getAppType()) || !SystemConstants.NORMAL.equals(app.getStatus())) {
            return menu.getExt();
        }
        Map<String, Object> microApp = new LinkedHashMap<>();
        microApp.put("version", 1);
        microApp.put("name", app.getAppKey());
        microApp.put("entry", app.getEntry());
        microApp.put("initialPath", StringUtils.blankToDefault(app.getInitialPath(), "/"));
        microApp.put("alive", Boolean.TRUE.equals(app.getAlive()));
        microApp.put("sync", Boolean.TRUE.equals(app.getSync()));
        Map<String, Object> ext = new LinkedHashMap<>();
        if (StringUtils.isNotBlank(menu.getExt())) {
            try {
                Map<?, ?> original = JsonUtils.parseObject(menu.getExt(), Map.class);
                if (original != null) {
                    original.forEach((key, value) -> ext.put(String.valueOf(key), value));
                }
            } catch (RuntimeException ex) {
                log.warn("菜单扩展字段不是合法JSON，menuId={}", menu.getMenuId());
            }
        }
        ext.put("appName", app.getAppName());
        ext.put("microApp", microApp);
        return JsonUtils.toJsonString(ext);
    }

    /**
     * 构建前端所需要下拉树结构
     *
     * @param menus 菜单列表
     * @return 下拉树结构列表
     */
    @Override
    public List<Tree<Long>> buildMenuTreeSelect(List<SysMenuVo> menus) {
        if (CollUtil.isEmpty(menus)) {
            return CollUtil.newArrayList();
        }
        Set<Long> appIds = StreamUtils.toSet(menus, SysMenuVo::getAppId);
        appIds.remove(null);
        Map<Long, SysApp> appMap = CollUtil.isEmpty(appIds)
            ? Collections.emptyMap()
            : StreamUtils.toIdentityMap(appMapper.selectBatchIds(appIds), SysApp::getAppId);
        return TreeBuildUtils.build(menus, (menu, tree) -> {
            Tree<Long> menuTree = tree.setId(menu.getMenuId())
                .setParentId(menu.getParentId())
                .setName(menu.getMenuName())
                .setWeight(menu.getOrderNum());
            SysApp app = appMap.get(menu.getAppId());
            menuTree.put("appId", menu.getAppId());
            if (app != null) {
                menuTree.put("appKey", app.getAppKey());
                menuTree.put("appName", app.getAppName());
                menuTree.put("appType", app.getAppType());
            }
            menuTree.put("menuType", menu.getMenuType());
            menuTree.put("icon", menu.getIcon());
            menuTree.put("visible", menu.getVisible());
            menuTree.put("status", menu.getStatus());
        });
    }

    /**
     * 根据菜单ID查询信息
     *
     * @param menuId 菜单ID
     * @return 菜单信息
     */
    @Override
    public SysMenuVo selectMenuById(Long menuId) {
        return menuMapper.selectVoById(menuId);
    }

    /**
     * 是否存在菜单子节点
     *
     * @param menuId 菜单ID
     * @return 结果
     */
    @Override
    public boolean hasChildByMenuId(Long menuId) {
        return menuMapper.lambda().eq(SysMenu::getParentId, menuId).exists();
    }

    /**
     * 是否存在菜单子节点
     *
     * @param menuIds 菜单ID列表
     * @return 结果
     */
    @Override
    public boolean hasChildByMenuId(Collection<Long> menuIds) {
        return menuMapper.lambda()
            .in(SysMenu::getParentId, menuIds)
            .notIn(SysMenu::getMenuId, menuIds)
            .exists();
    }

    /**
     * 查询菜单使用数量
     *
     * @param menuId 菜单ID
     * @return 结果
     */
    @Override
    public boolean checkMenuExistRole(Long menuId) {
        return roleMenuMapper.lambda().eq(SysRoleMenu::getMenuId, menuId).exists();
    }

    /**
     * 新增保存菜单信息
     *
     * @param bo 菜单信息
     * @return 结果
     */
    @Override
    public int insertMenu(SysMenuBo bo) {
        SysMenu menu = MapstructUtils.convert(bo, SysMenu.class);
        return menuMapper.insert(menu);
    }

    /**
     * 修改保存菜单信息
     *
     * @param bo 菜单信息
     * @return 结果
     */
    @Override
    public int updateMenu(SysMenuBo bo) {
        SysMenu menu = MapstructUtils.convert(bo, SysMenu.class);
        return menuMapper.updateById(menu);
    }

    /**
     * 删除菜单管理信息
     *
     * @param menuId 菜单ID
     * @return 结果
     */
    @Override
    public int deleteMenuById(Long menuId) {
        return menuMapper.deleteById(menuId);
    }

    /**
     * 批量删除菜单管理信息
     *
     * @param menuIds 菜单ID串
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenuById(Collection<Long> menuIds) {
        menuMapper.deleteByIds(menuIds);
        roleMenuMapper.deleteByMenuIds(menuIds);
    }

    /**
     * 校验菜单名称是否唯一
     *
     * @param menu 菜单信息
     * @return 结果
     */
    @Override
    public boolean checkMenuNameUnique(SysMenuBo menu) {
        boolean exist = menuMapper.lambda()
            .eq(SysMenu::getMenuName, menu.getMenuName())
            .eq(SysMenu::getParentId, menu.getParentId())
            .neIfPresent(SysMenu::getMenuId, menu.getMenuId())
            .exists();
        return !exist;
    }

    /**
     * 校验路由组合是否唯一
     *
     * @param menuBo 菜单信息
     * @return 结果
     */
    @Override
    public boolean checkRouteConfigUnique(SysMenuBo menuBo) {
        SysMenu menu = MapstructUtils.convert(menuBo, SysMenu.class);
        if (SystemConstants.TYPE_BUTTON.equals(menu.getMenuType())) {
            return true;
        }
        long menuId = ObjectUtil.isNull(menu.getMenuId()) ? -1L : menu.getMenuId();
        Long parentId = menu.getParentId();
        String path = menu.getPath();
        String routeName = StringUtils.isEmpty(menu.getRouteName()) ? path : menu.getRouteName();
        List<SysMenu> sysMenuList = menuMapper.lambda()
            .in(SysMenu::getMenuType, SystemConstants.TYPE_DIR, SystemConstants.TYPE_MENU)
            .and(w ->
                w.eq(SysMenu::getPath, path).or().eq(SysMenu::getPath, routeName)
            ).list();
        for (SysMenu sysMenu : sysMenuList) {
            if (!sysMenu.getMenuId().equals(menuId)) {
                Long dbParentId = sysMenu.getParentId();
                String dbPath = sysMenu.getPath();
                String dbRouteName = StringUtils.isEmpty(sysMenu.getRouteName()) ? dbPath : sysMenu.getRouteName();
                if (StringUtils.equalsAnyIgnoreCase(path, dbPath) && parentId.equals(dbParentId)) {
                    log.warn("[同级路由冲突] 同级下已存在相同路由路径 '{}'，冲突菜单：{}", dbPath, sysMenu.getMenuName());
                    return false;
                } else if (StringUtils.equalsAnyIgnoreCase(path, dbPath)
                    && Constants.TOP_PARENT_ID.equals(parentId)
                    && Constants.TOP_PARENT_ID.equals(dbParentId)) {
                    log.warn("[根目录路由冲突] 根目录下路由 '{}' 必须唯一，已被菜单 '{}' 占用", path, sysMenu.getMenuName());
                    return false;
                } else if (StringUtils.equalsAnyIgnoreCase(routeName, dbRouteName)
                    && sysMenu.getMenuType().equals(menu.getMenuType())) {
                    log.warn("[路由名称冲突] 路由名称 '{}' 需全局唯一，已被菜单 '{}' 使用", routeName, sysMenu.getMenuName());
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 校验菜单扩展参数中的业务应用配置。
     *
     * @param menu 菜单信息
     */
    @Override
    public void validateMicroAppConfig(SysMenuBo menu) {
        if (StringUtils.isBlank(menu.getExt())) {
            return;
        }

        JsonNode root = parseExt(menu.getExt(), menu.getMenuName());
        JsonNode microApp = root.get("microApp");
        if (microApp == null) {
            return;
        }
        if (!microApp.isObject()) {
            throw new ServiceException("业务应用配置 microApp 必须是 JSON 对象");
        }
        if (!SystemConstants.TYPE_DIR.equals(menu.getMenuType())
            || !Constants.TOP_PARENT_ID.equals(menu.getParentId())) {
            throw new ServiceException("业务应用只能配置在顶级目录菜单");
        }
        if (!microApp.path("version").isIntegralNumber() || microApp.path("version").asInt() != 1) {
            throw new ServiceException("业务应用配置 version 仅支持 1");
        }

        String name = requiredText(microApp, "name", "应用标识不能为空");
        if (!MICRO_APP_NAME_PATTERN.matcher(name).matches()) {
            throw new ServiceException("应用标识只能包含小写字母、数字和连字符");
        }
        String entry = requiredText(microApp, "entry", "应用入口不能为空");
        String entryPrefix = "/micro-apps/" + name + "/";
        if (!entry.startsWith(entryPrefix) || containsUnsafePath(entry)) {
            throw new ServiceException("应用入口只能使用 " + entryPrefix + " 下的同域相对地址");
        }
        String initialPath = requiredText(microApp, "initialPath", "应用初始路由不能为空");
        if (!initialPath.startsWith("/") || initialPath.startsWith("//") || containsUnsafePath(initialPath)) {
            throw new ServiceException("应用初始路由必须是以 / 开头的安全相对路径");
        }
        if (!microApp.path("alive").isBoolean() || !microApp.path("sync").isBoolean()) {
            throw new ServiceException("业务应用配置 alive 和 sync 必须是布尔值");
        }

        boolean duplicated = menuMapper.lambda()
            .isNotNull(SysMenu::getExt)
            .ne(SysMenu::getExt, "")
            .neIfPresent(SysMenu::getMenuId, menu.getMenuId())
            .list()
            .stream()
            .map(SysMenu::getExt)
            .map(this::parseExistingMicroAppName)
            .anyMatch(name::equals);
        if (duplicated) {
            throw new ServiceException("应用标识 '" + name + "' 已存在");
        }
    }

    private JsonNode parseExt(String ext, String menuName) {
        try {
            JsonNode root = JsonUtils.getObjectMapper().readTree(ext);
            if (root == null || !root.isObject()) {
                throw new ServiceException("菜单'" + menuName + "'的扩展参数必须是 JSON 对象");
            }
            return root;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("菜单'" + menuName + "'的扩展参数不是合法 JSON");
        }
    }

    private String parseExistingMicroAppName(String ext) {
        try {
            JsonNode root = JsonUtils.getObjectMapper().readTree(ext);
            JsonNode microApp = root == null ? null : root.get("microApp");
            return microApp != null && microApp.isObject() ? microApp.path("name").asText() : null;
        } catch (Exception e) {
            log.warn("忽略数据库中无法解析的菜单扩展参数: {}", ext);
            return null;
        }
    }

    private String requiredText(JsonNode node, String fieldName, String message) {
        JsonNode value = node.get(fieldName);
        if (value == null || !value.isTextual() || StringUtils.isBlank(value.asText())) {
            throw new ServiceException(message);
        }
        return value.asText();
    }

    private boolean containsUnsafePath(String path) {
        return path.contains("..") || path.contains("\\") || path.contains("://")
            || path.contains("?") || path.contains("#");
    }

}
