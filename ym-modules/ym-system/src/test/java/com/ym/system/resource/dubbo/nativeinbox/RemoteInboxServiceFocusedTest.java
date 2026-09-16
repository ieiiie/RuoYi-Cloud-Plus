package com.ym.system.resource.dubbo.nativeinbox;

import com.ym.common.core.exception.ServiceException;
import cn.hutool.extra.spring.SpringUtil;
import com.ym.system.api.model.LoginUser;
import com.ym.common.mybatis.utils.IdGeneratorUtil;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.ym.system.dubbo.RemoteUserServiceImpl;
import com.ym.system.resource.dubbo.RemoteInboxServiceImpl;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.EncodedResource;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Isolated H2/MySQL-mode checks of the receipt+message transaction; never contacts real DB or users. */
class RemoteInboxServiceFocusedTest {
    static final AtomicLong IDS=new AtomicLong(1000);
    static final String DELIVERY="a".repeat(64);
    JdbcTemplate jdbc; DriverManagerDataSource dataSource; RemoteUserServiceImpl users; RemoteInboxServiceImpl service;
    @BeforeAll static void initializeIdGeneratorWithoutApplicationOrNetwork() throws Exception {
        try(var spring=mockStatic(SpringUtil.class)) {
            IdentifierGenerator generator=entity->IDS.incrementAndGet();
            spring.when(()->SpringUtil.getBean(IdentifierGenerator.class)).thenReturn(generator);
            Class.forName(IdGeneratorUtil.class.getName(),true,IdGeneratorUtil.class.getClassLoader());
        }
    }
    @BeforeEach void setup() throws Exception {
        dataSource=new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000","sa","");
        jdbc=new JdbcTemplate(dataSource); createSchema(jdbc);
        users=mock(RemoteUserServiceImpl.class);
        when(users.getUserInfo(anyLong(),anyString())).thenAnswer(i->{ var user=new LoginUser();user.setUserId(i.getArgument(0));user.setTenantId(i.getArgument(1));return user; });
        service=new RemoteInboxServiceImpl(dataSource,users);
    }
    static void createSchema(JdbcTemplate jdbc) throws Exception {
        String path=System.getProperty("native.inbox.sql");
        String source;
        if(path!=null) source=Files.readString(Path.of(path));
        else try(var stream=RemoteInboxServiceFocusedTest.class.getResourceAsStream("/db/native-inbox.sql")) {
            if(stream==null)throw new IllegalStateException("Missing inbox schema resource");
            source=new String(stream.readAllBytes(),StandardCharsets.UTF_8);
        }
        String ddl=source.replace(" CHARACTER SET ascii COLLATE ascii_bin","").replace(" ENGINE=InnoDB COMMENT='Native notification receipt and inbox deduplication'","");
        try(var connection=jdbc.getDataSource().getConnection()) {
            ScriptUtils.executeSqlScript(connection,new EncodedResource(new ByteArrayResource(ddl.getBytes(StandardCharsets.UTF_8))));
        }
        jdbc.execute("CREATE TABLE sys_message(message_id BIGINT PRIMARY KEY,tenant_id VARCHAR(20),category VARCHAR(30),type VARCHAR(30),source VARCHAR(30),title VARCHAR(100),message VARCHAR(100),content VARCHAR(16000),data_json VARCHAR(20000),send_user_ids VARCHAR(3000),create_time TIMESTAMP)");
    }
    @AfterEach void close() { jdbc.execute("SHUTDOWN"); }
    Map<String,Object> metadata() { return Map.of("alarmConfigId","native-config","targetId","10","alarmTime",1577836800500L,"phase","trigger"); }
    boolean deliver(String id,String tenant,List<Long> recipients,String body) { return service.receive(id,tenant,recipients,"Offline",body,metadata()); }
    int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class); }
    @Test void receiptAndMessageUseConcreteTenantMembersAndNoBroadcastFallback() {
        assertTrue(deliver(DELIVERY,"658226",List.of(8L,7L,8L),"offline"));
        assertEquals(1,count("sys_inbox_delivery")); assertEquals(1,count("sys_message"));
        assertEquals("7,8",jdbc.queryForObject("SELECT send_user_ids FROM sys_message",String.class));
        assertEquals("658226",jdbc.queryForObject("SELECT tenant_id FROM sys_message",String.class));
        verify(users).getUserInfo(7L,"658226"); verify(users).getUserInfo(8L,"658226");
    }
    @Test void foreignOrMismatchedMemberPreventsBothReceiptAndMessage() {
        var wrong=new LoginUser(); wrong.setUserId(7L);wrong.setTenantId("000000"); when(users.getUserInfo(7L,"658226")).thenReturn(wrong);
        assertThrows(ServiceException.class,()->deliver(DELIVERY,"658226",List.of(7L),"offline"));
        assertEquals(0,count("sys_inbox_delivery")); assertEquals(0,count("sys_message"));
        wrong.setTenantId("658226");wrong.setUserId(99L);
        assertThrows(ServiceException.class,()->deliver(DELIVERY,"658226",List.of(7L),"offline"));
    }
    @Test void missingDisabledOrRemovedMemberPreventsANewDelivery() {
        when(users.getUserInfo(7L,"658226")).thenThrow(new ServiceException("member disabled"));
        assertThrows(ServiceException.class,()->deliver(DELIVERY,"658226",List.of(7L),"offline")); assertEquals(0,count("sys_message"));
    }
    @Test void exactDuplicateCanonicalizesRecipientOrderAndCommitsOnce() {
        assertTrue(deliver(DELIVERY,"658226",List.of(8L,7L),"offline"));
        assertTrue(deliver(DELIVERY,"658226",List.of(7L,8L,8L),"offline"));
        assertEquals(1,count("sys_inbox_delivery"));assertEquals(1,count("sys_message"));
    }
    @Test void sameIdCannotChangePayloadTenantOrRecipients() {
        assertTrue(deliver(DELIVERY,"658226",List.of(7L),"original"));
        assertThrows(ServiceException.class,()->deliver(DELIVERY,"658226",List.of(7L),"changed"));
        assertThrows(ServiceException.class,()->deliver(DELIVERY,"000000",List.of(7L),"original"));
        assertThrows(ServiceException.class,()->deliver(DELIVERY,"658226",List.of(8L),"original"));
        assertEquals("original",jdbc.queryForObject("SELECT content FROM sys_message",String.class)); assertEquals(1,count("sys_message"));
    }
    @Test void failedMessageInsertRollsBackReceiptThenRetryCreatesExactlyOneMessage() {
        jdbc.execute("ALTER TABLE sys_message ALTER COLUMN content VARCHAR(3)");
        assertThrows(RuntimeException.class,()->deliver(DELIVERY,"658226",List.of(7L),"too long"));
        assertEquals(0,count("sys_inbox_delivery"));assertEquals(0,count("sys_message"));
        jdbc.execute("ALTER TABLE sys_message ALTER COLUMN content VARCHAR(16000)");
        assertTrue(deliver(DELIVERY,"658226",List.of(7L),"too long")); assertEquals(1,count("sys_message"));assertEquals(1,count("sys_inbox_delivery"));
    }
    @Test void simultaneousDuplicateDeliveriesAreSerializedByReceiptUniqueness() throws Exception {
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()) {
            var gate=new CountDownLatch(1); List<Future<Boolean>> jobs=new ArrayList<>();
            for(int i=0;i<6;i++) jobs.add(executor.submit(()->{gate.await();return deliver(DELIVERY,"658226",List.of(7L),"offline");}));
            gate.countDown(); for(var job:jobs) assertTrue(job.get(15,TimeUnit.SECONDS));
        }
        assertEquals(1,count("sys_inbox_delivery"));assertEquals(1,count("sys_message"));
    }
    @Test void constructorUsesPhysicalMasterEvenWhenRoutingPrimaryPointsElsewhere() throws Exception {
        var other=new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","");
        var otherJdbc=new JdbcTemplate(other);createSchema(otherJdbc);
        try {
            var routing=new DynamicRoutingDataSource(List.of());routing.addDataSource("master",dataSource);routing.addDataSource("saas",other);routing.setPrimary("saas");
            var receiver=new RemoteInboxServiceImpl(routing,users);
            assertTrue(receiver.receive(DELIVERY,"658226",List.of(7L),"Offline","offline",metadata()));
            assertEquals(1,count("sys_message")); assertEquals(0,otherJdbc.queryForObject("SELECT COUNT(*) FROM sys_message",Integer.class));
            assertThrows(IllegalStateException.class,()->new RemoteInboxServiceImpl(new DynamicRoutingDataSource(List.of()),users));
        } finally {otherJdbc.execute("SHUTDOWN");}
    }
    @Test void alreadyCommittedDuplicateMustRemainAcknowledgedAfterMemberRevocation() {
        assertTrue(deliver(DELIVERY,"658226",List.of(7L),"offline"));
        when(users.getUserInfo(7L,"658226")).thenThrow(new ServiceException("member removed after original delivery"));
        assertDoesNotThrow(()->assertTrue(deliver(DELIVERY,"658226",List.of(7L),"offline")),
            "An identical persisted delivery is already done; current membership must not turn it into permanent retries");
        assertEquals(1,count("sys_message"));
    }
    @Test void oneInvalidRecipientRejectsWholeMessageWithoutPartialReceipt() {
        when(users.getUserInfo(8L,"658226")).thenReturn(null);
        assertThrows(ServiceException.class,()->deliver(DELIVERY,"658226",List.of(7L,8L),"offline"));
        verify(users).getUserInfo(7L,"658226");
        assertEquals(0,count("sys_inbox_delivery"));assertEquals(0,count("sys_message"));
    }
    @Test void concurrentDifferentPayloadsCannotShareOneDeliveryId() throws Exception {
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()) {
            var gate=new CountDownLatch(1);
            List<Future<Boolean>> jobs=new ArrayList<>();
            for(String body:List.of("payload-A","payload-B")) jobs.add(executor.submit(()->{
                gate.await();
                try {return deliver(DELIVERY,"658226",List.of(7L),body);}
                catch(ServiceException expectedConflict) {return false;}
            }));
            gate.countDown();
            int successes=0;for(var job:jobs) if(job.get(15,TimeUnit.SECONDS)) successes++;
            assertEquals(1,successes);
        }
        assertEquals(1,count("sys_inbox_delivery"));assertEquals(1,count("sys_message"));
        String winner=jdbc.queryForObject("SELECT content FROM sys_message",String.class);
        assertTrue(deliver(DELIVERY,"658226",List.of(7L),winner));
    }
    @Test void receiptTransactionIsIndependentFromAnOuterCallerRollback() {
        jdbc.execute("CREATE TABLE caller_work(id BIGINT PRIMARY KEY)");
        var outer=new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource));
        outer.executeWithoutResult(status->{
            jdbc.update("INSERT INTO caller_work VALUES(1)");
            assertTrue(deliver(DELIVERY,"658226",List.of(7L),"offline"));
            status.setRollbackOnly();
        });
        assertEquals(0,count("caller_work"));assertEquals(1,count("sys_inbox_delivery"));assertEquals(1,count("sys_message"));
    }
    @Test void malformedRecipientsNeverCreateReceiptOrBroadcastMessage() {
        for(List<Long> members:List.of(List.of(0L),List.of(-1L),Arrays.asList(7L,null),List.<Long>of()))
            assertThrows(RuntimeException.class,()->deliver(DELIVERY,"658226",members,"offline"));
        assertEquals(0,count("sys_inbox_delivery"));assertEquals(0,count("sys_message"));
    }
}
