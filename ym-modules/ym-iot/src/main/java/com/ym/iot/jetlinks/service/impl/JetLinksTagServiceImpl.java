package com.ym.iot.jetlinks.service.impl;

import static com.ym.iot.jetlinks.client.JetLinksRpcClient.await;
import static com.ym.iot.jetlinks.support.JetLinksMapping.beans;

import com.ym.iot.device.domain.vo.IotDeviceTagVo;
import com.ym.iot.jetlinks.client.JetLinksRpcClient;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.iot.jetlinks.service.IJetLinksTagService;
import com.ym.iot.jetlinks.support.JetLinksAccess;
import com.ym.jetlinks.rpc.RecordDto;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Primary
@ConditionalOnJetLinks
@RequiredArgsConstructor
public class JetLinksTagServiceImpl implements IJetLinksTagService {
    private final JetLinksRpcClient rpc;
    private final JetLinksAccess access;

    public List<RecordDto> records(Long id) {
        access.require(id);
        return await(rpc.getDevice().tags(id.toString()));
    }

    @Override
    public List<IotDeviceTagVo> queryByDeviceId(Long id) {
        return beans(records(id), "tagId", IotDeviceTagVo.class);
    }
}
