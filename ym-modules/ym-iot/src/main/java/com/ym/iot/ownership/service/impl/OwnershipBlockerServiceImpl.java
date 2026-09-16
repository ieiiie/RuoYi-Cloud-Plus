package com.ym.iot.ownership.service.impl;

import com.ym.iot.ownership.domain.vo.OwnershipBlockerVo;
import com.ym.iot.ownership.service.IOwnershipBlockerService;
import com.ym.iot.ownership.service.support.DeviceOwnershipRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

/** 以持久化业务事实检查设备转移，进程重启不会丢失未完成任务的阻断条件。 */
@Service
@RequiredArgsConstructor
public class OwnershipBlockerServiceImpl implements IOwnershipBlockerService {
    private final DeviceOwnershipRepository repository;

    @Override
    public List<OwnershipBlockerVo> blockers(Long id) {
        return List.copyOf(repository.blockers(id));
    }
}
