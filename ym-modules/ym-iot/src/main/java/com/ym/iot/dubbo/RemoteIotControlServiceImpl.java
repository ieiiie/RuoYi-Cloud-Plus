package com.ym.iot.dubbo;

import cn.hutool.core.bean.BeanUtil;

import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.iot.api.RemoteIotControlService;
import com.ym.iot.api.domain.bo.RemoteFertilizerRecordQueryBo;
import com.ym.iot.api.domain.bo.RemoteValveControlLogQueryBo;
import com.ym.iot.api.domain.vo.RemoteDeviceSummaryVo;
import com.ym.iot.api.domain.vo.RemoteFertilizerRecordVo;
import com.ym.iot.api.domain.vo.RemoteFertilizerStateVo;
import com.ym.iot.api.domain.vo.RemoteValveControlLogVo;
import com.ym.iot.api.domain.vo.RemoteValveSessionVo;
import com.ym.iot.fertilizer.domain.bo.IotFertilizerRecordBo;
import com.ym.iot.fertilizer.domain.vo.FertilizerStateSnapshot;
import com.ym.iot.motorvalve.domain.bo.ValveControlLogBo;
import com.ym.iot.motorvalve.service.IValveCommandService;
import com.ym.iot.motorvalve.service.IValveSessionService;
import com.ym.iot.motorvalve.support.MotorvalveValveTypeResolver;

import lombok.RequiredArgsConstructor;

import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** 施肥机与电动阀跨服务实现。 */
@Service
@DubboService
@RequiredArgsConstructor
public class RemoteIotControlServiceImpl implements RemoteIotControlService {

    private final com.ym.iot.jetlinks.service.IJetLinksFertilizerService jetLinksFertilizer;
    private final com.ym.iot.jetlinks.service.IJetLinksTagService jetLinksTags;
    private final com.ym.iot.jetlinks.support.JetLinksAccess jetLinksAccess;
    private final com.ym.iot.jetlinks.service.IJetLinksHistoryService jetLinksHistory;
    private final IValveCommandService valveCommandService;
    private final IValveSessionService valveSessionService;

    @Override
    public List<RemoteFertilizerRecordVo> listFertilizerRecords(
            RemoteFertilizerRecordQueryBo query) {
        RemoteFertilizerRecordQueryBo safe =
                query == null ? new RemoteFertilizerRecordQueryBo() : query;
        IotFertilizerRecordBo bo = BeanUtil.toBean(safe, IotFertilizerRecordBo.class);
        int limit = normalizeLimit(safe.getLimit());
        return jetLinksHistory.fertilizer(bo, new PageQuery(limit, 1)).getRows().stream()
                .map(row -> BeanUtil.toBean(row, RemoteFertilizerRecordVo.class))
                .toList();
    }

    @Override
    public RemoteFertilizerStateVo getFertilizerState(Long deviceId) {
        if (deviceId == null) {
            return null;
        }
        FertilizerStateSnapshot snapshot = jetLinksFertilizer.getState(deviceId);
        RemoteFertilizerStateVo target = BeanUtil.toBean(snapshot, RemoteFertilizerStateVo.class);
        target.setState(snapshot.getState() == null ? null : snapshot.getState().name());
        target.setOnline(
                snapshot.getState()
                                != com.ym.iot.fertilizer.enums.FertilizerDeviceState.OFFLINE
                        && snapshot.getSnapshotTime() != null);
        return target;
    }

    @Override
    public List<RemoteValveControlLogVo> listValveControlLogs(RemoteValveControlLogQueryBo query) {
        RemoteValveControlLogQueryBo safe =
                query == null ? new RemoteValveControlLogQueryBo() : query;
        ValveControlLogBo bo = BeanUtil.toBean(safe, ValveControlLogBo.class);
        return valveCommandService
                .queryControlLogPage(bo, new PageQuery(normalizeLimit(safe.getLimit()), 1))
                .getRows()
                .stream()
                .map(row -> BeanUtil.toBean(row, RemoteValveControlLogVo.class))
                .toList();
    }

    @Override
    public List<RemoteValveSessionVo> listOpenValveSessions() {
        return valveSessionService.listOpenSessions(null).stream()
                .map(row -> BeanUtil.toBean(row, RemoteValveSessionVo.class))
                .toList();
    }

    @Override
    public List<RemoteValveSessionVo> listRecentEndedValveSessions(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return jetLinksHistory.recentSessions(normalizeLimit(limit)).stream()
                .map(row -> BeanUtil.toBean(row, RemoteValveSessionVo.class))
                .toList();
    }

    @Override
    public List<RemoteDeviceSummaryVo> listValveDevices(String onlineStatus) {
        return valveCommandService.listValveDevices(onlineStatus).stream()
                .map(row -> BeanUtil.toBean(row, RemoteDeviceSummaryVo.class))
                .toList();
    }

    @Override
    public Map<Long, String> getChannelTagMap(Collection<Long> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return Map.of();
        }
        jetLinksAccess.require(deviceIds);
        Map<Long, String> result = new java.util.LinkedHashMap<>();
        for (Long id : deviceIds) {
            for (var tag : jetLinksTags.queryByDeviceId(id)) {
                if (MotorvalveValveTypeResolver.TAG_KEY_CHANNEL.equals(tag.getTagKey())) {
                    result.put(id, tag.getTagValue());
                }
            }
        }
        return result;
    }

    private static int normalizeLimit(Integer limit) {
        return limit == null || limit <= 0 ? 20 : Math.min(limit, 500);
    }
}
