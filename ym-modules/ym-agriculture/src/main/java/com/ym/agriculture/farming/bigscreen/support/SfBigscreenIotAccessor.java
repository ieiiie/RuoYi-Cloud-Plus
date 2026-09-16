package com.ym.agriculture.farming.bigscreen.support;

import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.iot.api.RemoteIotAlertService;
import com.ym.iot.api.RemoteIotControlService;
import com.ym.iot.api.RemoteIotDeviceService;
import com.ym.iot.api.RemoteIotTelemetryService;
import com.ym.iot.api.domain.bo.RemoteAlertQueryBo;
import com.ym.iot.api.domain.bo.RemoteDeviceQueryBo;
import com.ym.iot.api.domain.bo.RemoteFertilizerRecordQueryBo;
import com.ym.iot.api.domain.bo.RemoteValveControlLogQueryBo;
import com.ym.iot.api.domain.vo.RemoteAlertRecordVo;
import com.ym.iot.api.domain.vo.RemoteDeviceSummaryVo;
import com.ym.iot.api.domain.vo.RemoteFertilizerRecordVo;
import com.ym.iot.api.domain.vo.RemoteFertilizerStateVo;
import com.ym.iot.api.domain.vo.RemoteLatestTelemetryVo;
import com.ym.iot.api.domain.vo.RemotePestChartVo;
import com.ym.iot.api.domain.vo.RemoteProductVo;
import com.ym.iot.api.domain.vo.RemoteValveControlLogVo;
import com.ym.iot.api.domain.vo.RemoteValveSessionVo;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/** 大屏域访问 IoT 领域公开契约的桥接。 */
@Component
public class SfBigscreenIotAccessor {

    @DubboReference
    private RemoteIotDeviceService remoteIotDeviceService;
    @DubboReference
    private RemoteIotTelemetryService remoteIotTelemetryService;
    @DubboReference(version="2.0.0", retries=0)
    private RemoteIotAlertService remoteIotAlertService;
    @DubboReference
    private RemoteIotControlService remoteIotControlService;

    public List<RemoteDeviceSummaryVo> queryDevicesBySns(Collection<String> deviceSns) {
        if (deviceSns == null || deviceSns.isEmpty()) {
            return List.of();
        }
        RemoteDeviceQueryBo query = new RemoteDeviceQueryBo();
        query.setDeviceCodeList(deviceSns.stream().toList());
        return remoteIotDeviceService.listDevices(query);
    }

    public List<RemoteDeviceSummaryVo> queryTenantNormalDevices() {
        RemoteDeviceQueryBo query = new RemoteDeviceQueryBo();
        query.setStatus(SystemConstants.NORMAL);
        return remoteIotDeviceService.listDevices(query);
    }

    public Map<Long, RemoteLatestTelemetryVo> getLatestMap(Collection<Long> deviceIds) {
        return remoteIotTelemetryService.getLatestMap(deviceIds);
    }

    public RemotePestChartVo getPestChart(Long deviceId, Date from, Date to) {
        return remoteIotTelemetryService.getPestChart(deviceId, from, to);
    }

    public PageResult<RemoteAlertRecordVo> queryAlertPage(RemoteAlertQueryBo query, PageQuery pageQuery) {
        query.setPageNum(pageQuery.getPageNum());
        query.setPageSize(pageQuery.getPageSize());
        return remoteIotAlertService.pageRecords(query);
    }

    public long countAlarming() {
        return remoteIotAlertService.countAlarming();
    }

    public List<RemoteFertilizerRecordVo> queryFertilizerRecords(RemoteFertilizerRecordQueryBo query, int limit) {
        query.setLimit(limit);
        return remoteIotControlService.listFertilizerRecords(query);
    }

    public RemoteFertilizerStateVo getFertilizerState(Long deviceId) {
        return remoteIotControlService.getFertilizerState(deviceId);
    }

    public List<RemoteValveControlLogVo> queryValveControlLogs(RemoteValveControlLogQueryBo query, int limit) {
        query.setLimit(limit);
        return remoteIotControlService.listValveControlLogs(query);
    }

    public List<RemoteValveSessionVo> listOpenValveSessions() {
        return remoteIotControlService.listOpenValveSessions();
    }

    public List<RemoteValveSessionVo> queryRecentEndedSessions(int fetchSize) {
        return remoteIotControlService.listRecentEndedValveSessions(fetchSize);
    }

    public List<RemoteProductVo> queryProductsByIds(Collection<Long> productIds) {
        return remoteIotDeviceService.listProductsByIds(productIds);
    }

    public List<RemoteProductVo> queryProductsByProductKeys(Collection<String> productKeys) {
        return remoteIotDeviceService.listProductsByKeys(productKeys);
    }

    public Map<Long, String> getChannelTagMap(Collection<Long> deviceIds) {
        return remoteIotControlService.getChannelTagMap(deviceIds);
    }

    public List<RemoteDeviceSummaryVo> listValveDevices(String onlineStatus) {
        return remoteIotControlService.listValveDevices(onlineStatus);
    }
}
