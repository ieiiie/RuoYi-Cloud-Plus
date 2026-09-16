package com.ym.system.dubbo;

import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.system.api.RemoteLogService;
import com.ym.system.api.domain.bo.RemoteLoginInfoBo;
import com.ym.system.api.domain.bo.RemoteOperLogBo;
import com.ym.system.domain.bo.SysLoginInfoBo;
import com.ym.system.domain.bo.SysOperLogBo;
import com.ym.system.service.ISysLoginInfoService;
import com.ym.system.service.ISysOperLogService;
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
