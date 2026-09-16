package com.ym.iot.ownership.service;

import com.ym.iot.ownership.domain.vo.OwnershipBlockerVo;

import java.util.List;

/** 设备转移的持久化业务占用检查。 */
public interface IOwnershipBlockerService {
    List<OwnershipBlockerVo> blockers(Long id);
}
