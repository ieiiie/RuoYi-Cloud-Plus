package com.ym.common.log.event;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import com.ym.common.core.constant.Constants;
import com.ym.common.core.utils.ServletUtils;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.core.utils.ip.AddressUtils;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.system.api.RemoteClientService;
import com.ym.system.api.RemoteLogService;
import com.ym.system.api.domain.bo.RemoteLoginInfoBo;
import com.ym.system.api.domain.bo.RemoteOperLogBo;
import com.ym.system.api.domain.vo.RemoteClientVo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 异步调用日志服务
 *
 * @author ruoyi
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "ym.common.log", name = "remote-enabled", havingValue = "true", matchIfMissing = true)
public class LogEventListener {

    @DubboReference
    private RemoteLogService remoteLogService;
    @DubboReference
    private RemoteClientService remoteClientService;

    /**
     * 保存系统日志记录
     */
    @EventListener
    public void saveLog(OperLogEvent operLogEvent) {
        RemoteOperLogBo sysOperLog = BeanUtil.toBean(operLogEvent, RemoteOperLogBo.class);
        remoteLogService.saveLog(sysOperLog);
    }

    /**
     * 保存系统访问记录
     */
    @EventListener
    public void saveLoginInfo(LoginInfoEvent loginInfoEvent) {
        HttpServletRequest request = ServletUtils.getRequest();
        final UserAgent userAgent = UserAgentUtil.parse(request.getHeader("User-Agent"));
        final String ip = ServletUtils.getClientIP(request);
        // 客户端信息
        String clientId = request.getHeader(LoginHelper.CLIENT_KEY);
        RemoteClientVo clientVo = null;
        if (StringUtils.isNotBlank(clientId)) {
            clientVo = remoteClientService.queryByClientId(clientId);
        }

        String address = AddressUtils.getRealAddressByIP(ip);
        String s = getBlock(ip) +
            address +
            getBlock(loginInfoEvent.getUsername()) +
            getBlock(loginInfoEvent.getStatus()) +
            getBlock(loginInfoEvent.getMessage());
        // 打印信息到日志
        log.info(s, loginInfoEvent.getArgs());
        // 获取客户端操作系统
        String os = userAgent.getOs().getName();
        // 获取客户端浏览器
        String browser = userAgent.getBrowser().getName();
        // 封装对象
        RemoteLoginInfoBo loginInfo = new RemoteLoginInfoBo();
        loginInfo.setUserName(loginInfoEvent.getUsername());
        loginInfo.setTenantId(loginInfoEvent.getTenantId());
        if (ObjectUtil.isNotNull(clientVo)) {
            loginInfo.setClientKey(clientVo.getClientKey());
            loginInfo.setDeviceType(clientVo.getDeviceType());
        }
        loginInfo.setIpaddr(ip);
        loginInfo.setLoginLocation(address);
        loginInfo.setBrowser(browser);
        loginInfo.setOs(os);
        loginInfo.setMsg(loginInfoEvent.getMessage());
        // 日志状态
        if (StringUtils.equalsAny(loginInfoEvent.getStatus(), Constants.LOGIN_SUCCESS, Constants.LOGOUT, Constants.REGISTER)) {
            loginInfo.setStatus(Constants.SUCCESS);
        } else if (Constants.LOGIN_FAIL.equals(loginInfoEvent.getStatus())) {
            loginInfo.setStatus(Constants.FAIL);
        }
        remoteLogService.saveLoginInfo(loginInfo);
    }

    private String getBlock(Object msg) {
        if (msg == null) {
            msg = "";
        }
        return "[" + msg + "]";
    }

}
