package com.ym.system.ownership.service;

import com.ym.common.core.exception.ServiceException;
import com.ym.jetlinks.rpc.IotAlarmRpcService;
import com.ym.jetlinks.rpc.RequestContext;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
public class JetLinksOwnershipAlarmScopeCleanup implements OwnershipAlarmScopeCleanup {
    @DubboReference(group="jetlinks-iot",version="2.0.0",check=false,retries=0,timeout=10000)
    private IotAlarmRpcService alarms;
    @Override
    public void removeDevice(Long deviceId, String previousTenantId, long previousVersion,
                             long publishedVersion, String requestId, String operatorRef) {
        try {
            var context = new RequestContext(requestId,"ym-dbo",operatorRef,publishedVersion);
            if (!Boolean.TRUE.equals(alarms.removeDeviceScope(context,deviceId.toString(),previousVersion).get(12,TimeUnit.SECONDS)))
                throw new IllegalStateException("Native alarm scope cleanup refused");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("原生告警范围清理中断，设备保持冻结");
        } catch (Exception e) {
            throw new ServiceException("原生告警范围清理失败，设备保持冻结");
        }
    }
}
