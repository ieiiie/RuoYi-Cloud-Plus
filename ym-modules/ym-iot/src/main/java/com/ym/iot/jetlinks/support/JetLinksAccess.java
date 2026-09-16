package com.ym.iot.jetlinks.support;

import com.ym.common.core.exception.ServiceException;
import com.ym.iot.jetlinks.client.JetLinksRpcClient;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.iot.ownership.domain.dto.DeviceOwnershipSnapshot;
import com.ym.iot.ownership.service.IDeviceAccessService;
import com.ym.jetlinks.rpc.RecordDto;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Supplier;

@Component
@ConditionalOnJetLinks
@RequiredArgsConstructor
public class JetLinksAccess {
    private final IDeviceAccessService access;

    public static long rpcVisibleFrom(java.time.Instant effectiveFrom) {
        return Math.max(0L, effectiveFrom.toEpochMilli());
    }

    public List<String> ids() {
        return JetLinksMapping.ids(access.authorizedDeviceIds());
    }

    public void require(Long id) {
        if (id == null) throw new ServiceException("设备ID不能为空");
        access.requireAccess(id);
    }

    public void require(Collection<Long> ids) {
        if (ids == null) throw new ServiceException("设备ID集合不能为空");
        access.requireAccess(ids);
    }

    public DeviceOwnershipSnapshot snapshot(Long id) {
        require(id);
        DeviceOwnershipSnapshot snapshot = access.currentOwnership(id);
        if (snapshot == null
                || snapshot.getTenantId() == null
                || snapshot.getAssignmentVersion() == null
                || snapshot.getEffectiveFrom() == null)
            throw new ServiceException("设备缺少有效租户归属，拒绝访问");
        return snapshot;
    }

    public Map<Long, DeviceOwnershipSnapshot> capture(Collection<Long> ids) {
        require(ids);
        Map<Long, DeviceOwnershipSnapshot> result = new LinkedHashMap<>();
        for (Long id : ids) result.put(id, snapshot(id));
        return result;
    }

    public void recheck(Map<Long, DeviceOwnershipSnapshot> snapshots) {
        snapshots.forEach((id, s) -> access.requireAccess(id, s.getAssignmentVersion()));
    }

    public com.ym.jetlinks.rpc.RequestContext context(JetLinksRpcClient rpc, Long id) {
        return rpc.deviceContext(UUID.randomUUID().toString(), snapshot(id).getAssignmentVersion());
    }

    public void requireVersion(Long id, Long version) {
        access.requireAccess(id, version);
    }

    public long visibleFrom(Long id) {
        return rpcVisibleFrom(snapshot(id).getEffectiveFrom());
    }

    public Map<String, Long> visibleFrom(Collection<Long> ids) {
        require(ids);
        Map<String, Long> result = new LinkedHashMap<>();
        for (Long id : ids) result.put(id.toString(), visibleFrom(id));
        return result;
    }

    public <T> T write(Long id, Supplier<T> work) {
        DeviceOwnershipSnapshot snapshot = snapshot(id);
        return access.withAccess(id, snapshot.getAssignmentVersion(), work);
    }

    public void requireRecord(RecordDto record) {
        if (record == null) throw new ServiceException("记录不存在");
        Object id = record.data().get("deviceId");
        if (id == null)
            throw JetLinksRpcClient.invalidProviderRecord("device-scoped record has no deviceId");
        require(JetLinksMapping.id(id.toString()));
    }
}
