package com.ym.system.ownership.service;

/** Removes only the old business owner's native alarm scope; never global operations rules. */
public interface OwnershipAlarmScopeCleanup {
    void removeDevice(Long deviceId, String previousTenantId, long previousAssignmentVersion,
                      long publishedAssignmentVersion, String requestId, String operatorRef);
}
