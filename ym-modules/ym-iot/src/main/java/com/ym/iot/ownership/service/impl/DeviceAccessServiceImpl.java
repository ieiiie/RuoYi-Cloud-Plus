package com.ym.iot.ownership.service.impl;

import com.ym.common.core.exception.ServiceException;
import com.ym.iot.ownership.domain.dto.DeviceOwnershipSnapshot;
import com.ym.iot.ownership.service.IDeviceAccessService;
import com.ym.iot.ownership.service.support.DeviceOwnershipRepository;
import com.ym.iot.ownership.support.OwnershipActor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Supplier;

@Service
public class DeviceAccessServiceImpl implements IDeviceAccessService {
    private final DeviceOwnershipRepository repository;
    private final OwnershipActor actor;

    public DeviceAccessServiceImpl(DeviceOwnershipRepository repository, OwnershipActor actor) {
        this.repository = repository;
        this.actor = actor;
    }

    public void requireAccess(Long id) {
        requireAccess(Collections.singletonList(id));
    }

    public void requireAccess(Collection<Long> ids) {
        List<Long> requested = normalize(ids);
        List<Long> accessible = filterAccessible(requested);
        if (accessible.size() != requested.size()) throw denied();
    }

    public List<Long> authorizedDeviceIds() {
        return repository.accessible(actor.tenantId(), null);
    }

    public String getTenantId(Long id) {
        var row = currentOwnership(id);
        return row == null ? null : row.getTenantId();
    }

    public DeviceOwnershipSnapshot currentOwnership(Long id) {
        validateId(id);
        return repository.find(id);
    }

    public List<Long> filterAccessible(Collection<Long> ids) {
        String tenant = actor.tenantId();
        List<Long> requested = normalize(ids);
        Set<Long> allowed = new HashSet<>();
        for (int i = 0; i < requested.size(); i += 500)
            allowed.addAll(
                    repository.accessible(
                            tenant, requested.subList(i, Math.min(i + 500, requested.size()))));
        return requested.stream().filter(allowed::contains).toList();
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED,
            timeout = 30,
            rollbackFor = Exception.class)
    public void requireAccess(Long id, Long version) {
        withAccess(id, version, () -> null);
    }

    public void requireCurrentVersion(Long id, Long version) {
        validateId(id);
        checkVersion(repository.find(id), version);
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED,
            timeout = 30,
            rollbackFor = Exception.class)
    public <T> T withAccess(Long id, Long version, Supplier<T> action) {
        validateId(id);
        String tenant = actor.tenantId();
        Objects.requireNonNull(action, "action");
        var row = repository.lock(id);
        checkVersion(row, version);
        if (!tenant.equals(row.getTenantId())) throw denied();
        return action.get();
    }

    public static void checkVersion(DeviceOwnershipSnapshot row, Long version) {
        if (version == null
                || version < 0
                || row == null
                || !version.equals(row.getAssignmentVersion()))
            throw new ServiceException("设备归属版本已变化或缺失，请刷新后重新发起请求");
        if (row.getTenantId() == null || !"ACTIVE".equals(row.getFenceStatus()))
            throw new ServiceException("设备未分配或归属变更中，暂不可操作");
    }

    public static void validateId(Long id) {
        if (id == null || id <= 0) throw new ServiceException("设备ID必须为正整数");
    }

    private static List<Long> normalize(Collection<Long> ids) {
        if (ids == null) throw new ServiceException("设备ID集合不能为空");
        ids.forEach(DeviceAccessServiceImpl::validateId);
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }

    private static ServiceException denied() {
        return new ServiceException("设备未分配、归属变更中或不属于当前租户");
    }
}
