package org.dromara.system.dubbo;

import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.system.api.RemoteLogService;
import org.dromara.system.api.domain.bo.RemoteLoginInfoBo;
import org.dromara.system.api.domain.bo.RemoteOperLogBo;
import org.dromara.system.domain.bo.SysLoginInfoBo;
import org.dromara.system.domain.bo.SysOperLogBo;
import org.dromara.system.service.ISysLoginInfoService;
import org.dromara.system.service.ISysOperLogService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 操作日志记录
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
@DubboService
public class RemoteLogServiceImpl implements RemoteLogService {

    private final ISysOperLogService operLogService;
    private final ISysLoginInfoService loginInfoService;

    /**
     * 保存系统日志
     *
     * @param remoteOperLogBo 日志实体
     */
    @Async
    @Override
    public void saveLog(RemoteOperLogBo remoteOperLogBo) {
        TenantHelper.dynamic(remoteOperLogBo.getTenantId(), () -> {
            SysOperLogBo sysOperLogBo = MapstructUtils.convert(remoteOperLogBo, SysOperLogBo.class);
            operLogService.insertOperlog(sysOperLogBo);
        });
    }

    /**
     * 保存访问记录
     *
     * @param remoteLoginInfoBo 访问实体
     */
    @Async
    @Override
    public void saveLoginInfo(RemoteLoginInfoBo remoteLoginInfoBo) {
        TenantHelper.dynamic(remoteLoginInfoBo.getTenantId(), () -> {
            SysLoginInfoBo sysLoginInfoBo = MapstructUtils.convert(remoteLoginInfoBo, SysLoginInfoBo.class);
            loginInfoService.insertLoginInfo(sysLoginInfoBo);
        });
    }
}
