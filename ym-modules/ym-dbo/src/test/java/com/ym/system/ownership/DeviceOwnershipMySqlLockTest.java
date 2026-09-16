package com.ym.system.ownership;

import com.ym.common.core.exception.ServiceException;
import com.ym.system.ownership.dao.DeviceOwnershipRepository;
import com.ym.system.ownership.model.*;
import com.ym.system.ownership.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.*;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Explicit opt-in, disposable localhost schema only. Never point this fixture at a business database. */
@Tag("ownership")
@EnabledIfSystemProperty(named="ownership.mysql.url",matches="jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/ownership_lock_test\\?.*")
class DeviceOwnershipMySqlLockTest {
    static JdbcTemplate admin;
    static DriverManagerDataSource writer;
    JdbcTemplate jdbc;
    DeviceOwnershipRepository repository;
    DeviceOwnershipService service;
    TransactionTemplate tx;
    OwnershipActor actor;
    OwnershipTargetTenantValidator tenants;
    OwnershipBlockerService blockers;
    OwnershipOperationFence fence;
    OwnershipRuleCleanup cleanup;
    JetLinksOwnershipMetadataSnapshot metadataSnapshots;

    @BeforeAll static void schema() throws Exception {
        String url=System.getProperty("ownership.mysql.url");
        String rootPassword=Files.readString(Path.of(System.getProperty("ownership.mysql.adminPasswordFile"))).trim();
        String writerPassword=Files.readString(Path.of(System.getProperty("ownership.mysql.passwordFile"))).trim();
        admin=new JdbcTemplate(new DriverManagerDataSource(url,"root",rootPassword));
        assertEquals("ownership_lock_test",admin.queryForObject("SELECT DATABASE()",String.class));
        String ddl=Files.readString(DeviceOwnershipIntegrationTest.migration()).split("DROP PROCEDURE")[0]
            .replace("USE `ym-iot`;","");
        admin.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) connection -> {
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,
                new org.springframework.core.io.support.EncodedResource(new org.springframework.core.io.ByteArrayResource(ddl.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
            return null;
        });
        admin.execute("CREATE TABLE iot_device(device_id BIGINT PRIMARY KEY,device_code VARCHAR(40),device_name VARCHAR(80),del_flag CHAR(1)) ENGINE=InnoDB");
        admin.execute("ALTER TABLE iot_device_ownership_history ADD operator_source VARCHAR(20) DEFAULT 'BUSINESS'");
        admin.execute("ALTER TABLE iot_device_ownership_rule_cleanup ADD operator_source VARCHAR(20) DEFAULT 'BUSINESS'");
        admin.execute("ALTER TABLE iot_device_ownership_history ADD metadata_snapshot LONGTEXT NULL");
        // Parameter binding keeps the generated fixture credential out of SQL/log messages.
        admin.update("CREATE USER 'dbo_lock_test'@'%' IDENTIFIED BY ?",writerPassword);
        admin.execute("GRANT SELECT ON ownership_lock_test.iot_device TO 'dbo_lock_test'@'%'");
        for(String table:List.of("iot_device_ownership","iot_device_ownership_history","iot_device_ownership_rule_cleanup"))
            admin.execute("GRANT SELECT,INSERT,UPDATE ON ownership_lock_test."+table+" TO 'dbo_lock_test'@'%'");
        writer=new DriverManagerDataSource(url,"dbo_lock_test",writerPassword);
    }
    @BeforeEach @SuppressWarnings("unchecked") void setup() {
        for(String table:List.of("iot_device_ownership_rule_cleanup","iot_device_ownership_history","iot_device_ownership","iot_device"))
            admin.execute("TRUNCATE TABLE "+table);
        admin.update("INSERT INTO iot_device VALUES (31,'SN31','Lock fixture','0'),(32,'SN32','Deleted fixture','1')");
        jdbc=new JdbcTemplate(writer); repository=new DeviceOwnershipRepository(jdbc);
        tx=new TransactionTemplate(new DataSourceTransactionManager(writer));
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        tx.setTimeout(15);
        actor=mock(OwnershipActor.class); when(actor.operatorId()).thenReturn(1L);
        tenants=mock(OwnershipTargetTenantValidator.class); blockers=mock(OwnershipBlockerService.class);
        when(blockers.blockers(anyLong())).thenReturn(List.of()); fence=mock(OwnershipOperationFence.class);
        ObjectProvider<OwnershipAlarmScopeCleanup> provider=mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mock(OwnershipAlarmScopeCleanup.class));
        cleanup=new OwnershipRuleCleanup(jdbc,provider);
        metadataSnapshots=mock(JetLinksOwnershipMetadataSnapshot.class);
        when(metadataSnapshots.capture(anyLong())).thenReturn("{\"deviceName\":\"Lock fixture\"}");
        service=manager(repository);
    }
    DeviceOwnershipService manager(DeviceOwnershipRepository repo) {
        return new DeviceOwnershipService(repo,actor,tenants,blockers,fence,tx,cleanup,metadataSnapshots);
    }
    @Test void selectOnlyProfileCanAssignTransferAndReleaseButCannotTakeExclusiveProfileLock() {
        DataAccessException denied=assertThrows(DataAccessException.class,()->tx.execute(status ->
            jdbc.queryForList("SELECT device_id FROM iot_device WHERE device_id=31 FOR UPDATE")));
        assertEquals(1142,sqlCode(denied));
        assertEquals(1142,sqlCode(assertThrows(DataAccessException.class,()->jdbc.update("UPDATE iot_device SET device_name=device_name WHERE 1=0"))));
        var assigned=service.assign(31L,new OwnershipChangeBo("658226",0L,"assign"));
        assertEquals(2L,assigned.getAssignmentVersion());
        var transferred=service.transfer(31L,new OwnershipChangeBo("000000",2L,"transfer"));
        assertEquals(4L,transferred.getAssignmentVersion());
        var released=service.release(31L,new OwnershipReleaseBo(4L,"release"));
        assertEquals(6L,released.getAssignmentVersion()); assertNull(released.getTenantId());
        assertEquals("Lock fixture",admin.queryForObject("SELECT device_name FROM iot_device WHERE device_id=31",String.class));
        assertEquals(1,count("iot_device_ownership_history","device_id=31 AND action='REGISTER'"));
    }
    @Test void sixSimultaneousFirstAssignmentsSerializeAndAuditExactlyOneWinner() throws Exception {
        for(int round=0;round<5;round++) {
            long id=1000+round;
            admin.update("INSERT INTO iot_device VALUES (?,?,'Concurrent fixture','0')",id,"SN"+id);
            CyclicBarrier allMissing=new CyclicBarrier(6);
            AtomicInteger firstReads=new AtomicInteger();
            var concurrentRepository=new DeviceOwnershipRepository(jdbc) {
                @Override public DeviceOwnershipSnapshot find(Long current) {
                    var row=super.find(current);
                    if(current==id && row==null && firstReads.incrementAndGet()<=6) {
                        try { allMissing.await(10,TimeUnit.SECONDS); }
                        catch(Exception error) { throw new AssertionError(error); }
                    }
                    return row;
                }
            };
            var concurrentService=manager(concurrentRepository);
            try(var executor=Executors.newVirtualThreadPerTaskExecutor()) {
                List<Callable<Boolean>> actions=new ArrayList<>();
                for(int n=0;n<6;n++) actions.add(()->{
                    try { concurrentService.assign(id,new OwnershipChangeBo("658226",0L,"concurrent first assignment")); return true; }
                    catch(ServiceException conflict) { return false; }
                });
                var results=executor.invokeAll(actions,20,TimeUnit.SECONDS);
                int winners=0; for(var result:results) if(result.get()) winners++;
                assertEquals(1,winners);
            }
            assertEquals(2L,repository.find(id).getAssignmentVersion());
            assertEquals("ACTIVE",repository.find(id).getFenceStatus());
            assertEquals(1,count("iot_device_ownership_history","device_id="+id+" AND action='REGISTER'"));
            assertEquals(2,count("iot_device_ownership_history","device_id="+id));
            verify(fence,times(1)).set(id,1L,true,1L); verify(fence,times(1)).set(id,2L,false,1L);
        }
    }
    @Test void profileShareLockBlocksConcurrentArchiveUntilOwnershipTransactionEnds() throws Exception {
        CountDownLatch locked=new CountDownLatch(1),release=new CountDownLatch(1),archiveStarted=new CountDownLatch(1);
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()) {
            var holder=executor.submit(()->tx.executeWithoutResult(status->{
                repository.lockForAssignment(31L,1L,null,"share lock fixture"); locked.countDown();
                try { if(!release.await(10,TimeUnit.SECONDS)) throw new AssertionError("release timeout"); }
                catch(InterruptedException error) { throw new AssertionError(error); }
            }));
            assertTrue(locked.await(5,TimeUnit.SECONDS));
            var archive=executor.submit(()->{archiveStarted.countDown();return admin.update("UPDATE iot_device SET del_flag='1' WHERE device_id=31");});
            try {
                assertTrue(archiveStarted.await(5,TimeUnit.SECONDS));
                assertThrows(TimeoutException.class,()->archive.get(250,TimeUnit.MILLISECONDS));
            } finally { release.countDown(); }
            holder.get(5,TimeUnit.SECONDS); assertEquals(1,archive.get(5,TimeUnit.SECONDS));
        }
        assertThrows(ServiceException.class,()->service.assign(31L,new OwnershipChangeBo("658226",0L,"archived")));
    }
    @Test void blockedFirstAssignmentRollsBackBothCurrentAndRegisterBeforeRetry() {
        when(blockers.blockers(31L)).thenReturn(List.of(new OwnershipBlockerVo("TASK",1,"pending")));
        assertThrows(ServiceException.class,()->service.assign(31L,new OwnershipChangeBo("658226",0L,"blocked")));
        assertNull(repository.find(31L)); assertEquals(0,count("iot_device_ownership_history","device_id=31"));
        verifyNoInteractions(fence);
        when(blockers.blockers(31L)).thenReturn(List.of());
        service.assign(31L,new OwnershipChangeBo("658226",0L,"retry after clearing blocker"));
        assertEquals(1,count("iot_device_ownership_history","device_id=31 AND action='REGISTER'"));
    }
    @Test void failedRegisterAuditRollsBackCurrentAndReadsNeverInitialize() {
        service.pool(0,200); service.assigned(0,200,null); service.history(31L,-1,50); service.current(31L); service.blockers(31L);
        assertThrows(ServiceException.class,()->service.retryFence(31L,new OwnershipReleaseBo(0L,"no record")));
        assertNull(repository.find(31L)); assertEquals(0,count("iot_device_ownership_history","1=1"));
        // A pre-existing conflicting audit must fail closed, not overwrite history or commit registration.
        admin.update("INSERT INTO iot_device_ownership_history(device_id,assignment_version,effective_from,action,reason) VALUES (31,0,CURRENT_TIMESTAMP(3),'REGISTER','orphan fixture')");
        assertThrows(DataAccessException.class,()->service.assign(31L,new OwnershipChangeBo("658226",0L,"audit conflict")));
        assertNull(repository.find(31L)); assertEquals(1,count("iot_device_ownership_history","device_id=31"));
        verifyNoInteractions(fence);
    }
    static int count(String table,String where) { return admin.queryForObject("SELECT COUNT(*) FROM "+table+" WHERE "+where,Integer.class); }
    static int sqlCode(Throwable error) {
        for(Throwable cause=error;cause!=null;cause=cause.getCause()) if(cause instanceof SQLException sql) return sql.getErrorCode();
        throw new AssertionError("No SQL exception",error);
    }
}
