package com.ym.system.ownership.service;

import com.ym.common.core.exception.ServiceException;
import com.ym.jetlinks.rpc.IotBusinessGuardRpcService;
import com.ym.system.ownership.model.OwnershipBlockerVo;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Read-only reverse RPC includes local queued/running fertilizer tasks inside ym-iot. */
@Service
public class OwnershipBlockerService {
    @DubboReference(group="jetlinks-iot-business",version="2.0.0",check=false,retries=0,timeout=5000)
    private IotBusinessGuardRpcService guard;
    public List<OwnershipBlockerVo> blockers(Long deviceId) {
        try {
            var result = guard.checkOwnershipChange(List.of(deviceId.toString())).get(6,TimeUnit.SECONDS);
            if (result == null || !result.containsKey(deviceId.toString()) || result.get(deviceId.toString()) == null)
                throw new IllegalStateException("Incomplete business guard response");
            return result.get(deviceId.toString()).stream().map(reason -> {
                if (reason == null || reason.isBlank()) throw new IllegalStateException("Invalid business guard reason");
                return new OwnershipBlockerVo("BUSINESS_OCCUPIED",1,reason);
            }).toList();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("业务占用检查中断，拒绝变更归属");
        } catch (Exception e) {
            throw new ServiceException("业务占用检查不可用，拒绝变更归属");
        }
    }
}
