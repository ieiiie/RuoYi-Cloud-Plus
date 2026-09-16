package com.ym.iot.motorvalve.service.impl;

import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.iot.jetlinks.service.IJetLinksHistoryService;
import com.ym.iot.jetlinks.support.JetLinksMapping;
import com.ym.iot.motorvalve.domain.bo.ValveSessionBo;
import com.ym.iot.motorvalve.domain.vo.ValveSessionStatsVo;
import com.ym.iot.motorvalve.domain.vo.ValveSessionVo;
import com.ym.iot.motorvalve.service.IValveSessionService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

/** 开阀会话只读查询。写入统一由 JetLinks 事件投影负责，不能再根据“发送成功”创建会话。 设备转移后仍按历史记录的租户归属查询，避免新租户读取原租户的作业记录。 */
@Service
@RequiredArgsConstructor
public class ValveSessionServiceImpl implements IValveSessionService {

    private final IJetLinksHistoryService jetLinksHistory;

    @Override
    public PageResult<ValveSessionVo> querySessionPage(ValveSessionBo bo, PageQuery pageQuery) {
        return JetLinksMapping.slice(jetLinksHistory.sessions(bo), pageQuery);
    }

    @Override
    public ValveSessionStatsVo queryStats(ValveSessionBo bo) {
        return jetLinksHistory.stats(bo);
    }

    @Override
    public List<ValveSessionVo> listOpenSessions(Long deviceId) {
        ValveSessionBo filter = new ValveSessionBo();
        filter.setDeviceId(deviceId);
        filter.setStatus("OPEN");
        return jetLinksHistory.sessions(filter);
    }
}
