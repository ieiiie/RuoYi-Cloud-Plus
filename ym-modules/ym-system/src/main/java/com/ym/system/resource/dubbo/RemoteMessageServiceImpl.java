package com.ym.system.resource.dubbo;

import cn.hutool.core.bean.BeanUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import com.ym.common.core.enums.PushSourceEnum;
import com.ym.common.core.enums.PushTypeEnum;
import com.ym.common.push.dto.PushPayloadDTO;
import com.ym.common.push.helper.PushHelper;
import com.ym.resource.api.RemoteMessageService;
import com.ym.resource.api.domain.RemotePushPayLoad;
import com.ym.system.resource.service.ISysMessageService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消息服务
 *
 * @author Lion Li
 */
@Slf4j
@RequiredArgsConstructor
@Service
@DubboService
public class RemoteMessageServiceImpl implements RemoteMessageService {

    private final ISysMessageService sysMessageService;

    /**
     * 发送消息
     *
     * @param userIds 用户ID列表
     * @param message 消息文本
     */
    @Override
    public void publishMessage(List<Long> userIds, String message) {
        publishMessagePayload(userIds, RemotePushPayLoad.of(PushTypeEnum.MESSAGE, PushSourceEnum.BACKEND, message, null));
    }

    @Override
    public void publishMessagePayload(List<Long> userIds, RemotePushPayLoad payload) {
        PushPayloadDTO pushPayload = BeanUtil.copyProperties(payload, PushPayloadDTO.class);
        PushHelper.publishMessage(userIds, sysMessageService.storeUsers(userIds, pushPayload));
    }

    /**
     * 发布订阅的消息(群发)
     *
     * @param message 消息内容
     */
    @Override
    public void publishAll(String message) {
        publishAllPayload(RemotePushPayLoad.of(PushTypeEnum.MESSAGE, PushSourceEnum.BACKEND, message, null));
    }

    @Override
    public void publishAllPayload(RemotePushPayLoad payload) {
        PushPayloadDTO pushPayload = BeanUtil.copyProperties(payload, PushPayloadDTO.class);
        PushHelper.publishAll(sysMessageService.storeAll(pushPayload));
    }

}
