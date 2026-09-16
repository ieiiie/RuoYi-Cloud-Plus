package com.ym.system.resource.dubbo;

import com.alibaba.fastjson2.JSON;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.mybatis.utils.IdGeneratorUtil;
import com.ym.resource.api.RemoteInboxService;
import com.ym.system.dubbo.RemoteUserServiceImpl;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Message receipt and deduplication commit together on the physical business datasource. */
@Service
@DubboService(version="2.0.0",retries=0)
public class RemoteInboxServiceImpl implements RemoteInboxService {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final RemoteUserServiceImpl users;
    public RemoteInboxServiceImpl(DataSource dataSource,RemoteUserServiceImpl users) {
        DataSource physical=dataSource instanceof DynamicRoutingDataSource routing ? routing.getDataSources().get("master") : dataSource;
        if(physical==null)throw new IllegalStateException("Missing inbox master datasource");
        this.jdbc=new JdbcTemplate(physical);this.users=users;
        this.transaction=new TransactionTemplate(new DataSourceTransactionManager(physical));
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        this.transaction.setTimeout(10);
    }
    @Override public boolean receive(String deliveryId,String tenantId,List<Long> recipientIds,String title,String content,Map<String,Object> metadata) {
        if(deliveryId==null || !deliveryId.matches("[a-f0-9]{64}") || tenantId==null || !tenantId.matches("[A-Za-z0-9_-]{1,20}")
            || recipientIds==null || recipientIds.isEmpty() || recipientIds.size()>100 || title==null || title.isBlank() || title.length()>100
            || content==null || content.isBlank() || content.length()>16000 || metadata==null)
            throw new ServiceException("站内消息参数无效");
        if(recipientIds.stream().anyMatch(id->id==null || id<=0))throw new ServiceException("收件人无效");
        List<Long> recipients=recipientIds.stream().distinct().sorted().toList();
        Map<String,Object> fields=new TreeMap<>();fields.put("tenantId",tenantId);fields.put("recipientIds",recipients);
        fields.put("title",title);fields.put("content",content);fields.put("metadata",new TreeMap<>(metadata));
        String hash=sha256(JSON.toJSONString(fields));
        return Boolean.TRUE.equals(transaction.execute(status->{
            long messageId=IdGeneratorUtil.nextLongId();
            int inserted=jdbc.update("INSERT IGNORE INTO sys_inbox_delivery(delivery_id,tenant_id,payload_hash,message_id,received_at) VALUES(?,?,?,?,CURRENT_TIMESTAMP(3))",deliveryId,tenantId,hash,messageId);
            var receipt=jdbc.queryForMap("SELECT tenant_id,payload_hash,message_id FROM sys_inbox_delivery WHERE delivery_id=? FOR UPDATE",deliveryId);
            if(!tenantId.equals(receipt.get("tenant_id")) || !hash.equals(receipt.get("payload_hash")))throw new ServiceException("通知ID已被不同内容使用");
            if(inserted==0)return true;
            // A committed identical delivery stays acknowledged after membership changes.
            // New deliveries still require current concrete tenant members; failures roll back the receipt.
            for(Long userId:recipients) {
                var member=users.getUserInfo(userId,tenantId);
                if(member==null || !tenantId.equals(member.getTenantId()) || !userId.equals(member.getUserId()))
                    throw new ServiceException("收件人不属于通知租户");
            }
            jdbc.update("INSERT INTO sys_message(message_id,tenant_id,category,type,source,title,message,content,data_json,send_user_ids,create_time) VALUES(?,?,'system','message','backend',?,?,?,?,?,CURRENT_TIMESTAMP)",messageId,tenantId,title,title,content,JSON.toJSONString(metadata),String.join(",",recipients.stream().map(Object::toString).toList()));
            return true;
        }));
    }
    private static String sha256(String value) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
}
