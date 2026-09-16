package com.ym.system.ownership.service;

import com.ym.common.core.exception.ServiceException;
import com.ym.system.ownership.model.DeviceOwnershipSnapshot;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;

/** Durable outbox in the physical ownership database. Called only while holding the ownership row lock. */
@Service
public class OwnershipRuleCleanup {
    private final JdbcTemplate jdbc;
    private final ObjectProvider<OwnershipAlarmScopeCleanup> helpers;
    public OwnershipRuleCleanup(@Qualifier("deviceOwnershipJdbc") JdbcTemplate jdbc,
                                ObjectProvider<OwnershipAlarmScopeCleanup> helpers) {
        this.jdbc=jdbc; this.helpers=helpers;
    }
    public void enqueue(DeviceOwnershipSnapshot previous, long publishedVersion, Long operator) {
        if(previous.getTenantId()==null) return;
        jdbc.update("INSERT INTO iot_device_ownership_rule_cleanup(device_id,assignment_version,previous_tenant_id,previous_assignment_version,request_id,operator_id,operator_source,status,attempts,created_at) VALUES (?,?,?,?,?,?,'DBO','PENDING',0,CURRENT_TIMESTAMP(3))",
            previous.getDeviceId(),publishedVersion,previous.getTenantId(),previous.getAssignmentVersion(),UUID.randomUUID().toString(),operator);
    }
    private record Pending(String tenant, long version, String requestId, Long operator, String status, String operatorSource) {}
    public void complete(DeviceOwnershipSnapshot current) {
        var rows=jdbc.query("SELECT * FROM iot_device_ownership_rule_cleanup WHERE device_id=? AND assignment_version=? FOR UPDATE",
            (rs,n)->new Pending(rs.getString("previous_tenant_id"),rs.getLong("previous_assignment_version"),rs.getString("request_id"),rs.getObject("operator_id",Long.class),rs.getString("status"),rs.getString("operator_source")),current.getDeviceId(),current.getAssignmentVersion());
        if(rows.isEmpty()) {
            // A transfer/release may never be recovered by silently skipping its durable work.
            Long required=jdbc.queryForObject("SELECT COUNT(*) FROM iot_device_ownership_history WHERE device_id=? AND assignment_version=? AND action IN ('TRANSFER','RELEASE')",Long.class,current.getDeviceId(),current.getAssignmentVersion());
            if(required!=null && required>0) throw new ServiceException("归属转出规则清理待办缺失，保持SYNC_PENDING，请修复待办后重试");
            return;
        }
        Pending pending=rows.getFirst();
        if("DONE".equals(pending.status())) return;
        if(!"PENDING".equals(pending.status()) || !"SYNC_PENDING".equals(current.getFenceStatus())) throw new ServiceException("规则清理状态不一致");
        OwnershipAlarmScopeCleanup helper=helpers.getIfAvailable();
        if(helper==null) throw new ServiceException("规则清理服务不可用，不能解冻");
        helper.removeDevice(current.getDeviceId(),pending.tenant(),pending.version(),current.getAssignmentVersion(),pending.requestId(),("DBO".equals(pending.operatorSource())?"dbo:":"business:")+pending.operator());
        if(jdbc.update("UPDATE iot_device_ownership_rule_cleanup SET status='DONE',attempts=attempts+1,last_error=NULL,completed_at=CURRENT_TIMESTAMP(3) WHERE device_id=? AND assignment_version=? AND status='PENDING'",current.getDeviceId(),current.getAssignmentVersion())!=1) throw new ServiceException("规则清理待办已变化");
    }
    public void failed(Long deviceId, Long version, RuntimeException error) {
        String message=error.getMessage()==null?error.getClass().getSimpleName():error.getMessage();
        jdbc.update("UPDATE iot_device_ownership_rule_cleanup SET attempts=attempts+1,last_error=? WHERE device_id=? AND assignment_version=? AND status='PENDING'",
            message.substring(0,Math.min(1000,message.length())),deviceId,version);
    }
}
