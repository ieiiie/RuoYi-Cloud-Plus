package com.ym.system.ownership.service;

import com.ym.common.core.exception.ServiceException;
import com.ym.jetlinks.rpc.IotCommandRpcService;
import com.ym.jetlinks.rpc.RequestContext;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
public class JetLinksOwnershipOperationFence implements OwnershipOperationFence {
    @DubboReference(group="jetlinks-iot",version="1.0.0",check=false,retries=0,timeout=10000)
    private IotCommandRpcService commands;
    public void set(Long id,long version,boolean frozen,Long operatorId) {
        try {
            var service=commands;
            if(service==null) throw new IllegalStateException("IotCommandRpcService unavailable");
            var context=new RequestContext("dbo-fence-"+id+"-"+version+"-"+frozen,"ym-dbo","dbo:"+operatorId,version);
            if(!Boolean.TRUE.equals(service.setOperationFence(context,id.toString(),version,frozen).get(12,TimeUnit.SECONDS)))
                throw new IllegalStateException("Operation fence was refused");
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt(); throw new ServiceException("归属操作栅栏同步中断，设备保持冻结，请管理员重试栅栏同步");
        } catch(Exception e) {
            throw new ServiceException("归属操作栅栏同步失败，设备保持冻结，请管理员重试栅栏同步");
        }
    }
}
