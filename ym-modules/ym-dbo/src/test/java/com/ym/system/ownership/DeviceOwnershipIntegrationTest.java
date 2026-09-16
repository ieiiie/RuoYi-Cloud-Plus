package com.ym.system.ownership;

import com.ym.common.core.exception.ServiceException;
import com.ym.system.ownership.dao.DeviceOwnershipRepository;
import com.ym.system.ownership.model.*;
import com.ym.system.ownership.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Isolated in-memory database only. H2 is supplied by the ownership test harness, never a runtime datasource. */
@Tag("ownership")
class DeviceOwnershipIntegrationTest {
    JdbcTemplate jdbc;
    DeviceOwnershipRepository repository;
    OwnershipActor actor;
    OwnershipTargetTenantValidator tenants;
    OwnershipOperationFence fence;
    DeviceOwnershipService service;
    TransactionTemplate tx;
    OwnershipBlockerService blockerService;
    OwnershipAlarmScopeCleanup cleanupHelper;
    OwnershipRuleCleanup cleanup;
    JetLinksOwnershipMetadataSnapshot metadataSnapshots;
    java.sql.Connection schemaConnection;

    @BeforeEach @SuppressWarnings("unchecked") void setup() throws Exception {
        var ds=new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=3000","sa","");
        jdbc=new JdbcTemplate(ds) {
            @Override public <T> List<T> queryForList(String sql,Class<T> type,Object... args) {
                // H2 has no SHARE syntax. Keep this profile read unlocked so it cannot hide a
                // missing ownership-row lock; real MySQL tests verify the actual SHARE lock/grants.
                return super.queryForList(sql.replace(" FOR SHARE", ""),type,args);
            }
        };
        // H2 2.4 CHECK ... IN caches the DDL session as its constant-set comparator.
        // Keep that session alive while independent business/transaction connections use the schema.
        schemaConnection=ds.getConnection();
        // Execute the production DDL, including PK/check constraints, twice to verify idempotent creation.
        String ddl=Files.readString(migration()).split("DROP PROCEDURE")[0]
            .replaceAll("(?m)^--.*$","").replace("USE `ym-iot`;","").replace(" COLLATE utf8mb4_bin","")
            .replace(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4","");
        for(int pass=0;pass<2;pass++) org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(schemaConnection,
            new org.springframework.core.io.support.EncodedResource(new org.springframework.core.io.ByteArrayResource(ddl.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
        jdbc.execute("CREATE TABLE iot_device(device_id BIGINT PRIMARY KEY,device_code VARCHAR(40),device_name VARCHAR(80),tenant_id VARCHAR(20),del_flag CHAR(1))");
        jdbc.execute("CREATE SCHEMA agriculture");
        jdbc.execute("CREATE TABLE agriculture.sf_field_iot(id BIGINT PRIMARY KEY,tenant_id VARCHAR(20),field_id BIGINT,device_sn VARCHAR(40),del_flag CHAR(1))");
        jdbc.execute("CREATE TABLE iot_motorvalve_valve_session(id BIGINT PRIMARY KEY,device_id BIGINT,tenant_id VARCHAR(20),status VARCHAR(20),close_time TIMESTAMP)");
        jdbc.execute("CREATE TABLE iot_motorvalve_control_log(id BIGINT PRIMARY KEY,device_id BIGINT,tenant_id VARCHAR(20),status VARCHAR(20))");
        jdbc.execute("CREATE TABLE iot_fertilizer_control_log(log_id BIGINT PRIMARY KEY,device_id BIGINT,tenant_id VARCHAR(20),status VARCHAR(20),end_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE iot_jetlinks_command_task(request_id VARCHAR(60) PRIMARY KEY,device_id BIGINT,tenant_id VARCHAR(20),assignment_version BIGINT,state VARCHAR(30))");
        jdbc.execute("CREATE TABLE business_record(id BIGINT PRIMARY KEY,device_id BIGINT,tenant_id VARCHAR(20),record_time TIMESTAMP,payload VARCHAR(40))");
        for(int i=1;i<=30;i++) jdbc.update("INSERT INTO iot_device VALUES (?,?,?,?, '0')",i,"SN"+i,"Device "+i,i==30?"000000":"658226");
        for(int i=1;i<=4;i++) jdbc.update("INSERT INTO agriculture.sf_field_iot VALUES (?,'658226',?,?,'0')",i,i,"SN"+i);
        // Use the exact two initialization INSERTs from the migration, not a duplicate Java implementation.
        String source=Files.readString(migration());
        int first=source.indexOf("    INSERT INTO iot_device_ownership (device_id");
        int end=source.indexOf("    IF (SELECT COUNT(*) FROM iot_device_ownership) <> 30",first);
        script(source.substring(first,end));
        schemaConnection.createStatement().execute("ALTER TABLE iot_device_ownership_history ADD operator_source VARCHAR(20) DEFAULT 'BUSINESS'");
        schemaConnection.createStatement().execute("ALTER TABLE iot_device_ownership_rule_cleanup ADD operator_source VARCHAR(20) DEFAULT 'BUSINESS'");
        schemaConnection.createStatement().execute("ALTER TABLE iot_device_ownership_history ADD metadata_snapshot LONGTEXT NULL");
        repository=new DeviceOwnershipRepository(jdbc);
        actor=mock(OwnershipActor.class); when(actor.operatorId()).thenReturn(1L);
        tenants=mock(OwnershipTargetTenantValidator.class); fence=mock(OwnershipOperationFence.class);
        tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        blockerService=mock(OwnershipBlockerService.class);
        when(blockerService.blockers(anyLong())).thenReturn(List.of());
        cleanupHelper=mock(OwnershipAlarmScopeCleanup.class);
        ObjectProvider<OwnershipAlarmScopeCleanup> cleanupProvider=mock(ObjectProvider.class);
        when(cleanupProvider.getIfAvailable()).thenReturn(cleanupHelper);
        cleanup=new OwnershipRuleCleanup(jdbc,cleanupProvider);
        metadataSnapshots=mock(JetLinksOwnershipMetadataSnapshot.class);
        when(metadataSnapshots.capture(anyLong())).thenReturn("{\"deviceName\":\"Old owner display\"}");
        service=new DeviceOwnershipService(repository,actor,tenants,blockerService,fence,tx,cleanup,metadataSnapshots);
    }
    @AfterEach void closeSchemaSession() throws Exception {
        if(schemaConnection!=null) {
            try(var statement=schemaConnection.createStatement()) { statement.execute("SHUTDOWN"); }
            finally { schemaConnection.close(); }
        }
    }
    void script(String sql) {
        jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) connection -> {
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,
                new org.springframework.core.io.support.EncodedResource(new org.springframework.core.io.ByteArrayResource(sql.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
            return null;
        });
    }
    static Path migration() {
        Path p=Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while(p!=null) { Path f=p.resolve("script/sql/jetlinks-ownership.sql"); if(Files.exists(f)) return f; p=p.getParent(); }
        throw new IllegalStateException("Migration file not found");
    }
    @Test void copiesExactSourceOwnershipAndPreservesAllFourBindings() {
        assertEquals(29,jdbc.queryForObject("SELECT COUNT(*) FROM iot_device_ownership WHERE tenant_id='658226'",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM iot_device_ownership WHERE tenant_id='000000'",Integer.class));
        assertEquals(4,jdbc.queryForObject("SELECT COUNT(*) FROM agriculture.sf_field_iot",Integer.class));
        assertEquals(30,jdbc.queryForObject("SELECT COUNT(*) FROM iot_device_ownership_history",Integer.class));
        assertTrue(repository.find(10L).getEffectiveFrom().isBefore(Instant.parse("1970-01-01T00:00:00Z")));
    }
    @Test void physicalUniquenessAndChecksRejectOverwritingOrInvalidOwnership() {
        assertThrows(RuntimeException.class,()->jdbc.update("INSERT INTO iot_device_ownership VALUES (10,'999999',1,CURRENT_TIMESTAMP,'ACTIVE')"));
        assertThrows(RuntimeException.class,()->jdbc.update("UPDATE iot_device_ownership SET assignment_version=-1 WHERE device_id=10"));
        assertThrows(RuntimeException.class,()->jdbc.update("UPDATE iot_device_ownership SET fence_status='BOGUS' WHERE device_id=10"));
        assertEquals("658226",repository.find(10L).getTenantId());
    }
    @Test void realTransferClosesOldIntervalAndKeepsBusinessHistory() {
        jdbc.update("INSERT INTO business_record VALUES (1,10,'658226','2020-01-01','old history')");
        doAnswer(inv -> {
            var row=repository.find(10L);
            if((boolean)inv.getArgument(2)) { assertEquals("FROZEN",row.getFenceStatus()); assertEquals("658226",row.getTenantId()); }
            else { assertEquals("SYNC_PENDING",row.getFenceStatus()); assertEquals("000000",row.getTenantId()); }
            return null;
        }).when(fence).set(eq(10L),anyLong(),anyBoolean(),eq(1L));
        when(metadataSnapshots.capture(10L)).thenAnswer(inv -> {
            var row=repository.find(10L);
            assertEquals("FROZEN",row.getFenceStatus()); assertEquals("658226",row.getTenantId());
            verify(fence).set(10L,2L,true,1L);
            assertTrue(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());
            return "{\"deviceName\":\"Old owner display\"}";
        });
        var result=service.transfer(10L,new OwnershipChangeBo("000000",1L,"approved transfer"));
        assertEquals("000000",result.getTenantId()); assertEquals(3L,result.getAssignmentVersion()); assertEquals("ACTIVE",result.getFenceStatus());
        var history=service.history(10L,0,50); assertEquals(2,history.size());
        assertEquals(history.get(1).effectiveFrom(),history.get(0).effectiveTo());
        assertEquals("658226",history.get(1).previousTenantId());
        assertEquals("DBO",history.get(1).operatorSource()); assertNull(history.get(1).operatorTenantId());
        assertEquals("658226",jdbc.queryForObject("SELECT tenant_id FROM business_record WHERE id=1",String.class));
        assertEquals("658226",jdbc.queryForObject("SELECT tenant_id FROM iot_device WHERE device_id=10",String.class));
        assertEquals("{\"deviceName\":\"Old owner display\"}",jdbc.queryForObject(
            "SELECT metadata_snapshot FROM iot_device_ownership_history WHERE device_id=10 AND assignment_version=1",String.class));
        assertNull(jdbc.queryForObject(
            "SELECT metadata_snapshot FROM iot_device_ownership_history WHERE device_id=10 AND assignment_version=3",String.class));
    }
    @Test void snapshotFailureKeepsIntervalOpenAndRecoveryMustCaptureBeforeClosing() {
        when(metadataSnapshots.capture(10L)).thenThrow(new ServiceException("快照不可用"));
        assertThrows(ServiceException.class,()->service.transfer(10L,new OwnershipChangeBo("000000",1L,"capture outage")));
        assertEquals("FROZEN",repository.find(10L).getFenceStatus());
        assertEquals("658226",repository.find(10L).getTenantId());
        assertEquals(1L,repository.find(10L).getAssignmentVersion());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM iot_device_ownership_history WHERE device_id=10 AND effective_to IS NULL AND metadata_snapshot IS NULL",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM iot_device_ownership_rule_cleanup WHERE device_id=10",Integer.class));
        assertThrows(ServiceException.class,()->service.retryFence(10L,new OwnershipReleaseBo(1L,"still unavailable")));
        assertEquals("FROZEN",repository.find(10L).getFenceStatus());
        verify(fence,never()).set(10L,3L,false,1L);
        doReturn("{\"deviceName\":\"Recovered old owner\"}").when(metadataSnapshots).capture(10L);
        var recovered=service.retryFence(10L,new OwnershipReleaseBo(1L,"RPC recovered"));
        assertEquals("658226",recovered.getTenantId()); assertEquals("ACTIVE",recovered.getFenceStatus());
        assertEquals("{\"deviceName\":\"Recovered old owner\"}",jdbc.queryForObject(
            "SELECT metadata_snapshot FROM iot_device_ownership_history WHERE device_id=10 AND assignment_version=1",String.class));
        assertNull(jdbc.queryForObject("SELECT metadata_snapshot FROM iot_device_ownership_history WHERE device_id=10 AND assignment_version=3",String.class));
    }
    @Test void downstreamFailureRollsBackSnapshotHistoryAndOwnershipTogether() {
        var failingCleanup=spy(cleanup);
        doThrow(new ServiceException("cleanup enqueue failed")).when(failingCleanup).enqueue(any(),anyLong(),anyLong());
        var failingService=new DeviceOwnershipService(repository,actor,tenants,blockerService,fence,tx,failingCleanup,metadataSnapshots);
        assertThrows(ServiceException.class,()->failingService.transfer(10L,new OwnershipChangeBo("000000",1L,"rollback")));
        verify(metadataSnapshots).capture(10L);
        assertEquals("FROZEN",repository.find(10L).getFenceStatus());
        assertEquals("658226",repository.find(10L).getTenantId());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM iot_device_ownership_history WHERE device_id=10",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM iot_device_ownership_history WHERE device_id=10 AND effective_to IS NULL AND metadata_snapshot IS NULL",Integer.class));
    }
    @Test void laterTransferDoesNotRewriteEarlierSnapshot() {
        service.transfer(10L,new OwnershipChangeBo("000000",1L,"first transfer"));
        when(metadataSnapshots.capture(10L)).thenReturn("{\"deviceName\":\"Next owner display\"}");
        service.transfer(10L,new OwnershipChangeBo("999999",3L,"next transfer"));
        assertEquals("{\"deviceName\":\"Old owner display\"}",jdbc.queryForObject(
            "SELECT metadata_snapshot FROM iot_device_ownership_history WHERE device_id=10 AND assignment_version=1",String.class));
        assertEquals("{\"deviceName\":\"Next owner display\"}",jdbc.queryForObject(
            "SELECT metadata_snapshot FROM iot_device_ownership_history WHERE device_id=10 AND assignment_version=3",String.class));
    }
    @Test void releaseRetainsVersionThenAssignmentIncrementsItAgain() {
        var released=service.release(10L,new OwnershipReleaseBo(1L,"release"));
        assertNull(released.getTenantId()); assertEquals(3L,released.getAssignmentVersion());
        verify(fence).set(10L,3L,true,1L);
        var assigned=service.assign(10L,new OwnershipChangeBo("999999",3L,"new tenant"));
        assertEquals(5L,assigned.getAssignmentVersion()); assertEquals("999999",assigned.getTenantId());
    }
    @Test void firstAssignmentReservesFreezeVersionThenPublishesVersionTwo() {
        jdbc.update("INSERT INTO iot_device VALUES (31,'SN31','New','658226','0')");
        var result=service.assign(31L,new OwnershipChangeBo("999999",0L,"assign new"));
        assertEquals(2L,result.getAssignmentVersion()); assertEquals("999999",result.getTenantId());
        verify(fence).set(31L,1L,true,1L); verify(fence).set(31L,2L,false,1L);
        verifyNoInteractions(metadataSnapshots);
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(metadata_snapshot) FROM iot_device_ownership_history WHERE device_id=31",Integer.class));
    }
    @Test void staleOrMissingVersionFailsBeforeAnyRemoteEffect() {
        assertThrows(ServiceException.class,()->service.transfer(10L,new OwnershipChangeBo("000000",0L,"stale")));
        assertThrows(ServiceException.class,()->service.transfer(10L,new OwnershipChangeBo("000000",null,"missing")));
        verifyNoInteractions(fence); assertEquals(1L,repository.find(10L).getAssignmentVersion());
    }
    @Test void assigningOwnedAndTransferringToSameTenantAreNotFakeSuccess() {
        assertThrows(ServiceException.class,()->service.assign(10L,new OwnershipChangeBo("000000",1L,"wrong action")));
        assertThrows(ServiceException.class,()->service.transfer(10L,new OwnershipChangeBo("658226",1L,"same tenant")));
        verifyNoInteractions(fence);
    }
    @Test void remoteFreezeFailurePersistsFrozenStateAndCanRecoverWithoutChangingTenant() {
        doThrow(new ServiceException("timeout")).when(fence).set(10L,2L,true,1L);
        assertThrows(ServiceException.class,()->service.transfer(10L,new OwnershipChangeBo("000000",1L,"timeout")));
        assertEquals("FROZEN",repository.find(10L).getFenceStatus()); assertEquals("658226",repository.find(10L).getTenantId());
        reset(fence); var recovered=service.retryFence(10L,new OwnershipReleaseBo(1L,"recover"));
        assertEquals("ACTIVE",recovered.getFenceStatus()); assertEquals(3L,recovered.getAssignmentVersion());
    }
    @Test void remoteUnfreezeFailureKeepsCommittedHistoryAndRetriesOnlyFence() {
        doThrow(new ServiceException("timeout")).when(fence).set(10L,3L,false,1L);
        assertThrows(ServiceException.class,()->service.transfer(10L,new OwnershipChangeBo("000000",1L,"timeout")));
        assertEquals("SYNC_PENDING",repository.find(10L).getFenceStatus()); assertEquals("000000",repository.find(10L).getTenantId());
        assertEquals(2,repository.history(10L,0,50).size());
        reset(fence); assertEquals("ACTIVE",service.retryFence(10L,new OwnershipReleaseBo(3L,"recover")).getFenceStatus());
        assertEquals(2,repository.history(10L,0,50).size()); verify(fence).set(10L,3L,false,1L);
    }
    @Test void newBlockerAfterRemoteFreezeKeepsOldTenantAndRequiresExplicitRecovery() {
        doAnswer(inv->{when(blockerService.blockers(10L)).thenReturn(List.of(new OwnershipBlockerVo("VALVE_PENDING",1,"pending")));return null;}).when(fence).set(10L,2L,true,1L);
        assertThrows(ServiceException.class,()->service.transfer(10L,new OwnershipChangeBo("000000",1L,"race")));
        assertEquals("658226",repository.find(10L).getTenantId()); assertEquals("FROZEN",repository.find(10L).getFenceStatus());
    }
    @Test void historyWriteConflictRollsBackOwnershipButLeavesDurableFreeze() {
        jdbc.update("DELETE FROM iot_device_ownership_history WHERE device_id=10");
        assertThrows(ServiceException.class,()->service.transfer(10L,new OwnershipChangeBo("000000",1L,"broken history")));
        assertEquals("658226",repository.find(10L).getTenantId()); assertEquals(1L,repository.find(10L).getAssignmentVersion());
        assertEquals("FROZEN",repository.find(10L).getFenceStatus());
    }
    @Test void adminAuthorizationAndTargetValidationHappenBeforeWrites() {
        doThrow(new ServiceException("not admin")).when(actor).requirePermission(anyString());
        assertThrows(ServiceException.class,()->service.pool(0,50));
        assertThrows(ServiceException.class,()->service.transfer(10L,new OwnershipChangeBo("000000",1L,"forged")));
        verifyNoInteractions(fence); assertEquals("ACTIVE",repository.find(10L).getFenceStatus());
    }
    @Test void rejectedTargetTenantCannotMutateOwnership() {
        doThrow(new ServiceException("invalid tenant")).when(tenants).requireActive("bad");
        assertThrows(ServiceException.class,()->service.transfer(10L,new OwnershipChangeBo("bad",1L,"bad target")));
        verifyNoInteractions(fence); assertEquals("658226",repository.find(10L).getTenantId());
    }
    @Test void sameTenantReassignmentStillInvalidatesOldVersion() {
        service.transfer(10L,new OwnershipChangeBo("000000",1L,"away"));
        service.transfer(10L,new OwnershipChangeBo("658226",3L,"back"));
        assertEquals(3,repository.history(10L,0,50).size());
    }
    @Test void concurrentFirstAssignmentsCannotBothSucceed() throws Exception {
        jdbc.update("INSERT INTO iot_device VALUES (31,'SN31','New','658226','0')");
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()) {
            Callable<Boolean> action=()->{ try { service.assign(31L,new OwnershipChangeBo("999999",0L,"race")); return true; } catch(ServiceException ex){return false;} };
            var results=executor.invokeAll(List.of(action,action));
            assertEquals(1,results.stream().filter(f->{try{return f.get();}catch(Exception e){throw new RuntimeException(e);}}).count());
        }
        assertEquals(2L,repository.find(31L).getAssignmentVersion());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM iot_device_ownership_history WHERE device_id=31 AND action='REGISTER'",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM iot_device_ownership_history WHERE device_id=31",Integer.class));
    }
    @Test void readsAndRetryOfUnregisteredDeviceNeverCreateOwnershipOrAudit() {
        jdbc.update("INSERT INTO iot_device VALUES (31,'SN31','New','658226','0')");
        service.pool(0,200); service.assigned(0,200,null); service.history(31L,-1,50);
        service.blockers(31L); assertNull(service.current(31L));
        assertThrows(ServiceException.class,()->service.retryFence(31L,new OwnershipReleaseBo(0L,"not registered")));
        assertNull(repository.find(31L));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM iot_device_ownership_history WHERE device_id=31",Integer.class));
    }
    @Test void rejectedFirstAssignmentRollsBackRegistrationAndAudit() {
        jdbc.update("INSERT INTO iot_device VALUES (31,'SN31','New','658226','0')");
        when(blockerService.blockers(31L)).thenReturn(List.of(new OwnershipBlockerVo("TASK",1,"busy")));
        assertThrows(ServiceException.class,()->service.assign(31L,new OwnershipChangeBo("999999",0L,"blocked")));
        assertNull(repository.find(31L));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM iot_device_ownership_history WHERE device_id=31",Integer.class));
        verifyNoInteractions(fence);
    }
    @Test void failedRegisterAuditRollsBackNewCurrentRow() {
        jdbc.update("INSERT INTO iot_device VALUES (31,'SN31','New','658226','0')");
        jdbc.execute("ALTER TABLE iot_device_ownership_history ADD CONSTRAINT reject_register CHECK (action <> 'REGISTER')");
        assertThrows(RuntimeException.class,()->service.assign(31L,new OwnershipChangeBo("999999",0L,"audit fails")));
        assertNull(repository.find(31L)); verifyNoInteractions(fence);
    }
    @Test void lockRequiresPhysicalWriteTransactionAndDeletedProfileCannotRegister() {
        jdbc.update("INSERT INTO iot_device VALUES (31,'SN31','Deleted','658226','1')");
        assertThrows(IllegalStateException.class,()->repository.lockForAssignment(31L,1L,null,"no tx"));
        assertThrows(ServiceException.class,()->service.assign(31L,new OwnershipChangeBo("999999",0L,"deleted")));
        assertThrows(ServiceException.class,()->service.assign(32L,new OwnershipChangeBo("999999",0L,"missing")));
        assertNull(repository.find(31L)); assertNull(repository.find(32L));
    }
    @Test void localInitializationDeadlockRetriesAfterRollbackWithoutRepeatingRemoteFence() {
        jdbc.update("INSERT INTO iot_device VALUES (31,'SN31','New','658226','0')");
        when(blockerService.blockers(31L))
            .thenThrow(new org.springframework.dao.CannotAcquireLockException("deadlock",new java.sql.SQLException("deadlock","40001",1213)))
            .thenReturn(List.of());
        var result=service.assign(31L,new OwnershipChangeBo("999999",0L,"retry local transaction"));
        assertEquals(2L,result.getAssignmentVersion());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM iot_device_ownership_history WHERE device_id=31 AND action='REGISTER'",Integer.class));
        verify(fence,times(1)).set(31L,1L,true,1L); verify(fence,times(1)).set(31L,2L,false,1L);
    }
    @Test void repeatedLocalDeadlockIsBoundedAndNeverLeavesRegistrationOrRemoteEffects() {
        jdbc.update("INSERT INTO iot_device VALUES (31,'SN31','New','658226','0')");
        when(blockerService.blockers(31L)).thenThrow(new org.springframework.dao.CannotAcquireLockException(
            "deadlock",new java.sql.SQLException("deadlock","40001",1213)));
        assertThrows(org.springframework.dao.CannotAcquireLockException.class,
            ()->service.assign(31L,new OwnershipChangeBo("999999",0L,"bounded retry")));
        verify(blockerService,times(4)).blockers(31L);
        assertNull(repository.find(31L));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM iot_device_ownership_history WHERE device_id=31",Integer.class));
        verifyNoInteractions(fence);
    }
    @Test void remoteStageDeadlockIsNotAutomaticallyRetried() {
        jdbc.update("INSERT INTO iot_device VALUES (31,'SN31','New','658226','0')");
        doThrow(new org.springframework.dao.CannotAcquireLockException("remote SQL failure",
            new java.sql.SQLException("deadlock","40001",1213))).when(fence).set(31L,1L,true,1L);
        assertThrows(ServiceException.class,()->service.assign(31L,new OwnershipChangeBo("999999",0L,"remote stage")));
        verify(fence,times(1)).set(31L,1L,true,1L); verify(fence,never()).set(31L,2L,false,1L);
        assertEquals("FROZEN",repository.find(31L).getFenceStatus()); assertNull(repository.find(31L).getTenantId());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM iot_device_ownership_history WHERE device_id=31 AND action='REGISTER'",Integer.class));
    }
    static class StrictFence implements OwnershipOperationFence {
        long version=1; boolean frozen=false; boolean loseFreezeReply; boolean loseUnfreezeReply;
        @Override public synchronized void set(Long id,long next,boolean freeze,Long operator) {
            if(next<version || (next==version && freeze!=frozen)) throw new ServiceException("stale or conflicting fence");
            version=next;frozen=freeze;
            if(freeze && loseFreezeReply) { loseFreezeReply=false; throw new ServiceException("freeze applied but reply lost"); }
            if(!freeze && loseUnfreezeReply) { loseUnfreezeReply=false; throw new ServiceException("unfreeze applied but reply lost"); }
        }
    }
    DeviceOwnershipService strictService(StrictFence guard) {
        return new DeviceOwnershipService(repository,actor,tenants,blockerService,guard,tx,cleanup,metadataSnapshots);
    }
    @Test void strictMonotonicFenceRejectsDelayedFreezeAfterSuccessfulTransfer() {
        var guard=new StrictFence(); var manager=strictService(guard);
        var current=manager.transfer(10L,new OwnershipChangeBo("000000",1L,"transfer"));
        assertEquals(3L,current.getAssignmentVersion());assertEquals(3L,guard.version);assertFalse(guard.frozen);
        assertThrows(ServiceException.class,()->guard.set(10L,2L,true,1L));
        assertThrows(ServiceException.class,()->guard.set(10L,3L,true,1L));
        assertEquals(3L,guard.version);assertFalse(guard.frozen);
    }
    @Test void frozenRecoveryAdvancesVersionAndRejectsLateFreezeWithoutChangingTenant() {
        var guard=new StrictFence();guard.loseFreezeReply=true;var manager=strictService(guard);
        assertThrows(ServiceException.class,()->manager.transfer(10L,new OwnershipChangeBo("000000",1L,"uncertain freeze")));
        assertTrue(guard.frozen);assertEquals(2L,guard.version);
        var current=manager.retryFence(10L,new OwnershipReleaseBo(1L,"recover old tenant"));
        assertEquals("658226",current.getTenantId());assertEquals(3L,current.getAssignmentVersion());
        assertEquals("RECOVER",repository.history(10L,0,50).getLast().action());
        assertFalse(guard.frozen);assertEquals(3L,guard.version);
        assertThrows(ServiceException.class,()->guard.set(10L,2L,true,1L));
    }
    @Test void uncertainFinalReplyRetriesSameVersionAndStateIdempotently() {
        var guard=new StrictFence();guard.loseUnfreezeReply=true;var manager=strictService(guard);
        assertThrows(ServiceException.class,()->manager.transfer(10L,new OwnershipChangeBo("000000",1L,"uncertain unfreeze")));
        assertEquals(3L,guard.version);assertFalse(guard.frozen);assertEquals("SYNC_PENDING",repository.find(10L).getFenceStatus());
        assertEquals("ACTIVE",manager.retryFence(10L,new OwnershipReleaseBo(3L,"retry same state")).getFenceStatus());
        assertEquals(3L,guard.version);assertEquals(2,repository.history(10L,0,50).size());
    }


    @Test void ruleCleanupFailureStaysPendingAndRetryMustFinishBeforeUnfreeze() {
        doThrow(new ServiceException("scope CAS conflict")).doNothing().when(cleanupHelper)
            .removeDevice(eq(10L),eq("658226"),eq(1L),eq(3L),anyString(),eq("dbo:1"));
        assertThrows(ServiceException.class,()->service.transfer(10L,new OwnershipChangeBo("000000",1L,"transfer")));
        assertEquals("SYNC_PENDING",repository.find(10L).getFenceStatus());
        assertEquals("000000",repository.find(10L).getTenantId());
        verify(fence,never()).set(10L,3L,false,1L);
        assertEquals("PENDING",jdbc.queryForObject("SELECT status FROM iot_device_ownership_rule_cleanup WHERE device_id=10",String.class));
        assertEquals(1,jdbc.queryForObject("SELECT attempts FROM iot_device_ownership_rule_cleanup WHERE device_id=10",Integer.class));
        service.retryFence(10L,new OwnershipReleaseBo(3L,"retry"));
        assertEquals("ACTIVE",repository.find(10L).getFenceStatus());
        assertEquals("DONE",jdbc.queryForObject("SELECT status FROM iot_device_ownership_rule_cleanup WHERE device_id=10",String.class));
        var ids=org.mockito.ArgumentCaptor.forClass(String.class);
        verify(cleanupHelper,times(2)).removeDevice(eq(10L),eq("658226"),eq(1L),eq(3L),ids.capture(),eq("dbo:1"));
        assertEquals(ids.getAllValues().get(0),ids.getAllValues().get(1));
        var order=inOrder(cleanupHelper,fence); order.verify(fence).set(10L,2L,true,1L);
        order.verify(cleanupHelper,times(2)).removeDevice(eq(10L),eq("658226"),eq(1L),eq(3L),anyString(),eq("dbo:1"));
        order.verify(fence).set(10L,3L,false,1L);
    }
    @Test void completedCleanupSurvivesFailedUnfreeze() {
        doThrow(new ServiceException("fence unavailable")).doNothing().when(fence).set(10L,3L,false,1L);
        assertThrows(ServiceException.class,()->service.transfer(10L,new OwnershipChangeBo("000000",1L,"transfer")));
        assertEquals("DONE",jdbc.queryForObject("SELECT status FROM iot_device_ownership_rule_cleanup WHERE device_id=10",String.class));
        service.retryFence(10L,new OwnershipReleaseBo(3L,"retry"));
        verify(cleanupHelper,times(1)).removeDevice(eq(10L),eq("658226"),eq(1L),eq(3L),anyString(),eq("dbo:1"));
    }
    @Test void missingCleanupCannotBeBypassedByRetryFence() {
        doThrow(new ServiceException("pause")).when(cleanupHelper).removeDevice(anyLong(),anyString(),anyLong(),anyLong(),anyString(),anyString());
        assertThrows(ServiceException.class,()->service.release(10L,new OwnershipReleaseBo(1L,"release")));
        jdbc.update("DELETE FROM iot_device_ownership_rule_cleanup WHERE device_id=10");
        assertThrows(ServiceException.class,()->service.retryFence(10L,new OwnershipReleaseBo(3L,"retry")));
        assertEquals("SYNC_PENDING",repository.find(10L).getFenceStatus());
        verify(fence,never()).set(10L,3L,true,1L);
    }
    @Test void failedFreezeRecoveryDoesNotCleanRulesOfUnchangedOwner() {
        doThrow(new ServiceException("sent command")).when(fence).set(10L,2L,true,1L);
        assertThrows(ServiceException.class,()->service.transfer(10L,new OwnershipChangeBo("000000",1L,"transfer")));
        service.retryFence(10L,new OwnershipReleaseBo(1L,"recover"));
        verifyNoInteractions(cleanupHelper);
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM iot_device_ownership_rule_cleanup",Integer.class));
        assertEquals("658226",repository.find(10L).getTenantId());
    }
}
