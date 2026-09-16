package com.ym.iot.api;

import com.ym.iot.api.domain.bo.RemoteFertilizerRecordQueryBo;
import com.ym.iot.api.domain.bo.RemoteValveControlLogQueryBo;
import com.ym.iot.api.domain.vo.RemoteDeviceSummaryVo;
import com.ym.iot.api.domain.vo.RemoteFertilizerRecordVo;
import com.ym.iot.api.domain.vo.RemoteFertilizerStateVo;
import com.ym.iot.api.domain.vo.RemoteValveControlLogVo;
import com.ym.iot.api.domain.vo.RemoteValveSessionVo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** 施肥机和电动阀查询及控制结果契约。 */
public interface RemoteIotControlService {

    List<RemoteFertilizerRecordVo> listFertilizerRecords(RemoteFertilizerRecordQueryBo query);

    RemoteFertilizerStateVo getFertilizerState(Long deviceId);

    List<RemoteValveControlLogVo> listValveControlLogs(RemoteValveControlLogQueryBo query);

    List<RemoteValveSessionVo> listOpenValveSessions();

    List<RemoteValveSessionVo> listRecentEndedValveSessions(int limit);

    List<RemoteDeviceSummaryVo> listValveDevices(String onlineStatus);

    Map<Long, String> getChannelTagMap(Collection<Long> deviceIds);
}
