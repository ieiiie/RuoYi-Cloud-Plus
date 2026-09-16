package com.ym.system.ownership.service;

/** The remote guard only knows device/version/frozen; business tenant checks stay in this service. */
public interface OwnershipOperationFence {
    void set(Long deviceId, long version, boolean frozen, Long operatorId);
}
