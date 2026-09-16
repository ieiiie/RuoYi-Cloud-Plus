package com.ym.iot.api;

import com.ym.common.core.domain.PageResult;
import com.ym.iot.api.domain.bo.RemoteAlertQueryBo;
import com.ym.iot.api.domain.vo.RemoteAlertRecordVo;

/** IoT 告警跨服务契约。 */
public interface RemoteIotAlertService {

    PageResult<RemoteAlertRecordVo> pageRecords(RemoteAlertQueryBo query);

    long countAlarming();
}
