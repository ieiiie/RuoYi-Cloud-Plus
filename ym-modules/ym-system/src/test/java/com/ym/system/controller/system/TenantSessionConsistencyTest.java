package com.ym.system.controller.system;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.context.mock.SaStorageForMock;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.context.model.SaResponse;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.session.SaTerminalInfo;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.ym.common.core.domain.R;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.satoken.core.dao.PlusSaTokenDao;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.core.TenantSaTokenDao;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.system.api.model.LoginUser;
import com.ym.system.domain.vo.SysRoleVo;
import com.ym.system.domain.vo.SysUserVo;
import com.ym.system.domain.vo.UserInfoVo;
import com.ym.system.service.*;
import org.junit.jupiter.api.*;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * No Spring application or external Redis/database is started. Separate class loaders model
 * service-local DAO caches; a serialized in-memory Redis and shared locks model their boundary.
 */
@Tag("dev")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TenantSessionConsistencyTest {
    private static final String TOKEN = "synthetic-tenant-switch-token";
    private static final String TENANT_A = "000000";
    private static final String TENANT_B = "test-tenant-b";
    private final MemoryRedis redis = new MemoryRedis();
    private final SaTokenContextForThreadLocal context = new SaTokenContextForThreadLocal();
    private final TenantSaTokenDao dao = new TenantSaTokenDao();
    private ISysUserService users;
    private ISysRoleService roles;
    private ISysPermissionService permissions;
    private SysUserController controller;
    private ExecutorService pool;

    @BeforeAll
    void initializeOnlyInMemoryDependencies() {
        GenericApplicationContext spring = new GenericApplicationContext();
        spring.getBeanFactory().registerSingleton("redissonClient", redis.client);
        spring.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of("tenant.enable", "true")));
        spring.refresh();
        new SpringUtil().setApplicationContext(spring);
        new SpringUtil().postProcessBeanFactory(spring.getBeanFactory());
        SaManager.setSaTokenContext(context);
        SaManager.setSaTokenDao(dao);
        SaManager.setConfig(new SaTokenConfig().setTokenName("Authorization")
            .setTimeout(1800).setActiveTimeout(-1).setIsLog(false));
        StpUtil.setStpLogic(new StpLogicJwtForSimple());
    }

    @BeforeEach
    void seedSyntheticSession() {
        redis.reset();
        SaManager.setSaTokenDao(dao);
        users = mock(ISysUserService.class);
        roles = mock(ISysRoleService.class);
        permissions = mock(ISysPermissionService.class);
        controller = new SysUserController(users, roles, mock(ISysPostService.class),
            mock(ISysDeptService.class), mock(ISysGlobalUserService.class),
            new SysLoginPermissionService(permissions));
        pool = Executors.newFixedThreadPool(2);
        when(users.selectUserById(anyLong())).thenAnswer(i -> userView(i.getArgument(0)));
        when(roles.selectRolesByUserId(anyLong())).thenReturn(List.of());
        when(permissions.getMenuPermission(anyLong())).thenReturn(Set.of("device:read"));
        when(permissions.getRolePermission(anyLong())).thenReturn(Set.of("member"));
        when(permissions.getDataScopeRoleMap(anyList())).thenReturn(Map.of());
        seed(TOKEN, member(10L, TENANT_A));
    }

    @AfterEach
    void clearRequestAndWorkers() throws InterruptedException {
        context.clearContext();
        pool.shutdownNow();
        assertTrue(pool.awaitTermination(3, TimeUnit.SECONDS));
    }

    @Test
    void separateServiceCachesImmediatelyObserveIdentityChangesAndDeletion() throws Exception {
        try (URLClassLoader authLoader = serviceLoader(); URLClassLoader systemLoader = serviceLoader()) {
            SaTokenDao auth = (SaTokenDao) authLoader.loadClass(TenantSaTokenDao.class.getName()).getConstructor().newInstance();
            SaTokenDao system = (SaTokenDao) systemLoader.loadClass(TenantSaTokenDao.class.getName()).getConstructor().newInstance();
            assertNotSame(auth.getClass(), system.getClass());
            String mapping = "synthetic:mapping";
            String session = "synthetic:token-session";
            auth.set(mapping, "sys_user:10", 1800);
            auth.setObject(session, member(10L, TENANT_A), 1800);
            assertEquals("sys_user:10", system.get(mapping));
            assertEquals(TENANT_A, ((LoginUser) system.getObject(session)).getTenantId());
            assertEquals(TENANT_A, system.getObject(session, LoginUser.class).getTenantId());
            auth.update(mapping, "sys_user:20");
            auth.updateObject(session, member(20L, TENANT_B));
            assertEquals("sys_user:20", system.get(mapping), "another process must not retain token mapping");
            assertEquals(TENANT_B, ((LoginUser) system.getObject(session)).getTenantId());
            assertEquals(TENANT_B, system.getObject(session, LoginUser.class).getTenantId());
            auth.delete(mapping);
            auth.deleteObject(session);
            assertNull(system.get(mapping));
            assertNull(system.getObject(session));
        }
    }

    @Test
    void tenantDaoPreservesExactMillisecondDeadlineAndNeverExpire() {
        dao.set("ttl:mapping", "before", 1800);
        dao.setObject("ttl:object", "before", 1800);
        long mappingDeadline = redis.deadline("global:ttl:mapping");
        long objectDeadline = redis.deadline("global:ttl:object");
        redis.now.addAndGet(137);
        dao.update("ttl:mapping", "after");
        dao.updateObject("ttl:object", "after");
        assertEquals(mappingDeadline, redis.deadline("global:ttl:mapping"));
        assertEquals(objectDeadline, redis.deadline("global:ttl:object"));
        dao.set("ttl:forever", "before", -1);
        dao.setObject("ttl:forever-object", "before", -1);
        dao.update("ttl:forever", "after");
        dao.updateObject("ttl:forever-object", "after");
        assertEquals(-1, redis.deadline("global:ttl:forever"));
        assertEquals(-1, redis.deadline("global:ttl:forever-object"));
        dao.update("ttl:absent", "no-resurrection");
        dao.updateObject("ttl:absent-object", "no-resurrection");
        assertNull(dao.get("ttl:absent"));
        assertNull(dao.getObject("ttl:absent-object"));
    }

    @Test
    void getInfoStartedBeforeSwitchCannotRestoreOldTenant() throws Exception {
        CountDownLatch queryStarted = new CountDownLatch(1);
        CountDownLatch releaseQuery = new CountDownLatch(1);
        CountDownLatch switchAttemptedOrCompleted = new CountDownLatch(1);
        when(users.selectUserById(10L)).thenAnswer(i -> {
            queryStarted.countDown();
            await(releaseQuery);
            return userView(10L);
        });
        long tokenDeadline = redis.deadline(mappingKey(TOKEN));
        long sessionDeadline = redis.deadline(sessionKey(TOKEN));
        Future<R<UserInfoVo>> info = pool.submit(() -> request(TOKEN, controller::getInfo));
        await(queryStarted);
        redis.lockAttempt.set(() -> switchAttemptedOrCompleted.countDown());
        Future<?> change = pool.submit(() -> {
            try {
                request(TOKEN, () -> { LoginHelper.updateLoginUser(member(20L, TENANT_B)); return null; });
            } finally {
                switchAttemptedOrCompleted.countDown();
            }
        });
        try {
            // In the old implementation switch completes here; in the fixed one it waits on the token lock.
            await(switchAttemptedOrCompleted);
        } finally {
            releaseQuery.countDown();
        }
        info.get(3, TimeUnit.SECONDS);
        change.get(3, TimeUnit.SECONDS);
        request(TOKEN, () -> {
            assertEquals(TENANT_B, LoginHelper.getTenantId());
            assertEquals("sys_user:20", StpUtil.getLoginId());
            assertEquals(20L, controller.getInfo().getData().getUser().getUserId());
            assertEquals(TOKEN, StpUtil.getTokenValue());
            return null;
        });
        assertEquals(tokenDeadline, redis.deadline(mappingKey(TOKEN)));
        assertEquals(sessionDeadline, redis.deadline(sessionKey(TOKEN)));
    }

    @Test
    void getInfoWaitingForSwitchRereadsTargetMemberInsideLock() throws Exception {
        CountDownLatch switching = new CountDownLatch(1);
        CountDownLatch releaseSwitch = new CountDownLatch(1);
        CountDownLatch infoAttempted = new CountDownLatch(1);
        Future<?> change = pool.submit(() -> request(TOKEN, () -> LoginHelper.withTokenSessionLock(() -> {
            switching.countDown();
            await(releaseSwitch);
            LoginHelper.updateLoginUser(member(20L, TENANT_B));
            return null;
        })));
        await(switching);
        redis.lockAttempt.set(infoAttempted::countDown);
        Future<R<UserInfoVo>> info = pool.submit(() -> request(TOKEN, controller::getInfo));
        try { await(infoAttempted); } finally { releaseSwitch.countDown(); }
        change.get(3, TimeUnit.SECONDS);
        assertEquals(20L, info.get(3, TimeUnit.SECONDS).getData().getUser().getUserId());
        verify(users, never()).selectUserById(10L);
        assertEquals(TENANT_B, storedUser().getTenantId());
    }

    @Test
    void requestPreReadDoesNotPinLoginIdTokenSessionOrTenantAfterAnotherRequestSwitches() throws Exception {
        CountDownLatch preRead = new CountDownLatch(1);
        CountDownLatch switched = new CountDownLatch(1);
        Future<R<UserInfoVo>> info = pool.submit(() -> request(TOKEN, () -> {
            assertEquals("sys_user:10", StpUtil.getLoginId());
            assertEquals(TENANT_A, TenantHelper.getTenantId());
            SaSession previous = StpUtil.getTokenSession();
            assertNull(TenantHelper.getDynamic(), "an ordinary tenant read must not create a thread-local override");
            preRead.countDown();
            await(switched);
            R<UserInfoVo> result = controller.getInfo();
            assertEquals("sys_user:20", StpUtil.getLoginId());
            assertEquals(TENANT_B, TenantHelper.getTenantId());
            assertNotSame(previous, StpUtil.getTokenSession());
            return result;
        }));
        await(preRead);
        try { request(TOKEN, () -> { LoginHelper.updateLoginUser(member(20L, TENANT_B)); return null; }); }
        finally { switched.countDown(); }
        assertEquals(20L, info.get(3, TimeUnit.SECONDS).getData().getUser().getUserId());
        verify(users, never()).selectUserById(10L);
    }

    @Test
    void switchingAfterRequestPreReadRebindsFromLatestAccount() throws Exception {
        CountDownLatch preRead = new CountDownLatch(1);
        CountDownLatch switched = new CountDownLatch(1);
        Future<?> changeAgain = pool.submit(() -> request(TOKEN, () -> {
            assertEquals("sys_user:10", StpUtil.getLoginId());
            assertEquals(TENANT_A, LoginHelper.getTenantId());
            preRead.countDown(); await(switched);
            LoginHelper.updateLoginUser(member(30L, "test-tenant-c"));
            return null;
        }));
        await(preRead);
        try { request(TOKEN, () -> { LoginHelper.updateLoginUser(member(20L, TENANT_B)); return null; }); }
        finally { switched.countDown(); }
        changeAgain.get(3, TimeUnit.SECONDS);
        assertEquals("test-tenant-c", storedUser().getTenantId());
        assertNull(dao.getSession(StpUtil.stpLogic.splicingKeySession("sys_user:20")), "the intermediate account must lose the terminal");
        assertNotNull(dao.getSession(StpUtil.stpLogic.splicingKeySession("sys_user:30")).getTerminal(TOKEN));
        request(TOKEN, () -> { assertEquals("sys_user:30", StpUtil.getLoginId()); return null; });
    }

    @Test
    void getInfoStillRevokesRemovedPermissionsWithoutRebindingAccountOrChangingTtl() {
        long tokenDeadline = redis.deadline(mappingKey(TOKEN));
        long sessionDeadline = redis.deadline(sessionKey(TOKEN));
        byte[] accountBefore = redis.bytes(accountKey("sys_user:10"));
        request(TOKEN, () -> {
            R<UserInfoVo> result = controller.getInfo();
            assertEquals(Set.of("device:read"), result.getData().getPermissions());
            assertEquals(Set.of("member"), result.getData().getRoles());
            return null;
        });
        assertEquals(Set.of("device:read"), storedUser().getMenuPermission());
        assertFalse(storedUser().getMenuPermission().contains("device:delete"));
        assertArrayEquals(accountBefore, redis.bytes(accountKey("sys_user:10")));
        assertEquals(tokenDeadline, redis.deadline(mappingKey(TOKEN)));
        assertEquals(sessionDeadline, redis.deadline(sessionKey(TOKEN)));
        assertTrue(redis.locks.keySet().stream().allMatch(k -> k.startsWith("global:satoken:session-lock:login:")));
        assertTrue(redis.locks.keySet().stream().noneMatch(k -> k.contains(TOKEN) || k.contains(TENANT_A)));
    }

    @Test
    void getInfoKeepsPlatformAdministratorPermissions() {
        LoginUser admin = member(10L, TENANT_A);
        admin.setGlobalUserId(SystemConstants.SUPER_ADMIN_GLOBAL_USER_ID);
        seed(TOKEN, admin);
        request(TOKEN, () -> {
            assertEquals(Set.of("*:*:*"), controller.getInfo().getData().getPermissions());
            return null;
        });
        assertEquals(Set.of(SystemConstants.SUPER_ADMIN_ROLE_KEY), storedUser().getRolePermission());
    }

    @Test
    void failedPermissionRefreshDoesNotPersistAndReleasesLock() {
        byte[] before = redis.bytes(sessionKey(TOKEN));
        when(permissions.getMenuPermission(10L)).thenThrow(new IllegalStateException("synthetic query failure"));
        request(TOKEN, () -> {
            assertThrows(IllegalStateException.class, controller::getInfo);
            LoginHelper.updateLoginUser(member(20L, TENANT_B));
            return null;
        });
        assertEquals(TENANT_B, storedUser().getTenantId());
        assertTrue(redis.locks.values().stream().noneMatch(ReentrantLock::isLocked));
        // Re-run without switching to check persistence independently.
        seed(TOKEN, member(10L, TENANT_A));
        before = redis.bytes(sessionKey(TOKEN));
        request(TOKEN, () -> { assertThrows(IllegalStateException.class, controller::getInfo); return null; });
        assertArrayEquals(before, redis.bytes(sessionKey(TOKEN)));
    }

    @Test
    void permissionCallbackCannotChangeTenantIdentity() {
        byte[] before = redis.bytes(sessionKey(TOKEN));
        request(TOKEN, () -> {
            assertThrows(ServiceException.class, () -> LoginHelper.refreshLoginUser(user -> {
                user.setTenantId(TENANT_B);
                return null;
            }));
            return null;
        });
        assertArrayEquals(before, redis.bytes(sessionKey(TOKEN)));
    }

    @Test
    void unrelatedTokenDoesNotWaitForCurrentTokenLock() throws Exception {
        seed("synthetic-other-token", member(30L, "other-tenant"));
        CountDownLatch held = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Future<?> holder = pool.submit(() -> request(TOKEN, () -> LoginHelper.withTokenSessionLock(() -> {
            held.countDown(); await(release); return null;
        })));
        await(held);
        try {
            Future<R<UserInfoVo>> other = pool.submit(() -> request("synthetic-other-token", controller::getInfo));
            assertEquals(30L, other.get(2, TimeUnit.SECONDS).getData().getUser().getUserId());
        } finally { release.countDown(); }
        holder.get(3, TimeUnit.SECONDS);
    }

    @Test
    void timedOutLockDoesNotWriteOrUnlockAnotherOwner() {
        byte[] before = redis.bytes(sessionKey(TOKEN));
        redis.failLock = true;
        request(TOKEN, () -> { assertThrows(ServiceException.class, controller::getInfo); return null; });
        assertArrayEquals(before, redis.bytes(sessionKey(TOKEN)));
        assertEquals(0, redis.unlockCalls);
    }

    @Test
    void interruptedLockRestoresInterruptFlagWithoutWriting() {
        byte[] before = redis.bytes(sessionKey(TOKEN));
        redis.interruptLock = true;
        try {
            request(TOKEN, () -> { assertThrows(ServiceException.class, controller::getInfo); return null; });
            assertTrue(Thread.currentThread().isInterrupted());
            assertArrayEquals(before, redis.bytes(sessionKey(TOKEN)));
            assertEquals(0, redis.unlockCalls);
        } finally { Thread.interrupted(); }
    }

    private void seed(String token, LoginUser user) {
        String id = user.getLoginId();
        dao.set(StpUtil.stpLogic.splicingKeyTokenValue(token), id, 1800);
        SaSession session = new SaSession(StpUtil.stpLogic.splicingKeyTokenSession(token));
        session.set(LoginHelper.LOGIN_USER_KEY, user);
        dao.setSession(session, 1800);
        SaSession account = new SaSession(StpUtil.stpLogic.splicingKeySession(id));
        account.setLoginId(id);
        account.addTerminal(new SaTerminalInfo().setTokenValue(token).setDeviceType("pc").setCreateTime(1));
        dao.setSession(account, 1800);
    }

    private LoginUser member(Long id, String tenant) {
        LoginUser user = new LoginUser();
        user.setUserId(id); user.setGlobalUserId(100L); user.setUserType("sys_user");
        user.setTenantId(tenant); user.setDeviceType("pc"); user.setClientKey("test-client");
        user.setMenuPermission(Set.of("device:read", "device:delete"));
        user.setRolePermission(Set.of("member", "removed-role"));
        user.setRoles(List.of()); user.setDataScopeRoleMap(Map.of());
        return user;
    }

    private SysUserVo userView(Long id) { SysUserVo view = new SysUserVo(); view.setUserId(id); return view; }
    private LoginUser storedUser() { return (LoginUser) dao.getSession(StpUtil.stpLogic.splicingKeyTokenSession(TOKEN)).get(LoginHelper.LOGIN_USER_KEY); }
    private String mappingKey(String token) { return "global:" + StpUtil.stpLogic.splicingKeyTokenValue(token); }
    private String sessionKey(String token) { return "global:" + StpUtil.stpLogic.splicingKeyTokenSession(token); }
    private String accountKey(String id) { return "global:" + StpUtil.stpLogic.splicingKeySession(id); }

    private <T> T request(String token, Supplier<T> action) {
        SaRequest request = mock(SaRequest.class);
        when(request.getHeader("Authorization")).thenReturn(token);
        context.setContext(request, mock(SaResponse.class), new SaStorageForMock());
        try { return action.get(); } finally { context.clearContext(); }
    }

    private static void await(CountDownLatch latch) {
        try { assertTrue(latch.await(3, TimeUnit.SECONDS), "test synchronization timed out"); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
    }

    private URLClassLoader serviceLoader() {
        URL[] urls = {PlusSaTokenDao.class.getProtectionDomain().getCodeSource().getLocation(),
            TenantSaTokenDao.class.getProtectionDomain().getCodeSource().getLocation()};
        return new URLClassLoader(urls, getClass().getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!name.equals(PlusSaTokenDao.class.getName()) && !name.equals(TenantSaTokenDao.class.getName())) {
                    return super.loadClass(name, resolve);
                }
                synchronized (getClassLoadingLock(name)) {
                    Class<?> result = findLoadedClass(name);
                    if (result == null) result = findClass(name);
                    if (resolve) resolveClass(result);
                    return result;
                }
            }
        };
    }

    /** Serialized values deliberately prevent object references leaking between simulated services. */
    private static final class MemoryRedis {
        private record Entry(byte[] bytes, long deadline) { }
        final Map<String, Entry> values = new ConcurrentHashMap<>();
        final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();
        final java.util.concurrent.atomic.AtomicReference<Runnable> lockAttempt = new java.util.concurrent.atomic.AtomicReference<>();
        final AtomicLong now = new AtomicLong(100_000);
        final RedissonClient client;
        volatile boolean failLock;
        volatile boolean interruptLock;
        volatile int unlockCalls;

        MemoryRedis() {
            client = mock(RedissonClient.class);
            RKeys keys = mock(RKeys.class, invocation -> {
                String[] names = (String[]) invocation.getRawArguments()[0];
                return switch (invocation.getMethod().getName()) {
                    case "countExists" -> Arrays.stream(names).filter(values::containsKey).count();
                    case "delete" -> Arrays.stream(names).filter(name -> values.remove(name) != null).count();
                    default -> throw new AssertionError("Unexpected keys operation: " + invocation.getMethod());
                };
            });
            when(client.getKeys()).thenReturn(keys);
            when(client.getBucket(anyString())).thenAnswer(i -> bucket(i.getArgument(0)));
            when(client.getLock(anyString())).thenAnswer(i -> lock(i.getArgument(0)));
        }

        void reset() {
            values.clear(); locks.clear(); lockAttempt.set(null); now.set(100_000);
            failLock = false; interruptLock = false; unlockCalls = 0;
        }
        byte[] bytes(String key) { return values.get(key).bytes(); }
        long deadline(String key) { return values.get(key).deadline(); }

        @SuppressWarnings("rawtypes")
        private RBucket bucket(String key) {
            return mock(RBucket.class, invocation -> {
                Entry entry = values.get(key);
                if (entry != null && entry.deadline() != -1 && entry.deadline() <= now.get()) {
                    values.remove(key); entry = null;
                }
                Object[] args = invocation.getArguments();
                return switch (invocation.getMethod().getName()) {
                    case "get" -> entry == null ? null : deserialize(entry.bytes());
                    case "isExists" -> entry != null;
                    case "remainTimeToLive" -> entry == null ? -2L : entry.deadline() == -1 ? -1L : entry.deadline() - now.get();
                    case "set" -> {
                        long until = args.length == 1 ? -1 : now.get() + ((Duration) args[1]).toMillis();
                        values.put(key, new Entry(serialize(args[0]), until)); yield null;
                    }
                    case "setAndKeepTTL" -> {
                        values.put(key, new Entry(serialize(args[0]), entry == null ? -1 : entry.deadline())); yield null;
                    }
                    case "delete" -> values.remove(key) != null;
                    case "expire" -> {
                        if (entry != null) values.put(key, new Entry(entry.bytes(), now.get() + ((Duration) args[0]).toMillis()));
                        yield entry != null;
                    }
                    default -> throw new AssertionError("Unexpected Redis operation: " + invocation.getMethod());
                };
            });
        }

        private RLock lock(String key) {
            ReentrantLock delegate = locks.computeIfAbsent(key, k -> new ReentrantLock());
            return mock(RLock.class, invocation -> {
                switch (invocation.getMethod().getName()) {
                    case "tryLock":
                        Runnable attempted = lockAttempt.get(); if (attempted != null) attempted.run();
                        if (interruptLock) throw new InterruptedException("synthetic interruption");
                        if (failLock) return false;
                        assertEquals(2, invocation.getArguments().length, "use watchdog, not fixed lease");
                        return delegate.tryLock((Long) invocation.getArgument(0), invocation.getArgument(1));
                    case "unlock": delegate.unlock(); unlockCalls++; return null;
                    default: throw new AssertionError("Unexpected lock operation: " + invocation.getMethod());
                }
            });
        }

        private static byte[] serialize(Object value) throws IOException {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(value); }
            return bytes.toByteArray();
        }
        private static Object deserialize(byte[] value) throws IOException, ClassNotFoundException {
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(value))) { return in.readObject(); }
        }
    }
}
