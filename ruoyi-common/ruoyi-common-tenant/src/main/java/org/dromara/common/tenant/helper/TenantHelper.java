package org.dromara.common.tenant.helper;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.reflect.ReflectUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.tenant.exception.TenantException;

import java.util.Stack;
import java.util.function.Supplier;

/**
 * 租户上下文助手。
 *
 * <p>当前租户优先取动态上下文；不存在时再从登录令牌读取。动态上下文只用于
 * 平台管理员切换租户、登录前用户查询等受控场景。</p>
 *
 * @author Lion Li
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TenantHelper {

    /** token session 中的平台临时数据视图租户。 */
    private static final String DYNAMIC_TENANT_KEY = "dynamicTenant";

    private static final ThreadLocal<String> TEMP_DYNAMIC_TENANT = new ThreadLocal<>();

    private static final ThreadLocal<Stack<Integer>> REENTRANT_IGNORE = ThreadLocal.withInitial(Stack::new);

    /**
     * 租户功能是否启用。
     *
     * @return 是否启用
     */
    public static boolean isEnable() {
        return Convert.toBool(SpringUtils.getProperty("tenant.enable"), false);
    }

    /**
     * 校验租户模式下的外部请求是否提供了租户编号。
     *
     * <p>缺少租户编号时行级插件会跳过过滤；因此注册等必须明确目标租户的
     * 匿名入口应主动调用此方法，不能依赖插件兜底。全局账号登录不需要调用。</p>
     *
     * @param tenantId 租户编号
     */
    public static void checkTenantId(String tenantId) {
        if (isEnable() && StringUtils.isBlank(tenantId)) {
            throw new TenantException("tenant.number.not.blank");
        }
    }

    private static IgnoreStrategy getIgnoreStrategy() {
        Object ignoreStrategyLocal = ReflectUtils.getStaticFieldValue(
            ReflectUtils.getField(InterceptorIgnoreHelper.class, "IGNORE_STRATEGY_LOCAL"));
        if (ignoreStrategyLocal instanceof ThreadLocal<?> ignoreStrategyLocalHolder
            && ignoreStrategyLocalHolder.get() instanceof IgnoreStrategy ignoreStrategy) {
            return ignoreStrategy;
        }
        return null;
    }

    private static void enableIgnore() {
        IgnoreStrategy ignoreStrategy = getIgnoreStrategy();
        if (ObjectUtil.isNull(ignoreStrategy)) {
            InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().tenantLine(true).build());
        } else {
            ignoreStrategy.setTenantLine(true);
        }
        Stack<Integer> reentrantStack = REENTRANT_IGNORE.get();
        reentrantStack.push(reentrantStack.size() + 1);
    }

    private static void disableIgnore() {
        IgnoreStrategy ignoreStrategy = getIgnoreStrategy();
        if (ObjectUtil.isNull(ignoreStrategy)) {
            return;
        }
        boolean noOtherIgnoreStrategy = !Boolean.TRUE.equals(ignoreStrategy.getDynamicTableName())
            && !Boolean.TRUE.equals(ignoreStrategy.getBlockAttack())
            && !Boolean.TRUE.equals(ignoreStrategy.getIllegalSql())
            && !Boolean.TRUE.equals(ignoreStrategy.getDataPermission())
            && CollectionUtil.isEmpty(ignoreStrategy.getOthers());
        Stack<Integer> reentrantStack = REENTRANT_IGNORE.get();
        boolean empty = reentrantStack.isEmpty() || reentrantStack.pop() == 1;
        if (noOtherIgnoreStrategy && empty) {
            InterceptorIgnoreHelper.clearIgnoreStrategy();
        } else if (empty) {
            ignoreStrategy.setTenantLine(false);
        }
    }

    /**
     * 在忽略租户隔离的上下文中执行指定动作。
     *
     * @param handle 动作
     */
    public static void ignore(Runnable handle) {
        enableIgnore();
        try {
            handle.run();
        } finally {
            disableIgnore();
        }
    }

    /**
     * 在忽略租户隔离的上下文中执行指定动作。
     *
     * @param handle 动作
     * @param <T> 返回值类型
     * @return 动作返回值
     */
    public static <T> T ignore(Supplier<T> handle) {
        enableIgnore();
        try {
            return handle.get();
        } finally {
            disableIgnore();
        }
    }

    /**
     * 设置当前线程动态租户。
     *
     * @param tenantId 租户编号
     */
    public static void setDynamic(String tenantId) {
        setDynamic(tenantId, false);
    }

    /**
     * 设置动态租户。
     *
     * @param tenantId 租户编号
     * @param global 是否在当前登录会话范围内持久化
     */
    public static void setDynamic(String tenantId, boolean global) {
        if (!isEnable()) {
            return;
        }
        if (!LoginHelper.isLogin() || !global) {
            TEMP_DYNAMIC_TENANT.set(tenantId);
            return;
        }
        StpUtil.getTokenSession().set(DYNAMIC_TENANT_KEY, tenantId);
    }

    /**
     * 获取动态租户。
     *
     * @return 动态租户编号；不存在时返回 {@code null}
     */
    public static String getDynamic() {
        if (!isEnable()) {
            return null;
        }
        String tenantId = TEMP_DYNAMIC_TENANT.get();
        if (StringUtils.isNotBlank(tenantId)) {
            return tenantId;
        }
        if (!LoginHelper.isLogin()) {
            return null;
        }
        Object sessionTenantId = StpUtil.getTokenSession().get(DYNAMIC_TENANT_KEY);
        return sessionTenantId == null ? null : Convert.toStr(sessionTenantId);
    }

    /**
     * 清除动态租户。
     */
    public static void clearDynamic() {
        if (!isEnable()) {
            return;
        }
        TEMP_DYNAMIC_TENANT.remove();
        if (!LoginHelper.isLogin()) {
            return;
        }
        StpUtil.getTokenSession().delete(DYNAMIC_TENANT_KEY);
    }

    /**
     * 在指定租户上下文中执行指定动作。
     *
     * @param tenantId 租户编号
     * @param handle 动作
     */
    public static void dynamic(String tenantId, Runnable handle) {
        if (!isEnable()) {
            handle.run();
            return;
        }
        String previousTenantId = TEMP_DYNAMIC_TENANT.get();
        if (StringUtils.isBlank(tenantId)) {
            TEMP_DYNAMIC_TENANT.remove();
        } else {
            TEMP_DYNAMIC_TENANT.set(tenantId);
        }
        try {
            handle.run();
        } finally {
            restoreTemporaryDynamic(previousTenantId);
        }
    }

    /**
     * 在指定租户上下文中执行指定动作。
     *
     * @param tenantId 租户编号
     * @param handle 动作
     * @param <T> 返回值类型
     * @return 动作返回值
     */
    public static <T> T dynamic(String tenantId, Supplier<T> handle) {
        if (!isEnable()) {
            return handle.get();
        }
        String previousTenantId = TEMP_DYNAMIC_TENANT.get();
        if (StringUtils.isBlank(tenantId)) {
            TEMP_DYNAMIC_TENANT.remove();
        } else {
            TEMP_DYNAMIC_TENANT.set(tenantId);
        }
        try {
            return handle.get();
        } finally {
            restoreTemporaryDynamic(previousTenantId);
        }
    }

    /**
     * 恢复临时上下文，不影响平台管理员持久化的动态租户选择。
     */
    private static void restoreTemporaryDynamic(String previousTenantId) {
        if (StringUtils.isBlank(previousTenantId)) {
            TEMP_DYNAMIC_TENANT.remove();
        } else {
            TEMP_DYNAMIC_TENANT.set(previousTenantId);
        }
    }

    /**
     * 获取当前有效租户编号。
     *
     * @return 当前租户编号；租户功能未开启时返回 {@code null}
     */
    public static String getTenantId() {
        if (!isEnable()) {
            return null;
        }
        String tenantId = getDynamic();
        return StringUtils.isNotBlank(tenantId) ? tenantId : LoginHelper.getTenantId();
    }

}
