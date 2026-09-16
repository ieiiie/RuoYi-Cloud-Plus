package com.ym.iot.ownership.service;

import com.ym.iot.ownership.domain.dto.DeviceOwnershipSnapshot;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/** Business-side exclusivity contract. No tenant enforcement is delegated to JetLinks. */
public interface IDeviceAccessService {
    void requireAccess(Long deviceId);
    void requireAccess(Collection<Long> deviceIds);
    List<Long> authorizedDeviceIds();
    /** Trusted internal lookup for ingestion. Null means absent/unassigned, never infer another tenant. */
    String getTenantId(Long deviceId);
    /** Trusted internal lookup, null when absent. Do not expose without caller authorization. */
    DeviceOwnershipSnapshot currentOwnership(Long deviceId);
    List<Long> filterAccessible(Collection<Long> deviceIds);
    /** Required for a request created against an earlier ownership view. Null versions are refused. */
    void requireAccess(Long deviceId, Long expectedAssignmentVersion);
    /** Revalidate a captured task version immediately before asynchronous dispatch; no request tenant fallback. */
    void requireCurrentVersion(Long deviceId, Long expectedAssignmentVersion);
    /** Serialize initiation with ownership changes. Pass the captured version to every subsequent RPC command. */
    <T> T withAccess(Long deviceId, Long expectedAssignmentVersion, Supplier<T> action);
}
