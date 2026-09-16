package com.ym.iot.jetlinks.service;

import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.iot.fertilizer.domain.bo.IotFertilizerRecordBo;
import com.ym.iot.fertilizer.domain.vo.IotFertilizerRecordVo;
import com.ym.iot.motorvalve.domain.bo.ValveControlLogBo;
import com.ym.iot.motorvalve.domain.bo.ValveSessionBo;
import com.ym.iot.motorvalve.domain.vo.ValveControlLogVo;
import com.ym.iot.motorvalve.domain.vo.ValveSessionStatsVo;
import com.ym.iot.motorvalve.domain.vo.ValveSessionVo;

import java.util.List;

/** JetLinks 业务接口；控制器和 Dubbo 适配只依赖业务契约。 */
public interface IJetLinksHistoryService {
    PageResult<IotFertilizerRecordVo> fertilizer(IotFertilizerRecordBo bo, PageQuery page);

    IotFertilizerRecordVo fertilizer(Long recordId);

    PageResult<ValveControlLogVo> valveLogs(ValveControlLogBo bo, PageQuery page);

    List<ValveSessionVo> sessions(ValveSessionBo bo);

    List<ValveSessionVo> recentSessions(int limit);

    ValveSessionStatsVo stats(ValveSessionBo bo);
}
