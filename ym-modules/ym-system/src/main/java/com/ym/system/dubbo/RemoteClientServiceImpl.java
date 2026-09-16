package com.ym.system.dubbo;

import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.system.api.RemoteClientService;
import com.ym.system.api.domain.vo.RemoteClientVo;
import com.ym.system.domain.vo.SysClientVo;
import com.ym.system.service.ISysClientService;
import org.springframework.stereotype.Service;

/**
 * 客户端服务
 *
 * @author Michelle.Chung
 */
@RequiredArgsConstructor
@Service
@DubboService
public class RemoteClientServiceImpl implements RemoteClientService {

    private final ISysClientService sysClientService;

    /**
     * 根据客户端id获取客户端详情
     *
     * @see com.ym.system.domain.convert.SysClientVoConvert
     */
    @Override
    public RemoteClientVo queryByClientId(String clientId) {
        SysClientVo vo = sysClientService.queryByClientId(clientId);
        return MapstructUtils.convert(vo, RemoteClientVo.class);
    }

}
