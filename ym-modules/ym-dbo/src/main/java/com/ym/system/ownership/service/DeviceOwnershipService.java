package com.ym.system.ownership.service;

import com.ym.common.core.exception.ServiceException;
import com.ym.system.ownership.dao.DeviceOwnershipRepository;
import com.ym.system.ownership.model.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DeviceOwnershipService {
    private final DeviceOwnershipRepository repository;
    private final OwnershipActor actor;
    private final OwnershipTargetTenantValidator tenants;
    private final OwnershipBlockerService blockers;
    private final OwnershipOperationFence fence;
    private final TransactionTemplate tx;
    private final OwnershipRuleCleanup ruleCleanup;
    private final JetLinksOwnershipMetadataSnapshot metadataSnapshots;
    public DeviceOwnershipService(DeviceOwnershipRepository repository,OwnershipActor actor,
        OwnershipTargetTenantValidator tenants,OwnershipBlockerService blockers,OwnershipOperationFence fence,
        @Qualifier("deviceOwnershipTransaction") TransactionTemplate tx, OwnershipRuleCleanup ruleCleanup,
        JetLinksOwnershipMetadataSnapshot metadataSnapshots) {
        this.repository=repository; this.actor=actor; this.tenants=tenants; this.blockers=blockers; this.fence=fence; this.tx=tx; this.ruleCleanup=ruleCleanup; this.metadataSnapshots=metadataSnapshots;
    }
    public List<UnassignedDeviceVo> pool(long afterId,int limit) { actor.requirePermission("query"); page(afterId,limit); return repository.pool(afterId,limit); }
    public List<AssignedDeviceVo> assigned(long afterId,int limit,String tenantId) { actor.requirePermission("query"); page(afterId,limit); return repository.assigned(afterId,limit,tenantId); }
    public List<OwnershipTenantVo> tenants() { actor.requirePermission("query"); return tenants.activeTenants(); }
    public DeviceOwnershipSnapshot current(Long id) {
        actor.requirePermission("query"); validateId(id); return repository.find(id);
    }
    public List<OwnershipHistoryVo> history(Long id,long afterVersion,int limit) {
        actor.requirePermission("query"); validateId(id); if(afterVersion < -1) throw new ServiceException("历史游标无效"); page(0,limit); return repository.history(id,afterVersion,limit);
    }
    public List<OwnershipBlockerVo> blockers(Long id) { actor.requirePermission("query"); validateId(id); return blockers.blockers(id); }
    public DeviceOwnershipSnapshot assign(Long id,OwnershipChangeBo bo) { return change(id,bo.expectedAssignmentVersion(),bo.targetTenantId(),"ASSIGN",bo.reason()); }
    public DeviceOwnershipSnapshot transfer(Long id,OwnershipChangeBo bo) { return change(id,bo.expectedAssignmentVersion(),bo.targetTenantId(),"TRANSFER",bo.reason()); }
    public DeviceOwnershipSnapshot release(Long id,OwnershipReleaseBo bo) { return change(id,bo.expectedAssignmentVersion(),null,"RELEASE",bo.reason()); }

    private DeviceOwnershipSnapshot change(Long id,Long version,String target,String action,String reason) {
        actor.requirePermission(action.toLowerCase(java.util.Locale.ROOT)); validateId(id); validate(version,reason);
        Long operator=actor.operatorId(); String operatorTenant=null;
        if(target!=null) tenants.requireActive(target);
        // Durable local freeze BEFORE any remote effect. All stages use the same row lock.
        freezeWithDeadlockRetry(() -> {
            var old="ASSIGN".equals(action)
                ? repository.lockForAssignment(id,operator,operatorTenant,reason) : repository.lock(id);
            version(old,version);
            if(old!=null && !"ACTIVE".equals(old.getFenceStatus())) throw new ServiceException("设备归属变更尚未结束，请先重试栅栏同步");
            String oldTenant=old==null ? null : old.getTenantId();
            if("ASSIGN".equals(action) && oldTenant!=null) throw new ServiceException("设备已有租户，请使用转移操作");
            if(!"ASSIGN".equals(action) && oldTenant==null) throw new ServiceException("设备尚未分配租户");
            if(Objects.equals(oldTenant,target)) throw new ServiceException("目标租户与当前归属相同");
            requireClear(id);
            repository.fence(id,"FROZEN");
        });
        try {
            tx.executeWithoutResult(status -> {
                var old=repository.lock(id); version(old,version);
                if(!"FROZEN".equals(old.getFenceStatus())) throw new ServiceException("冻结状态已变化，请刷新");
                fence.set(id,Math.addExact(version,1L),true,operator);
                requireClear(id); // after remote freeze; includes all tenants and queued local tasks
                // 在旧租户仍冻结、归属行仍锁定时读取核心目录；失败则保留已提交的冻结供重试。
                String metadataSnapshot=old.getTenantId()==null ? null : metadataSnapshots.capture(id);
                var published=repository.change(old,target,action,operator,operatorTenant,reason,metadataSnapshot);
                ruleCleanup.enqueue(old,published.getAssignmentVersion(),operator);
            });
        } catch(RuntimeException e) {
            throw new ServiceException("设备已冻结，归属未变更：" + e.getMessage() + "；处理后请先调用 retry-fence 恢复，再重试归属操作");
        }
        // Commit ownership+history BEFORE releasing the remote guard. Failures retain SYNC_PENDING.
        return synchronizeFence(id,Math.addExact(version,2L),operator);
    }
    private void freezeWithDeadlockRetry(Runnable freeze) {
        // Concurrent INSERT IGNORE losers can hold shared PK locks before their FOR UPDATE.
        // A deadlock rolls back this entire local transaction, including REGISTER. Retry only
        // this pre-remote stage; command fences and cleanup must never be replayed implicitly.
        for (int attempt=0; ; attempt++) {
            try {
                tx.executeWithoutResult(status -> freeze.run());
                return;
            } catch (RuntimeException error) {
                if (attempt >= 3 || !isDeadlock(error)) throw error;
            }
        }
    }
    private static boolean isDeadlock(Throwable error) {
        for (Throwable cause=error; cause!=null; cause=cause.getCause())
            if (cause instanceof SQLException sql && "40001".equals(sql.getSQLState())) return true;
        return false;
    }
    public DeviceOwnershipSnapshot retryFence(Long id,OwnershipReleaseBo bo) {
        actor.requirePermission("retry"); validateId(id); validate(bo.expectedAssignmentVersion(),bo.reason());
        Long operator = actor.operatorId();
        String operatorTenant = null;
        var recovered = tx.execute(status -> {
            var row = repository.lock(id);
            version(row,bo.expectedAssignmentVersion());
            if (row == null) throw new ServiceException("设备尚无归属记录");
            if ("FROZEN".equals(row.getFenceStatus())) {
                // A late freeze(V+1) may still arrive. Never unfreeze at V or reuse V+1.
                // Retain the tenant, but publish V+2 with an auditable RECOVER interval first.
                // RECOVER 也会关闭租户持有区间，不得绕过历史展示快照。
                String metadataSnapshot=row.getTenantId()==null ? null : metadataSnapshots.capture(id);
                return repository.change(row,row.getTenantId(),"RECOVER",operator,operatorTenant,bo.reason(),metadataSnapshot);
            }
            return row;
        });
        return synchronizeFence(id,recovered.getAssignmentVersion(),operator);
    }
    private DeviceOwnershipSnapshot synchronizeFence(Long id,Long version,Long operator) {
        try {
            tx.executeWithoutResult(status -> {
                var row=repository.lock(id); version(row,version);
                if(row==null) throw new ServiceException("设备尚无归属记录");
                if("ACTIVE".equals(row.getFenceStatus())) return;
                if(!"SYNC_PENDING".equals(row.getFenceStatus())) throw new ServiceException("归属状态已变化");
                ruleCleanup.complete(row);
            });
        } catch(RuntimeException error) {
            tx.executeWithoutResult(status -> ruleCleanup.failed(id,version,error));
            throw new ServiceException("旧租户规则范围清理未完成，设备保持SYNC_PENDING；请处理后调用retry-fence："+error.getMessage());
        }
        // DONE is committed before unfreeze; a failed unfreeze cannot lose cleanup progress.
        return tx.execute(status -> {
            var row=repository.lock(id); version(row,version);
            if(row==null) throw new ServiceException("设备尚无归属记录");
            if("ACTIVE".equals(row.getFenceStatus())) return row;
            if(!"SYNC_PENDING".equals(row.getFenceStatus()))
                throw new ServiceException("设备正在另一次冻结操作中，请刷新并使用 retry-fence 恢复");
            // Released/unassigned devices remain remotely frozen, even once synchronization is complete.
            fence.set(id,version,row.getTenantId()==null,operator);
            repository.fence(id,"ACTIVE");
            return repository.find(id);
        });
    }
    private void requireClear(Long id) {
        var reasons=blockers.blockers(id);
        if(!reasons.isEmpty()) throw new ServiceException("无法释放或转移："+reasons.stream().map(b->b.code()+"("+b.count()+"): "+b.message()).collect(Collectors.joining("；")));
    }
    private static void version(DeviceOwnershipSnapshot row,Long expected) {
        long actual=row==null ? 0L : row.getAssignmentVersion();
        if(expected==null || actual!=expected) throw new ServiceException("设备归属版本已变化，请刷新后重试");
    }
    private static void validate(Long version,String reason) {
        if(version==null || version<0 || reason==null || reason.isBlank() || reason.length()>500) throw new ServiceException("必须提供有效归属版本及500字以内的操作原因");
    }
    private static void validateId(Long id) {
        if (id == null || id <= 0) throw new ServiceException("设备ID必须为正整数");
    }
    private static void page(long after,int limit) { if(after<0 || limit<1 || limit>200) throw new ServiceException("游标不得为负，limit 必须为1至200"); }
}
