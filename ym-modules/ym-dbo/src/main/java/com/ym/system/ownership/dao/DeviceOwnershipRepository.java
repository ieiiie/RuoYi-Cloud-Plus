package com.ym.system.ownership.dao;

import com.ym.common.core.exception.ServiceException;
import com.ym.system.ownership.model.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

/** Deliberately explicit SQL: global ownership and blocker checks must not inherit tenant exclusions/ignores. */
@Repository
public class DeviceOwnershipRepository {
    private final JdbcTemplate jdbc;
    private static final RowMapper<DeviceOwnershipSnapshot> SNAPSHOT = (rs,n) -> new DeviceOwnershipSnapshot(
        rs.getLong("device_id"), rs.getString("tenant_id"), rs.getLong("assignment_version"),
        rs.getTimestamp("effective_from").toInstant(), rs.getString("fence_status"));

    public DeviceOwnershipRepository(@Qualifier("deviceOwnershipJdbc") JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
    public boolean deviceExists(Long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM iot_device WHERE device_id=? AND del_flag='0'",Long.class,id) == 1;
    }
    public DeviceOwnershipSnapshot find(Long id) {
        var rows = jdbc.query("SELECT * FROM iot_device_ownership WHERE device_id=?", SNAPSHOT, id);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    public DeviceOwnershipSnapshot lock(Long id) {
        lockProfile(id);
        return lockOwnership(id);
    }
    /** Only a management assignment may materialize the unassigned row, in its freeze transaction. */
    public DeviceOwnershipSnapshot lockForAssignment(Long id, Long actorId, String actorTenant, String reason) {
        lockProfile(id);
        // Avoid duplicate-insert shared locks on already registered rows. Concurrent first inserts
        // are resolved by the ownership PK; lockOwnership then reads the committed winner.
        if (find(id) == null) registerIfAbsent(id, actorId, actorTenant, reason);
        return lockOwnership(id);
    }
    private void lockProfile(Long id) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
            || TransactionSynchronizationManager.isCurrentTransactionReadOnly()
            || !TransactionSynchronizationManager.hasResource(Objects.requireNonNull(jdbc.getDataSource())))
            throw new IllegalStateException("设备归属锁必须在IoT写事务中获取");
        // DBO only has SELECT on technical profiles. SHARE prevents deletion while ownership changes.
        var devices = jdbc.queryForList("SELECT device_id FROM iot_device WHERE device_id=? AND del_flag='0' FOR SHARE", Long.class, id);
        if (devices.isEmpty()) throw new ServiceException("设备不存在或已删除");
    }
    private DeviceOwnershipSnapshot lockOwnership(Long id) {
        var rows = jdbc.query("SELECT * FROM iot_device_ownership WHERE device_id=? FOR UPDATE", SNAPSHOT, id);
        return rows.isEmpty() ? null : rows.getFirst();
    }
    public List<Long> accessible(String tenantId, Collection<Long> ids) {
        String sql = "SELECT o.device_id FROM iot_device_ownership o JOIN iot_device d ON d.device_id=o.device_id "
            + "WHERE o.tenant_id=? AND o.fence_status='ACTIVE' AND d.del_flag='0'";
        List<Object> args = new ArrayList<>(); args.add(tenantId);
        if (ids != null) {
            if (ids.isEmpty()) return List.of();
            sql += " AND o.device_id IN (" + String.join(",", Collections.nCopies(ids.size(), "?")) + ")";
            args.addAll(ids);
        }
        return jdbc.queryForList(sql + " ORDER BY o.device_id", Long.class, args.toArray());
    }
    public List<AssignedDeviceVo> assigned(long afterId,int limit,String tenantId) {
        String sql="SELECT d.device_id,d.device_code,d.device_name,o.tenant_id,o.assignment_version,o.effective_from,o.fence_status FROM iot_device d JOIN iot_device_ownership o ON o.device_id=d.device_id WHERE d.del_flag='0' AND o.tenant_id IS NOT NULL AND d.device_id>?";
        List<Object> args=new ArrayList<>(); args.add(afterId);
        if(tenantId!=null && !tenantId.isBlank()) { sql+=" AND o.tenant_id=?"; args.add(tenantId); }
        args.add(limit);
        return jdbc.query(sql+" ORDER BY d.device_id LIMIT ?",(rs,n) -> new AssignedDeviceVo(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getLong(5),rs.getTimestamp(6).toInstant(),rs.getString(7)),args.toArray());
    }
    public List<UnassignedDeviceVo> pool(long afterId, int limit) {
        return jdbc.query("SELECT d.device_id,d.device_code,d.device_name,COALESCE(o.assignment_version,0) assignment_version,"
            + "COALESCE(o.fence_status,'ACTIVE') fence_status FROM iot_device d LEFT JOIN iot_device_ownership o ON o.device_id=d.device_id "
            + "WHERE d.del_flag='0' AND o.tenant_id IS NULL AND d.device_id>? ORDER BY d.device_id LIMIT ?",
            (rs,n) -> new UnassignedDeviceVo(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getLong(4),rs.getString(5)), afterId,limit);
    }
    public List<OwnershipHistoryVo> history(Long id, long afterVersion, int limit) {
        return jdbc.query("SELECT * FROM iot_device_ownership_history WHERE device_id=? AND assignment_version>? ORDER BY assignment_version LIMIT ?",
            (rs,n) -> new OwnershipHistoryVo(rs.getLong("device_id"),rs.getString("tenant_id"),rs.getString("previous_tenant_id"),
                rs.getLong("assignment_version"),rs.getTimestamp("effective_from").toInstant(),
                rs.getTimestamp("effective_to") == null ? null : rs.getTimestamp("effective_to").toInstant(),
                rs.getString("action"),rs.getObject("operator_id",Long.class),rs.getString("operator_tenant_id"),rs.getString("reason"),rs.getString("operator_source")),id,afterVersion,limit);
    }
    public Instant now() { return jdbc.queryForObject("SELECT CURRENT_TIMESTAMP(3)",Timestamp.class).toInstant(); }
    private void registerIfAbsent(Long id, Long actorId, String actorTenant, String reason) {
        Instant now = now();
        int inserted = jdbc.update("INSERT IGNORE INTO iot_device_ownership(device_id,tenant_id,assignment_version,effective_from,fence_status) VALUES (?,NULL,0,?,'ACTIVE')",id,Timestamp.from(now));
        if (inserted == 1)
            append(new DeviceOwnershipSnapshot(id,null,0L,now,"ACTIVE"),null,"REGISTER",actorId,actorTenant,reason);
    }
    public void fence(Long id, String status) {
        if (jdbc.update("UPDATE iot_device_ownership SET fence_status=? WHERE device_id=?",status,id) != 1)
            throw new ServiceException("设备归属记录已变化");
    }
    public DeviceOwnershipSnapshot change(DeviceOwnershipSnapshot old, String tenant, String action, Long actorId, String actorTenant, String reason, String metadataSnapshot) {
        if (old.getTenantId() != null && (metadataSnapshot == null || metadataSnapshot.isBlank()))
            throw new ServiceException("租户归属历史缺少展示快照，拒绝关闭区间");
        Instant now = now();
        // Preserve strict interval ordering even if two changes land within the same DB millisecond.
        if (!now.isAfter(old.getEffectiveFrom())) now = old.getEffectiveFrom().plusMillis(1);
        if (jdbc.update("UPDATE iot_device_ownership_history SET effective_to=?,metadata_snapshot=? WHERE device_id=? AND assignment_version=? AND effective_to IS NULL",
            Timestamp.from(now),metadataSnapshot,old.getDeviceId(),old.getAssignmentVersion()) != 1) throw new ServiceException("归属历史不完整，拒绝覆盖");
        // V+1 is reserved for remote freeze; V+2 is the next published ownership/command version.
        long version = Math.addExact(old.getAssignmentVersion(),2L);
        if (jdbc.update("UPDATE iot_device_ownership SET tenant_id=?,assignment_version=?,effective_from=?,fence_status='SYNC_PENDING' WHERE device_id=? AND assignment_version=? AND fence_status='FROZEN'",
            tenant,version,Timestamp.from(now),old.getDeviceId(),old.getAssignmentVersion()) != 1) throw new ServiceException("设备归属版本已变化，请刷新后重试");
        var next = new DeviceOwnershipSnapshot(old.getDeviceId(),tenant,version,now,"SYNC_PENDING");
        append(next,old.getTenantId(),action,actorId,actorTenant,reason);
        return next;
    }
    private void append(DeviceOwnershipSnapshot row, String previous, String action, Long actorId, String actorTenant, String reason) {
        jdbc.update("INSERT INTO iot_device_ownership_history(device_id,assignment_version,tenant_id,previous_tenant_id,effective_from,action,operator_id,operator_tenant_id,reason,operator_source) VALUES (?,?,?,?,?,?,?,?,?,'DBO')",
            row.getDeviceId(),row.getAssignmentVersion(),row.getTenantId(),previous,Timestamp.from(row.getEffectiveFrom()),action,actorId,actorTenant,reason);
    }
}
